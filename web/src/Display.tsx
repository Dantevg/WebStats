import { render } from "@itsjavi/jsx-runtime"
import Data from "./Data"
import Pagination from "./Pagination"
import { Heading, Row } from "./Table"
import { TableConfig } from "./WebStats"

export default class Display {
	table: HTMLTableElement
	pagination?: Pagination
	columns: string[]
	sortColumn: string
	descending: boolean
	showSkins: boolean
	hideOffline: boolean

	data: Data
	headerElem: JSX.Element
	rows: Map<string, Row>

	static CONSOLE_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAPElEQVQ4T2NUUlL6z0ABYBw1gGE0DBioHAZ3795lUFZWJildosQCRQaQoxnkVLgL0A2A8dFpdP8NfEICAMkiK2HeQ9JUAAAAAElFTkSuQmCC"

	constructor({ table, pagination, showSkins = true }, { columns, sortColumn = "Player", sortDirection = "descending" }: TableConfig) {
		this.table = table
		this.pagination = pagination
		this.columns = columns
		this.sortColumn = sortColumn
		this.descending = sortDirection == "descending"
		this.showSkins = showSkins
		this.hideOffline = false

		if (this.pagination) this.pagination.onPageChange = (page) => this.show()
	}

	init(data: Data) {
		this.data = data

		// Set pagination controls
		if (this.pagination) this.updatePagination()

		// Create header of columns
		this.headerElem = <Heading
			columns={this.columns ?? this.data.columns}
			showSkins={this.showSkins}
			onClick={this.thClick.bind(this)} />

		// Create rows of (empty) entries
		this.rows = new Map()
		for (const entry of this.getEntries()) {
			this.rows.set(entry, new Row({
				columns: this.columns ?? this.data.columns,
				showSkins: this.showSkins,
				entry,
				isCurrentPlayer: this.data.isCurrentPlayer(entry)
			}))
		}

		// Fill entries
		this.updateStats()
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
				this.rows.get(row[1]).values.set(column, value)
			}
		}
	}

	updateScoreboardAndShow() {
		this.updateScoreboard()
		this.show()
	}

	updateOnlineStatus() {
		for (const [_, row] of this.rows) {
			row.status = this.data.getStatus(row.props.entry)
		}
	}

	updateOnlineStatusAndShow() {
		this.updateOnlineStatus()
		if (this.pagination) this.show()
	}

	updateStats() {
		this.updateScoreboard()
		this.updateOnlineStatus()
	}

	updateStatsAndShow() {
		this.updateStats()
		this.show()
	}

	changeHideOffline(hideOffline: boolean) {
		this.hideOffline = hideOffline
		if (this.pagination) {
			this.updatePagination()
			this.pagination.changePage(1)
			this.show()
		}
	}

	// Re-display table contents
	show() {
		this.data.sort(this.sortColumn, this.descending)
		this.table.innerHTML = ""
		render(this.headerElem, this.table)
		const scores = this.getScores()
		const [min, max] = this.pagination
			? this.pagination.getRange(scores.length)
			: [0, scores.length]
		for (let i = min; i < max; i++) {
			render(this.rows.get(scores[i][1]).render(), this.table)
		}
	}

	// When a table header is clicked, sort by that header
	thClick(e: Event) {
		let objective = (e.target as HTMLTableCellElement).innerText
		this.descending = (objective === this.sortColumn) ? !this.descending : true
		this.sortColumn = objective
		this.pagination?.changePage(1)
		this.show()
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
