import Data from "./Data"
import FormattingCodes from "./FormattingCodes"
import Pagination from "./Pagination"
import { Direction, TableConfig } from "./WebStats"

export default class Display {
	table: HTMLTableElement
	pagination?: Pagination
	columns: string[]
	sortColumn: string
	descending: boolean
	showSkins: boolean
	hideOffline: boolean

	data: Data
	headerElem: HTMLTableRowElement
	rows: Map<string, HTMLTableRowElement>

	static CONSOLE_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAPElEQVQ4T2NUUlL6z0ABYBw1gGE0DBioHAZ3795lUFZWJildosQCRQaQoxnkVLgL0A2A8dFpdP8NfEICAMkiK2HeQ9JUAAAAAElFTkSuQmCC"

	constructor({ table, pagination, showSkins = true }, { columns, sortColumn = "Player", sortDirection = "descending" }: TableConfig) {
		this.table = table
		this.pagination = pagination
		this.columns = columns
		this.sortColumn = sortColumn
		this.descending = sortDirection == "descending"
		this.showSkins = showSkins
		this.hideOffline = false

		if (this.pagination) this.pagination.onPageChange = (page) => {
			this.updatePagination()
			this.show()
		}
	}

	init(data: Data) {
		this.data = data

		// Set pagination controls
		if (this.pagination) this.updatePagination()

		// Create header of columns
		this.headerElem = document.createElement("tr")
		this.table.append(this.headerElem)
		Display.appendTh(this.headerElem, "Player", this.thClick.bind(this),
			this.showSkins ? 2 : undefined)
		for (const column of this.columns ?? this.data.columns) {
			Display.appendTh(this.headerElem, column, this.thClick.bind(this))
		}

		// Create rows of (empty) entries
		this.rows = new Map()
		for (const entry of this.getEntries()) {
			this.appendEntry(entry)
		}

		// Fill entries
		this.updateStatsAndShow()
	}

	getEntries() {
		const entriesHere = this.data.entries.filter((entry: string) =>
			(this.columns ?? this.data.columns).some((column: string) =>
				this.data.scoreboard.scores[column]?.[entry]
				&& this.data.scoreboard.scores[column][entry] != "0"))

		return this.hideOffline
			? entriesHere.filter(entry => this.data.isOnline(entry))
			: entriesHere
	}

	getScores() {
		const scoresHere = this.data.scores.filter(row => this.rows.has(row[1]))

		return this.hideOffline
			? scoresHere.filter(row => this.data.isOnline(row[1]))
			: scoresHere
	}

	updatePagination() {
		this.pagination.update(this.getEntries().length)
	}

	appendEntry(entry: string) {
		let tr = document.createElement("tr")
		tr.setAttribute("entry", Display.quoteEscape(entry))

		// Append skin image
		if (this.showSkins) {
			let img = Display.appendElement(tr, "td")
			Display.appendImg(img, "")
			img.classList.add("sticky", "skin")
			img.setAttribute("title", entry)
		}

		// Append player name
		let name = Display.appendTextElement(tr, "td", this.transformEntryName(entry))
		name.setAttribute("objective", "Player")
		name.setAttribute("value", entry)

		// Prepend online/afk status
		let status = Display.prependElement(name, "div")
		status.classList.add("status")

		// Highlight current player
		if (this.data.isCurrentPlayer(entry)) tr.classList.add("current-player")

		// Append empty elements for alignment
		for (const column of this.columns ?? this.data.columns) {
			let td = Display.appendElement(tr, "td")
			td.classList.add("empty")
			td.setAttribute("objective", Display.quoteEscape(column))
		}
		this.rows.set(entry, tr)
	}

	setSkin(entry: string, row: HTMLTableRowElement) {
		const img = row.getElementsByTagName("img")[0]
		if (img) {
			if (entry == "#server") img.src = Display.CONSOLE_IMAGE
			else img.src = `https://www.mc-heads.net/avatar/${entry}.png`
		}
	}

	updateScoreboard() {
		for (const row of this.data.scores) {
			for (const column of this.columns ?? this.data.columns) {
				let value = row[this.data.columns_[column]] as string
				if (!value) continue
				const td = this.rows.get(row[1]).querySelector(`td[objective='${column}']`) as HTMLTableCellElement
				td.classList.remove("empty")
				td.setAttribute("value", value)

				// Convert numbers to locale
				value = isNaN(value as any) ? value : Number(value).toLocaleString()

				// Convert Minecraft formatting codes
				td.innerHTML = ""
				td.append(...FormattingCodes.convertFormattingCodes(value))
			}
		}
	}

	updateScoreboardAndShow() {
		this.updateScoreboard()
		this.show()
	}

	updateOnlineStatus() {
		for (const [_, row] of this.rows) {
			const statusElement = row.querySelector("td .status")
			if (!statusElement) continue
			const entry = row.getAttribute("entry")
			row.classList.remove("online", "afk", "offline")
			statusElement.classList.remove("online", "afk", "offline")

			const status = this.data.getStatus(entry)
			row.classList.add(status.toLowerCase())
			statusElement.classList.add(status.toLowerCase())
			statusElement.setAttribute("title", this.data.getStatus(entry))
		}
	}

	updateOnlineStatusAndShow() {
		this.updateOnlineStatus()
		if (this.pagination && this.hideOffline) this.show()
	}

	updateStats() {
		this.updateScoreboard()
		this.updateOnlineStatus()
	}

	updateStatsAndShow() {
		this.updateScoreboard()
		this.updateOnlineStatus()
		this.show()
	}

	changeHideOffline(hideOffline: boolean) {
		this.hideOffline = hideOffline
		if (this.pagination) {
			this.pagination.changePage(1)
			this.show()
		}
	}

	// Re-display table contents
	show() {
		this.data.sort(this.sortColumn, this.descending)
		this.table.innerHTML = ""
		this.table.append(this.headerElem)
		const scores = this.getScores()
		const [min, max] = this.pagination
			? this.pagination.getRange(scores.length)
			: [0, scores.length]
		for (let i = min; i < max; i++) {
			if (this.showSkins) this.setSkin(scores[i][1], this.rows.get(scores[i][1]))
			this.table.append(this.rows.get(scores[i][1]))
		}
	}

	// When a table header is clicked, sort by that header
	thClick(e: Event) {
		let objective = (e.target as HTMLTableCellElement).innerText
		this.descending = (objective === this.sortColumn) ? !this.descending : true
		this.sortColumn = objective
		if (this.pagination) this.pagination.changePage(1)
		this.show()
	}
	
	// Transform an entry name into the name to be displayed
	transformEntryName = (entry: string) => {
		if (entry == "#server") return "Server"
		else if (this.data.isBedrockPlayer(entry)) return Data.transformBedrockPlayername(entry)
		else return entry
	}

	// Replace single quotes by '&quot;' (html-escape)
	static quoteEscape = (string: string) => string.replace(/'/g, "&quot;")

	static appendElement<K extends keyof HTMLElementTagNameMap>(base: HTMLElement, type: K) {
		let el = document.createElement(type)
		base.append(el)
		return el
	}

	static prependElement<K extends keyof HTMLElementTagNameMap>(base: HTMLElement, type: K) {
		let el = document.createElement(type)
		document.createElement
		base.prepend(el)
		return el
	}

	static appendTextElement<K extends keyof HTMLElementTagNameMap>(base: HTMLElement, type: K, name: string) {
		let el = Display.appendElement(base, type)
		el.innerText = name
		return el
	}

	static appendTh(base: HTMLElement, name: string, onclick: (this, ev) => any, colspan?: number) {
		let th = Display.appendTextElement(base, "th", name)
		th.onclick = onclick
		if (colspan != undefined) th.setAttribute("colspan", String(colspan))
		return th
	}

	static appendImg(base: HTMLElement, src: string) {
		let img = Display.appendElement(base, "img")
		img.src = src
		return img
	}

}
