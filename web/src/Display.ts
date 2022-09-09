import Data from "./Data"
import Pagination from "./Pagination"

export default class Display {
	table: HTMLTableElement
	pagination?: Pagination
	columns: string[]
	sortBy: string
	descending: boolean
	showSkins: boolean
	hideOffline: boolean

	data: Data
	headerElem: HTMLTableRowElement
	rows: HTMLTableRowElement[]

	static COLOUR_CODES = {
		["§0"]: "black",
		["§1"]: "dark_blue",
		["§2"]: "dark_green",
		["§3"]: "dark_aqua",
		["§4"]: "dark_red",
		["§5"]: "dark_purple",
		["§6"]: "gold",
		["§7"]: "gray",
		["§8"]: "dark_gray",
		["§9"]: "blue",
		["§a"]: "green",
		["§b"]: "aqua",
		["§c"]: "red",
		["§d"]: "light_purple",
		["§e"]: "yellow",
		["§f"]: "white",
	}

	static FORMATTING_CODES = {
		["§k"]: "obfuscated",
		["§l"]: "bold",
		["§m"]: "strikethrough",
		["§n"]: "underline",
		["§o"]: "italic",
		["§r"]: "reset",
	}

	// § followed by a single character, or of the form §x§r§r§g§g§b§b
	// (also capture rest of string, until next §)
	static FORMATTING_CODE_REGEX = /(§x§.§.§.§.§.§.|§.)([^§]*)/gm

	static CONSOLE_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAPElEQVQ4T2NUUlL6z0ABYBw1gGE0DBioHAZ3795lUFZWJildosQCRQaQoxnkVLgL0A2A8dFpdP8NfEICAMkiK2HeQ9JUAAAAAElFTkSuQmCC"

	constructor({ table, pagination, showSkins = true }, { columns = [], sortBy = "Player", sortDescending = false }) {
		this.table = table
		this.pagination = pagination
		this.columns = columns
		this.sortBy = sortBy
		this.descending = sortDescending
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
		if (this.pagination) {
			this.updatePagination()
		} else {
			// Hide pagination controls when pagination is disabled
			const paginationSpanElem = document.querySelector("span.webstats-pagination") as HTMLElement
			if (paginationSpanElem) paginationSpanElem.style.display = "none"
		}

		// Create header of columns
		this.headerElem = document.createElement("tr")
		this.table.append(this.headerElem)
		Display.appendTh(this.headerElem, "Player", this.thClick.bind(this),
			this.showSkins ? 2 : undefined)
		for (const column of this.columns) {
			Display.appendTh(this.headerElem, column, this.thClick.bind(this))
		}

		// Create rows of (empty) entries
		this.rows = []
		for (const entry of this.data.entries) {
			this.appendEntry(entry)
		}

		// Fill entries
		this.updateStatsAndShow()
	}

	updatePagination() {
		const entries = this.hideOffline
			? this.data.entries.filter(entry => this.data.isOnline(entry))
			: this.data.entries
		this.pagination.update(entries.length)
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
		let name = Display.appendTextElement(tr, "td", entry == "#server" ? "Server" : entry)
		name.setAttribute("objective", "Player")
		name.setAttribute("value", entry)

		// Prepend online/afk status
		let status = Display.prependElement(name, "div")
		status.classList.add("status")

		// Highlight current player
		if (this.data.isCurrentPlayer(entry)) tr.classList.add("current-player")

		// Append empty elements for alignment
		for (const column of this.columns) {
			let td = Display.appendElement(tr, "td")
			td.classList.add("empty")
			td.setAttribute("objective", Display.quoteEscape(column))
		}
		this.rows.push(tr)
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
			for (const column of this.columns) {
				let value = row[this.data.columns_[column]] as string
				if (!value) continue
				const td = this.rows[row[0]].querySelector(`td[objective='${column}']`) as HTMLTableCellElement
				td.classList.remove("empty")
				td.setAttribute("value", value)

				// Convert numbers to locale
				value = isNaN(value as any) ? value : Number(value).toLocaleString()

				// Convert Minecraft formatting codes
				td.innerHTML = ""
				td.append(...Display.convertFormattingCodes(value))
			}
		}
	}
	
	updateScoreboardAndShow() {
		this.updateScoreboard()
		this.show()
	}

	updateOnlineStatus() {
		for (const row of this.rows) {
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
		this.table.innerHTML = ""
		this.table.append(this.headerElem)
		const scores = this.hideOffline
			? this.data.scores.filter(row => this.data.isOnline(row[1]))
			: this.data.scores
		const [min, max] = this.pagination
			? this.pagination.getRange(scores.length)
			: [0, scores.length]
		for (let i = min; i < max; i++) {
			if (this.showSkins) this.setSkin(scores[i][1], this.rows[scores[i][0]])
			this.table.append(this.rows[scores[i][0]])
		}
	}

	// Sort a HTML table element
	sort(by: string = this.sortBy, descending: boolean = this.descending) {
		this.data.sort(by, descending)
		this.show()
	}

	// When a table header is clicked, sort by that header
	thClick(e: Event) {
		let objective = (e.target as HTMLTableCellElement).innerText
		this.descending = (objective === this.sortBy) ? !this.descending : true
		this.sortBy = objective
		if (this.pagination) this.pagination.changePage(1)
		this.sort()
	}

	// Replace single quotes by '&quot;' (html-escape)
	static quoteEscape = (string: string) => string.replace(/'/g, "&quot;")

	// Replace all formatting codes by <span> elements
	static convertFormattingCodes = (value) =>
		Display.parseFormattingCodes(value).map(Display.convertFormattingCode)

	// Convert a single formatting code to a <span> element
	static convertFormattingCode(part) {
		if (!part.format && !part.colour) return part.text

		const span = document.createElement("span")
		span.innerText = part.text
		span.classList.add("mc-format")

		if (part.format) span.classList.add("mc-" + part.format)
		if (part.colour) {
			if (part.colourType == "simple") span.classList.add("mc-" + part.colour)
			if (part.colourType == "hex") span.style.color = part.colour
		}

		return span
	}

	static parseFormattingCodes(value) {
		const parts = []

		const firstIdx = value.matchAll(Display.FORMATTING_CODE_REGEX).next().value?.index
		if (firstIdx == undefined || firstIdx > 0) {
			parts.push({ text: value.substring(0, firstIdx) })
		}

		for (const match of value.matchAll(Display.FORMATTING_CODE_REGEX)) {
			parts.push(Display.parseFormattingCode(match[1], match[2], parts[parts.length - 1]))
		}

		return parts
	}

	static parseFormattingCode(code, text, prev) {
		// Simple colour codes and formatting codes
		if (Display.COLOUR_CODES[code]) {
			return {
				text,
				colour: Display.COLOUR_CODES[code],
				colourType: "simple",
			}
		}
		if (Display.FORMATTING_CODES[code]) {
			return {
				text,
				format: Display.FORMATTING_CODES[code],
				colour: prev?.colour,
				colourType: prev?.colourType,
			}
		}

		// Hex colour codes
		const matches = code.match(/§x§(.)§(.)§(.)§(.)§(.)§(.)/m)
		if (matches) {
			return {
				text,
				colour: "#" + matches.slice(1).join(""),
				colourType: "hex",
			}
		}

		// Not a valid formatting code, just return the input unaltered
		return { text }
	}

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
