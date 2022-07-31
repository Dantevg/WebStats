type Scoreboard = {
	entries: string[],
	scores: { [column: string]: { [entry: string]: string } },
	columns?: string[]
}
type Online = { [player: string]: boolean | "afk" }
type Entry = [number, string, ...(string)[]]

export default class Data {
	scoreboard: Scoreboard
	columns: string[]
	scores: Entry[]
	players: Online
	columns_: { [column: string]: number }
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
	getStatus = (player: string) => this.isOnline(player) ? "online"
		: (this.isAFK(player) ? "AFK" : "offline")
	isCurrentPlayer = (player: string) => this.playernames?.includes(player) ?? false

	setScoreboard(scoreboard: Scoreboard) {
		this.scoreboard = scoreboard
		this.columns = scoreboard.columns
			?? Object.keys(scoreboard.scores).sort()

		this.filter()

		this.scores = []
		for (const entryName of this.entries) {
			const entry = []
			entry.push(this.scores.push(entry as Entry) - 1)
			entry.push(entryName)
			for (const columnName of this.columns) {
				entry.push(this.scoreboard.scores[columnName]?.[entryName] ?? 0)
			}
		}

		// Reverse-map column names to indices
		// (index 0 contains the original index, before sorting)
		this.columns_ = { Player: 1 }
		this.columns.forEach((val, idx) => this.columns_[val] = idx + 2)
	}
	setOnlineStatus(online: Online) { this.players = online }
	setPlayernames(playernames: string[]) { this.playernames = playernames }
	setStats(data: { scoreboard: Scoreboard, online: Online, playernames: string[] }) {
		this.setScoreboard(data.scoreboard)
		this.setOnlineStatus(data.online)
		this.setPlayernames(data.playernames)
	}

	filter() {
		// Remove non-player / empty entries and sort
		this.scoreboard.entries = this.scoreboard.entries
			.filter(Data.isPlayer)
			.filter(this.isNonemptyEntry.bind(this))
			.sort(Intl.Collator().compare)

		// Remove empty columns
		this.scoreboard.scores = Data.filter(this.scoreboard.scores, Data.isNonemptyObjective)

		// Filter out Minecraft colour codes
		this.scoreboard.scores = Data.map(this.scoreboard.scores,
			(_, col) => Data.map(col, Data.stripColourCodes))
	}

	sort(by: string, descending: boolean) {
		// Pre-create collator for significant performance improvement
		// over `a.localeCompare(b, undefined, {sensitivity: "base"})`
		// funny / weird thing: for localeCompare, supplying an empty `options`
		// object is way slower than supplying nothing...
		const collator = new Intl.Collator(undefined, { sensitivity: "base" })

		// When a and b are both numbers, compare as numbers.
		// Otherwise, case-insensitive compare as string
		this.scores = this.scores.sort((a_row, b_row) => {
			const a = a_row[this.columns_[by]]
			const b = b_row[this.columns_[by]]
			if (!isNaN(Number(a)) && !isNaN(Number(b))) {
				return (descending ? -1 : 1) * ((a as number) - (b as number))
			} else {
				return (descending ? -1 : 1) * collator.compare(a as string, b as string)
			}
		})
	}

	// Ignore all entries which have no scores (armour stand book fix)
	// (also hides entries with only 0 values)
	isNonemptyEntry = (entry: string) => Object.entries(this.scoreboard.scores)
		.filter(([_, score]) => score[entry] && score[entry] != "0").length > 0

	// Valid player names only contain between 3 and 16 characters [A-Za-z0-9_],
	// entries with only digits are ignored as well (common for datapacks)
	static isPlayer = (entry: string) => entry.match(/^\w{3,16}$/) && !entry.match(/^\d*$/)

	// Whether any entry has a value for this objective
	static isNonemptyObjective = (objective: { [entry: string]: string | number }) =>
		Object.keys(objective).filter(Data.isPlayer).length > 0

	// Remove Minecraft colour codes from a string
	// (§ followed by a single character, but not when preceded by a backslash)
	static stripColourCodes = (_: any, str: string) => str.replace(/(?<!\\)(§.)/gm, "")

	// Array-like filter function for objects
	// https://stackoverflow.com/a/37616104
	static filter = <V>(obj: { [k: string]: V }, predicate: (_: V) => boolean): { [k: string]: V } =>
		Object.fromEntries(Object.entries(obj).filter(([_, v]) => predicate(v)))

	// Likewise, array-like map function for objects
	static map = <V>(obj: { [k: string]: V }, mapper: (k: string, v: V) => V) =>
		Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, mapper(k, v)]))

}
