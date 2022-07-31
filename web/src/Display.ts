import Data from "./Data"

export default class Display {
	table: HTMLTableElement
	sortBy: string
	descending: boolean
	showSkins: boolean
	displayCount: number
	hideOffline: boolean
	currentPage: number
	maxPage: number

	data: Data
	headerElem: HTMLTableRowElement
	rows: HTMLTableRowElement[]
	selectElem: HTMLSelectElement
	prevButton: HTMLButtonElement
	nextButton: HTMLButtonElement

	constructor({ table, sortBy = "Player", descending = false, showSkins = true, displayCount = 100 }) {
		this.table = table
		this.sortBy = sortBy
		this.descending = descending
		this.showSkins = showSkins
		this.displayCount = displayCount
		this.hideOffline = false
		this.currentPage = 1
	}

	init(data: Data) {
		this.data = data

		// Set pagination controls
		if (this.displayCount > 0) {
			this.initPagination()
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
		for (const column of this.data.columns) {
			Display.appendTh(this.headerElem, column, this.thClick.bind(this))
		}

		// Create rows of (empty) entries
		this.rows = []
		for (const entry of this.data.entries) {
			this.appendEntry(entry)
		}

		// Fill entries
		this.updateStats()
	}

	initPagination() {
		this.maxPage = Math.ceil(this.data.entries.length / this.displayCount)

		// Page selector
		this.selectElem = document.querySelector("select.webstats-pagination")
		if (this.selectElem) this.selectElem.onchange = (e) => this.changePage(Number((e.target as HTMLSelectElement).value))
		else console.warn("WebStats: no/invalid page control elements")

		// "Prev" button
		this.prevButton = document.querySelector("button.webstats-pagination[name=prev]")
		if (this.prevButton) this.prevButton.onclick = () => this.changePage(this.currentPage - 1)
		else console.warn("WebStats: no/invalid page control elements")

		// "Next" button
		this.nextButton = document.querySelector("button.webstats-pagination[name=next]")
		if (this.nextButton) this.nextButton.onclick = () => this.changePage(this.currentPage + 1)
		else console.warn("WebStats: no/invalid page control elements")

		this.updatePagination()
	}

	updatePagination() {
		const entries = this.hideOffline
			? this.data.entries.filter(entry => this.data.isOnline(entry))
			: this.data.entries
		this.maxPage = Math.ceil(entries.length / this.displayCount)

		// Page selector
		if (this.selectElem) {
			this.selectElem.innerHTML = ""
			for (let i = 1; i <= this.maxPage; i++) {
				const optionElem = document.createElement("option")
				optionElem.innerText = String(i)
				this.selectElem.append(optionElem)
			}
			this.selectElem.value = String(this.currentPage)
		}

		// "Prev" button
		if (this.prevButton) this.prevButton.toggleAttribute("disabled", this.currentPage <= 1)

		// "Next" button
		if (this.nextButton) this.nextButton.toggleAttribute("disabled", this.currentPage >= this.maxPage)
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
		let name = Display.appendTextElement(tr, "td", entry)
		name.setAttribute("objective", "Player")
		name.setAttribute("value", entry)

		// Prepend online/afk status
		let status = Display.prependElement(name, "div")
		status.classList.add("status")

		// Highlight current player
		if (this.data.isCurrentPlayer(entry)) tr.classList.add("current-player")

		// Append empty elements for alignment
		for (const objective of this.data.columns) {
			let td = Display.appendElement(tr, "td")
			td.classList.add("empty")
			td.setAttribute("objective", Display.quoteEscape(objective))
		}
		this.rows.push(tr)
	}

	setSkin(entry: string, row: HTMLTableRowElement) {
		const img = row.getElementsByTagName("img")[0]
		if (img) img.src = `https://www.mc-heads.net/avatar/${entry}.png`
	}

	updateScoreboard() {
		for (const row of this.data.scores) {
			for (const column of this.data.columns) {
				const value = row[this.data.columns_[column]] as string
				if (!value) continue
				const td = this.rows[row[0]].querySelector(`td[objective='${column}']`) as HTMLTableCellElement
				td.classList.remove("empty")
				td.setAttribute("value", value)
				td.innerText = isNaN(value as any) ? value : Number(value).toLocaleString()
			}
		}
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
		// Re-display if pagination is enabled
		if (this.displayCount > 0) this.show()
	}

	updateStats() {
		this.updateScoreboard()
		this.updateOnlineStatus()
	}

	// Change the page, re-display if `show` is not false and set page controls
	changePage(page: number, show?: boolean) {
		page = Math.max(1, Math.min(page, this.maxPage))
		this.currentPage = page
		if (show != false) this.show()

		if (this.displayCount > 0) this.updatePagination()
	}

	changeHideOffline(hideOffline: boolean) {
		this.hideOffline = hideOffline
		if (this.displayCount > 0) {
			this.updatePagination()
			this.changePage(1)
		}
	}

	// Re-display table contents
	show() {
		this.table.innerHTML = ""
		this.table.append(this.headerElem)
		const scores = this.hideOffline
			? this.data.scores.filter(row => this.data.isOnline(row[1]))
			: this.data.scores
		const min = (this.currentPage - 1) * this.displayCount
		const max = (this.displayCount > 0)
			? Math.min(this.currentPage * this.displayCount, scores.length)
			: scores.length
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
		if (this.displayCount > 0) this.changePage(1, false)
		this.sort()

		// Set URL query string, for sharing
		window.history.replaceState({}, "",
			location.pathname + "?sort=" + this.sortBy.replace(/\s/g, "+")
			+ "&order=" + (this.descending ? "desc" : "asc"))
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
