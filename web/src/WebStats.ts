import Connection from "./Connection"
import Data from "./Data"
import Display from "./Display"
import { convertFormattingCodes } from "./FormattingCodes"
import Pagination from "./Pagination"

export { Connection, Data, Display, Pagination, convertFormattingCodes }

declare global {
	interface Window { webstats: WebStats }
}

export type Direction = "ascending" | "descending"

export type TableConfig = {
	name?: string
	columns?: string[]
	sortColumn?: string
	sortDirection?: Direction
}

export type WebStatsConfig = {
	// Either one of these
	host?: string
	connection?: Connection

	// Either one of these
	tableParent?: HTMLTableElement
	tables?: { [key: string]: { table: HTMLTableElement, pagination?: HTMLElement } }

	updateInterval?: number
	showSkins?: boolean
	displayCount?: number
}

export default class WebStats {
	static CONNECTION_ERROR_MSG = "No connection to server. Maybe the server is offline, or the 'host' setting in index.html is incorrect."

	displays: Display[]
	connection: Connection
	data: Data
	updateInterval: number
	interval: number

	loadingElem?: HTMLElement
	errorElem?: HTMLElement

	constructor(config: WebStatsConfig) {
		this.displays = []
		this.connection = config.connection ?? Connection.json(config.host)
		this.updateInterval = config.updateInterval ?? 10000

		// Status HTML elements
		const optionHideOffline = document.querySelector("input.webstats-option#hide-offline") as HTMLInputElement
		const statusElem = document.querySelector(".webstats-status")
		this.loadingElem = statusElem?.querySelector(".webstats-loading-indicator")
		this.errorElem = statusElem?.querySelector(".webstats-error-message")
		this.setLoadingStatus(true)

		// Get data and init
		const statsPromise = this.connection.getStats()
		const tableConfigsPromise = this.connection.getTables()
		Promise.all([statsPromise, tableConfigsPromise])
			.then(
				([stats, tableConfigs]) => this.init(stats, tableConfigs, config, optionHideOffline.checked),
				this.catchError(WebStats.CONNECTION_ERROR_MSG, config)
			)
			.catch(this.catchError(undefined, config))

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

		optionHideOffline?.addEventListener("change", (e) => {
			this.displays.forEach(display => display.changeHideOffline(optionHideOffline.checked))
		})

		window.webstats = this
	}

	init(data, tableConfigs: TableConfig[] | undefined, config: WebStatsConfig, hideOffline: boolean) {
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
			display.hideOffline = hideOffline
			display.show()
		})

		// Set update interval
		if (this.updateInterval > 0) {
			this.startUpdateInterval(true)
			document.addEventListener("visibilitychange", () => document.hidden
				? this.stopUpdateInterval() : this.startUpdateInterval())
		}

		this.setLoadingStatus(false)
	}

	update() {
		// When nobody is online, assume scoreboard does not change
		if (this.data.nOnline > 0) {
			this.connection.getStats().then(data => {
				this.data.setStats(data)
				this.displays.forEach(display => display.updateStatsAndShow())
			}).catch(this.catchError(WebStats.CONNECTION_ERROR_MSG))
		} else {
			this.connection.getOnline().then(data => {
				this.data.setOnlineStatus(data)
				this.displays.forEach(display => display.updateOnlineStatusAndShow())
			}).catch(this.catchError(WebStats.CONNECTION_ERROR_MSG))
		}
	}

	startUpdateInterval(first?: boolean) {
		this.interval = setInterval(this.update.bind(this) as TimerHandler, this.updateInterval)
		if (!first) this.update()
	}

	stopUpdateInterval() {
		clearInterval(this.interval)
	}

	addTableManual(config: WebStatsConfig, tableConfig: TableConfig) {
		let pagination: Pagination
		if (config.displayCount > 0 && config.tables[tableConfig.name ?? ""].pagination) {
			const paginationParent = config.tables[tableConfig.name ?? ""].pagination
			pagination = new Pagination(config.displayCount, paginationParent)
		}
		this.displays.push(new Display(
			{ ...config, table: config.tables[tableConfig.name ?? ""].table, pagination: pagination, serverIconURL: this.connection.serverIcon },
			tableConfig
		))
	}

	addTableAutomatic(config: WebStatsConfig, tableConfig: TableConfig) {
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
			{ ...config, table: tableElem, pagination: pagination, serverIconURL: this.connection.serverIcon },
			tableConfig
		))
	}

	setLoadingStatus(loading: boolean) {
		if (!this.loadingElem) return
		this.loadingElem.style.display = loading ? "inline" : "none"
	}

	setErrorMessage(msg: string, config?: WebStatsConfig) {
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

	catchError(msg?: string, config?: WebStatsConfig) {
		const self = this
		return e => {
			console.error(e)
			if (msg) console.warn(msg)
			self.setErrorMessage(msg ?? e, config)
			self.setLoadingStatus(false)
			self.stopUpdateInterval()
		}
	}

}
