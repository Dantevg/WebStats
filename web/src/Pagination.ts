export default class Pagination {
	maxPage: number
	displayCount: number
	currentPage: number

	parentElem: HTMLElement
	selectElem: HTMLSelectElement
	prevButton: HTMLButtonElement
	nextButton: HTMLButtonElement

	onPageChange: (page: number) => void

	constructor(displayCount: number, elem: HTMLElement) {
		this.displayCount = displayCount
		this.currentPage = 1
		
		this.parentElem = elem
		this.selectElem = elem.querySelector("select.webstats-pagination[name=page]")
		this.prevButton = elem.querySelector("button.webstats-pagination[name=prev]")
		this.nextButton = elem.querySelector("button.webstats-pagination[name=next]")

		this.selectElem.addEventListener("change",
			(e) => this.changePageAndCallback(Number((e.target as HTMLSelectElement).value)))
		this.prevButton.addEventListener("click",
			() => this.changePageAndCallback(this.currentPage - 1))
		this.nextButton.addEventListener("click",
			() => this.changePageAndCallback(this.currentPage + 1))
	}

	static create(displayCount: number, elem: HTMLElement) {
		const prevButton = elem.appendChild(document.createElement("button"))
		prevButton.classList.add("webstats-pagination")
		prevButton.name = "prev"
		prevButton.innerText = "Prev"

		const pageSelect = elem.appendChild(document.createElement("select"))
		pageSelect.classList.add("webstats-pagination")
		pageSelect.name = "page"

		const nextButton = elem.appendChild(document.createElement("button"))
		nextButton.classList.add("webstats-pagination")
		nextButton.name = "next"
		nextButton.innerText = "Next"

		return new Pagination(displayCount, elem)
	}

	update(nEntries: number) {
		this.maxPage = Math.ceil(nEntries / this.displayCount)
		
		// Hide all controls when there is only one page
		if (this.maxPage == 1) {
			this.parentElem.classList.add("pagination-hidden")
		} else {
			this.parentElem.classList.remove("pagination-hidden")
		}

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

	changePage(page: number) {
		page = Math.max(1, Math.min(page, this.maxPage))
		this.currentPage = page
	}

	changePageAndCallback(page: number) {
		this.changePage(page)
		console.log("callback")
		if (this.onPageChange) this.onPageChange(this.currentPage)
	}

	getRange(nEntries?: number): [number, number] {
		const min = (this.currentPage - 1) * this.displayCount
		const max = (this.displayCount > 0)
			? Math.min(this.currentPage * this.displayCount, nEntries)
			: nEntries
		return [min, max]
	}

}
