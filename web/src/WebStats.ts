import Connection from "./Connection"
import Data from "./Data"
import Display from "./Display"
import Pagination from "./Pagination"

declare global {
	interface Window { webstats: WebStats }
}

export type TableConfig = {
	name?: string
	columns?: string[]
	sortBy?: string
	sortDescending?: boolean
}

export default class WebStats {
	static CONNECTION_ERROR_MSG = "No connection to server. Either the server is offline, or the 'host' setting in index.html is incorrect."

	displays: Display[]
	connection: Connection
	data: Data
	updateInterval: number
	interval: number

	loadingElem?: HTMLElement
	errorElem?: HTMLElement

	constructor(config) {
		this.displays = []
		this.connection = config.connection ?? Connection.json(config.host)
		this.updateInterval = config.updateInterval ?? 10000

		// Status HTML elements
		const statusElem = document.querySelector(".webstats-status")
		this.loadingElem = statusElem?.querySelector(".webstats-loading-indicator")
		this.errorElem = statusElem?.querySelector(".webstats-error-message")
		this.setLoadingStatus(true)

		// Get data and init
		const statsPromise = this.connection.getStats()
		const tableConfigsPromise = this.connection.getTables()
		Promise.all([statsPromise, tableConfigsPromise])
			.then(([stats, tableConfigs]) => this.init(stats, tableConfigs, config))
			.catch(e => {
				console.error(e)
				console.warn(WebStats.CONNECTION_ERROR_MSG)
				this.setErrorMessage(WebStats.CONNECTION_ERROR_MSG, config)
				this.setLoadingStatus(false)
			})

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
				this.displays.forEach(display => display.changeHideOffline(optionHideOffline.checked))
			})
			this.displays.forEach(display => display.changeHideOffline(optionHideOffline.checked))
		}

		window.webstats = this
	}

	init(data, tableConfigs: TableConfig[] | undefined, config) {
		if (config.tables) {
			for (const tableName in config.tables) {
				const tableConfig = tableConfigs
					? tableConfigs.find(tc => (tc.name ?? "") == tableName)
					: { colums: data.scoreboard.columns as string[] } as TableConfig
				if (tableConfig) this.addTableManual(config, tableConfig)
			}
		} else {
			if (tableConfigs) {
				for (const tableConfig of tableConfigs) {
					this.addTableAutomatic(config, tableConfig)
				}
			} else {
				this.addTableAutomatic(config, { colums: data.scoreboard.columns as string[] } as TableConfig)
			}
		}

		this.data = new Data(data)
		this.displays.forEach(display => {
			display.init(this.data)
			display.sort()
		})

		// Set update interval
		if (this.updateInterval > 0) this.startUpdateInterval(true)
		document.addEventListener("visibilitychange", () => document.hidden
			? this.stopUpdateInterval() : this.startUpdateInterval())

		this.setLoadingStatus(false)
	}

	update() {
		// When nobody is online, assume scoreboard does not change
		if (this.data.nOnline > 0) {
			this.connection.getStats().then(data => {
				this.data.setStats(data)
				this.displays.forEach(display => display.updateStatsAndShow())
			}).catch(e => {
				console.error(e)
				console.warn(WebStats.CONNECTION_ERROR_MSG)
				this.setErrorMessage(WebStats.CONNECTION_ERROR_MSG)
				this.stopUpdateInterval()
			})
		} else {
			this.connection.getOnline().then(data => {
				this.data.setOnlineStatus(data)
				this.displays.forEach(display => display.updateOnlineStatusAndShow())
			}).catch(e => {
				console.error(e)
				console.warn(WebStats.CONNECTION_ERROR_MSG)
				this.setErrorMessage(WebStats.CONNECTION_ERROR_MSG)
				this.stopUpdateInterval()
			})
		}
	}

	startUpdateInterval(first?: boolean) {
		this.interval = setInterval(this.update.bind(this) as TimerHandler, this.updateInterval)
		if (!first) this.update()
	}

	stopUpdateInterval() {
		clearInterval(this.interval)
	}

	addTableManual(config, tableConfig: TableConfig) {
		let pagination: Pagination
		if (config.displayCount > 0 && config.tables[tableConfig.name ?? ""].pagination) {
			const paginationParent = config.tables[tableConfig.name ?? ""].pagination
			pagination = new Pagination(config.displayCount, paginationParent)
		}
		this.displays.push(new Display(
			{ ...config, table: config.tables[tableConfig.name ?? ""].table, pagination: pagination },
			tableConfig
		))
	}

	addTableAutomatic(config, tableConfig: TableConfig) {
		const headerElem = (config.tableParent as HTMLElement)
			.appendChild(document.createElement("div"))
		headerElem.classList.add("webstats-tableheading")
		if (tableConfig.name) {
			headerElem.innerText = tableConfig.name
			headerElem.setAttribute("webstats-table", tableConfig.name)
		}

		let pagination: Pagination
		if (config.displayCount > 0) {
			const paginationControls = headerElem.appendChild(document.createElement("span"))
			paginationControls.classList.add("webstats-pagination")
			pagination = Pagination.create(config.displayCount, paginationControls)
		}

		const tableElem = (config.tableParent as HTMLElement)
			.appendChild(document.createElement("table"))
		if (tableConfig.name) tableElem.setAttribute("webstats-table", tableConfig.name)
		this.displays.push(new Display(
			{ ...config, table: tableElem, pagination: pagination },
			tableConfig
		))
	}

	setLoadingStatus(loading) {
		if (!this.loadingElem) return
		this.loadingElem.style.display = loading ? "inline" : "none"
	}

	setErrorMessage(msg: string, config?) {
		if (this.errorElem) this.errorElem.innerText = msg
		else {
			const spanElem = document.createElement("span")
			spanElem.innerText = msg
			spanElem.classList.add("webstats-error-message")
			if (config?.tableParent) {
				config.tableParent.appendChild(spanElem)
			} else if (config?.tables) {
				for (const tablename in config.tables) {
					if (config.tables[tablename].table) config.tables[tablename].table.appendChild(spanElem)
				}
			}
		}
	}

}
