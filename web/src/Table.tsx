import { Component } from "@itsjavi/jsx-runtime"
import { PlayerStatus } from "./Data"
import { convertFormattingCodes } from "./FormattingCodes"

const CONSOLE_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAPElEQVQ4T2NUUlL6z0ABYBw1gGE0DBioHAZ3795lUFZWJildosQCRQaQoxnkVLgL0A2A8dFpdP8NfEICAMkiK2HeQ9JUAAAAAElFTkSuQmCC"

type HeadingData = {
	columns: string[]
	showSkins: boolean
	sortColumn: string
	sortDescending: boolean
	onClick: (e: Event) => any
}

const columnClass = (column: string, sortColumn: string, sortDescending: boolean) =>
	sortColumn == column && ["webstats-sort-column", sortDescending ? "descending" : "ascending"]

const onKeyDown = (e: KeyboardEvent) => {
	if (e.key == "Enter") (e.target as HTMLElement).click()
}

export const Heading = ({ columns, showSkins, sortColumn, sortDescending, onClick }: HeadingData) => (
	<tr>
		<th colSpan={showSkins && 2} onClick={onClick} onKeyDown={onKeyDown} tabIndex="0" data-objective="Player" className={columnClass("Player", sortColumn, sortDescending)}>
			Player
		</th>
		{...columns.map(column => <th onClick={onClick} onKeyDown={onKeyDown} tabIndex="0" data-objective={column} className={columnClass(column, sortColumn, sortDescending)}>
			{column}
		</th>)}

const Avatar = ({ entry }: { entry: string }) => (
	<td className={["sticky", "skin"]}>
		<img
			title={entry}
			src={entry == "#server" ? CONSOLE_IMAGE : `https://www.mc-heads.net/avatar/${entry}.png`} />
	</td>
)

const Cell = ({ column, value }: { column: string, value: string }) => {
	if (value == undefined || value == "0") return <td data-objective={column} className="empty"></td>
	const formatted = isNaN(value as any) ? value : Number(value).toLocaleString()
	const coloured = convertFormattingCodes(formatted)
	return <td data-objective={column} data-value={value}>{coloured}</td>
}

const PlayerCell = ({ entry, status }: { entry: string, status: PlayerStatus }) => (
	<td data-objective="Player" data-value={entry}>
		<div className={["status", status]} title={status}></div>
		{entry == "#server" ? "Server" : entry}
	</td>
)

type RowData = {
	columns: string[]
	showSkins: boolean
	entry: string
	isCurrentPlayer: boolean
}

export class Row extends Component {
	values: Map<string, string> = new Map()
	status: PlayerStatus = "offline"

	constructor(public props: RowData) {
		super(props)
	}

	render = () => (
		<tr entry={this.props.entry} className={[this.status, this.props.isCurrentPlayer ? "current-player" : undefined]}>
			{this.props.showSkins && <Avatar entry={this.props.entry} />}
			<PlayerCell entry={this.props.entry} status={this.status} />
			{...this.props.columns.map(column => <Cell column={column} value={this.values.get(column)} />)}
		</tr>
	)
}
