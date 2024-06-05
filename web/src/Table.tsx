import { Component } from "@itsjavi/jsx-runtime"
import Data, { PlayerStatus } from "./Data"
import { convertFormattingCodes } from "./FormattingCodes"
import { autoConvertUnits } from "./Units"

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

// Transform an entry name into the name to be displayed
const transformEntryName = (entry: string) => {
	if (entry == "#server") return "Server"
	else if (Data.isBedrockPlayer(entry)) return Data.transformBedrockPlayername(entry)
	else return entry
}

const getSkin = (entry: string, skin?: string) => {
	if (skin != undefined && skin.startsWith("http")) {
		return skin
	} else if (skin != undefined) {
		return `https://www.mc-heads.net/avatar/${skin}/64.png`
	} else if (entry != "#server") {
		return `https://www.mc-heads.net/avatar/${entry}/64.png`
	}
}

export const Heading = ({ columns, showSkins, sortColumn, sortDescending, onClick }: HeadingData) => (
	<tr>
		<th colSpan={showSkins && 2} onClick={onClick} onKeyDown={onKeyDown} tabIndex="0" data-objective="Player" className={columnClass("Player", sortColumn, sortDescending)}>
			Player
		</th>
		{...columns.map(column => <th onClick={onClick} onKeyDown={onKeyDown} tabIndex="0" data-objective={column} className={columnClass(column, sortColumn, sortDescending)}>
			{column}
		</th>)}
	</tr>)

const Avatar = ({ entry, skin }: { entry: string, skin?: string }) => (
	<td className={["sticky", "skin"]} title={entry}>
		<img title={entry} src={getSkin(entry, skin)} alt=" " />
	</td>
)

const Cell = ({ column, value, relative, unit }: { column: string, value: string, relative?: number, unit?: string }) => {
	if (value == undefined || value == "0") return <td data-objective={column} className="empty"></td>
	const coloured = convertFormattingCodes(autoConvertUnits(value, unit))
	return <td
		data-objective={column}
		data-value={value}
		data-unit={unit}
		title={unit ? `${value} ${unit}` : value}
		style={relative && `--relative: ${relative}%;`}>
		{coloured}
	</td>
}

const PlayerCell = ({ entry, status, name }: { entry: string, status: PlayerStatus, name?: string }) => (
	<td data-objective="Player" data-value={entry} title={entry}>
		<div className={["status", status]} title={status}></div>
		<>{name ? convertFormattingCodes(name) : transformEntryName(entry)}</>
	</td>
)

type RowData = {
	columns: string[]
	units: { [column: string]: string }
	showSkins: boolean
	skin?: string
	entry: string
	isCurrentPlayer: boolean
}

export class Row extends Component {
	values: Map<string, string> = new Map()
	relative: Map<string, number> = new Map()
	status: PlayerStatus = "offline"

	constructor(public props: RowData) {
		super(props)
	}

	render = () => (
		<tr entry={this.props.entry} className={[this.status, this.props.isCurrentPlayer ? "current-player" : undefined]}>
			{this.props.showSkins ? <Avatar entry={this.props.entry} skin={this.props.skin} /> : <></>}
			<PlayerCell entry={this.props.entry} status={this.status} name={this.values.get("Player")} />
			{...this.props.columns.map(column => <Cell column={column} value={this.values.get(column)} relative={this.relative.get(column)} unit={this.props.units[column]} />)}
		</tr>
	)
}
