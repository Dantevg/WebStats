import Connection from "./Connection"
import Data from "./Data"
import Display from "./Display"

declare global {
	interface Window { webstats: WebStats }
}

type TableConfig = {
	name?: string
	columns?: string[]
	sortBy?: string
	sortDescending?: boolean
}

export default class WebStats {
	displays: Display[]
	connection: Connection
	data: Data
	updateInterval: number
	interval: number

	constructor(config) {
		this.displays = []
		this.connection = config.connection ?? Connection.json(config.ip, config.port)
		this.updateInterval = config.updateInterval ?? 10000

		// Set online status update interval
		if (this.updateInterval > 0) this.startUpdateInterval(true)
		document.addEventListener("visibilitychange", () => document.hidden
			? this.stopUpdateInterval() : this.startUpdateInterval())

		// Get data and init
		const statsPromise = this.connection.getStats()
		const tableConfigsPromise = this.connection.getTables()
		Promise.all([statsPromise, tableConfigsPromise])
			.then(([stats, tableConfigs]) => this.init(stats, tableConfigs, config))

		// Get saved toggles from cookies
		const cookies = document.cookie.split("; ") ?? []
		cookies.filter(str => str.length > 0).forEach(cookie => {
			const [property, value] = cookie.match(/[^=]+/g)
			document.documentElement.classList.toggle(property, value == "true")
			const el = document.querySelector("input.webstats-option#" + property) as HTMLInputElement
			if (el) el.checked = (value == "true")
		})

		// On config option toggle, set the html element's class and store cookie
		document.querySelectorAll("input.webstats-option").forEach(el =>
			el.addEventListener("change", () => {
				document.documentElement.classList.toggle(el.id, (el as HTMLInputElement).checked)
				// Set a cookie which expires in 10 years
				document.cookie = `${el.id}=${(el as HTMLInputElement).checked}; max-age=${60 * 60 * 24 * 365 * 10}; SameSite=Lax`
			})
		)

		const optionHideOffline = document.querySelector("input.webstats-option#hide-offline") as HTMLInputElement
		if (optionHideOffline) {
			// Re-show if displayCount is set
			optionHideOffline.addEventListener("change", (e) => {
				this.displays.forEach(display => {
					if (display.displayCount > 0) display.changeHideOffline((e.target as HTMLInputElement).checked)
				})
			})
			this.displays.forEach(display => display.hideOffline = optionHideOffline.checked)
		}

		window.webstats = this
	}

	init(data: Data, tableConfigs: TableConfig[], config) {
		if (config.tables) {
			for (const tableName in config.tables) {
				const tableConfig = tableConfigs.find(config => config.name == tableName)
				if (tableConfig) {
					this.displays.push(new Display(
						{ ...config, table: config.tables[tableName] },
						tableConfig
					))
				}
			}
		} else {
			for (const tableConfig of tableConfigs) {
				const headerElem = (config.tableParent as HTMLElement)
					.appendChild(document.createElement("span"))
				headerElem.innerText = String(tableConfig.name)
				headerElem.classList.add("webstats-tablename")
				headerElem.setAttribute("webstats-table", tableConfig.name)
				
				const tableElem = (config.tableParent as HTMLElement)
					.appendChild(document.createElement("table"))
				tableElem.setAttribute("webstats-table", tableConfig.name)
				this.displays.push(new Display(
					{ ...config, table: tableElem },
					tableConfig
				))
			}
		}

		this.data = new Data(data)
		this.displays.forEach(display => display.init(this.data)) // Display data in table
	}

	update() {
		// When nobody is online, assume scoreboard does not change
		if (this.data.nOnline > 0) {
			this.connection.getStats().then(data => {
				this.data.setStats(data)
				this.displays.forEach(display => display.updateStats())
			})
		} else {
			this.connection.getOnline().then(data => {
				this.data.setOnlineStatus(data)
				this.displays.forEach(display => display.updateOnlineStatus())
			})
		}
	}

	startUpdateInterval(first?: boolean) {
		this.interval = setInterval(this.update.bind(this), this.updateInterval)
		if (!first) this.update()
	}

	stopUpdateInterval() {
		clearInterval(this.interval)
	}

}
