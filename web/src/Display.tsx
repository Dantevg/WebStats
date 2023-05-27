import { ijJSX, render } from "@itsjavi/jsx-runtime"
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
	rows: Map<string, Row>

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
		const scores = this.getScores()
		const [min, max] = this.pagination
			? this.pagination.getRange(scores.length)
			: [0, scores.length]

		const rows: ijJSX.Node[] = []
		rows.push(<Heading
			columns={this.columns ?? this.data.columns}
			showSkins={this.showSkins}
			sortColumn={this.sortColumn}
			sortDescending={this.descending}
			onClick={this.thClick.bind(this)} />)
		for (let i = min; i < max; i++) {
			rows.push(this.rows.get(scores[i][1]))
		}
		render(<>{rows}</>, this.table)
	}

	// When a table header is clicked, sort by that header
	thClick(e: Event) {
		let objective = (e.target as HTMLTableCellElement).getAttribute("data-objective")
		this.descending = (objective === this.sortColumn) ? !this.descending : true
		this.sortColumn = objective
		this.pagination?.changePage(1)
		this.show()
	}

	// Replace single quotes by '&quot;' (html-escape)
	static quoteEscape = (string: string) => string.replace(/'/g, "&quot;")

}
