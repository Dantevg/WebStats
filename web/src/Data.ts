export type PlayerStatus = "online" | "afk" | "offline"
type Scoreboard = {
	entries: string[]
	scores: { [column: string]: { [entry: string]: string } }
	columns?: string[]
}
type Online = { [player: string]: boolean | "afk" }
type Entry = [number, string, ...(string)[]]

export default class Data {
	static BEDROCK_PREFIX = "."

	scoreboard: Scoreboard
	columns: string[]
	scores: Entry[]
	players: Online
	columns_: { [column: string]: number }
	units: { [column: string]: string }
	playernames: string[]

	constructor(data: { scoreboard: Scoreboard, online: Online, playernames: string[] }) {
		this.setStats(data)
	}

	get entries() { return this.scoreboard.entries }
	get online() { return this.players }
	get nOnline() { return Object.keys(this.players).length }

	isOnline = (player: string) => this.players[player] === true
	isAFK = (player: string) => this.players[player] === "afk"
	isOffline = (player: string) => !!this.players[player]
	getStatus = (player: string): PlayerStatus => this.isOnline(player) ? "online"
		: (this.isAFK(player) ? "afk" : "offline")
	isCurrentPlayer = (player: string) => this.playernames?.includes(player) ?? false

	setScoreboard(scoreboard: Scoreboard) {
		this.scoreboard = scoreboard
		this.columns = Object.keys(scoreboard.scores).sort()

		this.filter()

		this.scores = []
		for (const entryName of this.entries) {
			const entry = []
			entry.push(this.scores.push(entry as Entry) - 1)
			entry.push(entryName)
			for (const columnName of this.columns) {
				entry.push(this.scoreboard.scores[columnName]?.[entryName] ?? "")
			}
		}

		// Reverse-map column names to indices
		// (index 0 contains the original index, before sorting)
		this.columns_ = { Player: 1 }
		this.columns.forEach((val, idx) => this.columns_[val] = idx + 2)
	}
	setOnlineStatus(online: Online) { this.players = online }
	setPlayernames(playernames: string[]) { this.playernames = playernames }
	setUnits(units: { [column: string]: string }) { this.units = units }
	setStats(data: { scoreboard: Scoreboard, online: Online, playernames: string[], units?: { [column: string]: string } }) {
		this.setScoreboard(data.scoreboard)
		this.setOnlineStatus(data.online)
		this.setPlayernames(data.playernames)
		this.setUnits(data.units ?? {})
	}

	filter() {
		// Remove non-player / empty entries and sort
		this.scoreboard.entries = this.scoreboard.entries
			.filter(Data.isPlayerOrServer)
			.filter(this.isNonemptyEntry.bind(this))
			.sort(Intl.Collator().compare)

		// Remove empty columns
		this.scoreboard.scores = Data.filter(this.scoreboard.scores, Data.isNonemptyObjective)
	}

	sort(by: string, descending: boolean) {
		// Pre-create collator for significant performance improvement
		// over `a.localeCompare(b, undefined, {sensitivity: "base"})`
		// funny / weird thing: for localeCompare, supplying an empty `options`
		// object is way slower than supplying nothing...
		const collator = new Intl.Collator(undefined, { sensitivity: "base", numeric: true })

		// Case-insensitive compare as numbers or strings
		this.scores = this.scores.sort((a_row, b_row) =>
			(descending ? -1 : 1) * collator.compare(a_row[this.columns_[by]] as string, b_row[this.columns_[by]] as string))
	}

	// Ignore all entries which have no scores (armour stand book fix)
	// (also hides entries with only 0 values)
	isNonemptyEntry = (entry: string) => Object.entries(this.scoreboard.scores)
		.filter(([_, score]) => score[entry] && score[entry] != "0").length > 0

	// Valid player names only contain between 3 and 16 characters [A-Za-z0-9_],
	// entries with only digits are ignored as well (common for datapacks)
	static isPlayerOrServer = (entry: string) =>
		entry == "#server" || (entry.match(/^\w{3,16}$/) && !entry.match(/^\d*$/))
		|| Data.isBedrockPlayer(entry)

	// Whether this entry is a Bedrock player through Geyser/Floodgate
	static isBedrockPlayer = (entry: string) => entry.startsWith(Data.BEDROCK_PREFIX)

	// Whether any entry has a value for this objective
	static isNonemptyObjective = (objective: { [entry: string]: string | number }) =>
		Object.keys(objective).filter(Data.isPlayerOrServer).length > 0

	// Transform a Bedrock player's name mangled by Geyser/Floodgate back to a real name
	static transformBedrockPlayername = (entry: string) =>
		entry.substring(1).replaceAll("_", " ")

	// Array-like filter function for objects
	// https://stackoverflow.com/a/37616104
	static filter = <V>(obj: { [k: string]: V }, predicate: (_: V) => boolean): { [k: string]: V } =>
		Object.fromEntries(Object.entries(obj).filter(([_, v]) => predicate(v)))

	// Likewise, array-like map function for objects
	static map = <V>(obj: { [k: string]: V }, mapper: (k: string, v: V) => V) =>
		Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, mapper(k, v)]))

}
