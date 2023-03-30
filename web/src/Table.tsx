import { Component } from "@itsjavi/jsx-runtime"
import { PlayerStatus } from "./Data"
import Display from "./Display"
import FormattingCodes from "./FormattingCodes"

type HeadingData = {
	columns: string[]
	showSkins: boolean
	onClick: (e: Event) => any
}

export const Heading = ({ columns, showSkins, onClick }: HeadingData) => (
	<tr>
		<th colSpan={showSkins && 2} onClick={onClick}>Player</th>
		{...columns.map(column => <th onClick={onClick}>{column}</th>)}
	</tr>
)

const Avatar = ({ entry }: { entry: string }) => (
	<td className={["sticky", "skin"]}>
		<img
			title={entry}
			src={entry == "#server" ? Display.CONSOLE_IMAGE : `https://www.mc-heads.net/avatar/${entry}.png`} />
	</td>
)

const Cell = ({ column, value }: { column: string, value: string }) => {
	const formatted = isNaN(value as any) ? value : Number(value).toLocaleString()
	const coloured = FormattingCodes.convertFormattingCodes(formatted ?? "")
	return (<td data-objective={column} data-value={value}>{coloured}</td>)
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

	render() {
		return (
			<tr entry={this.props.entry} className={[this.status, this.props.isCurrentPlayer ? "current" : undefined]}>
				{this.props.showSkins && <Avatar entry={this.props.entry} />}
				<PlayerCell entry={this.props.entry} status={this.status} />
				{...this.props.columns.map(column => <Cell column={column} value={this.values.get(column)} />)}
			</tr>
		)
	}
}
