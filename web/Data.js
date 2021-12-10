class Data {
	constructor(data){
		this.scoreboard = data.scoreboard
		this.players = data.online
		this.columns = data.scoreboard.columns
			?? Object.keys(data.scoreboard.scores).sort()
		
		// Reverse-map column names to indices
		// (index 0 contains the original index, before sorting)
		this.columns_ = {Player: 1}
		this.columns.forEach((val, idx) => this.columns_[val] = idx + 2)
		
		this.filter()
		
		this.scores = []
		for(const entryName of this.entries){
			const entry = []
			entry.push(this.scores.push(entry) - 1)
			entry.push(entryName)
			for(const columnName of this.columns){
				entry.push(this.scoreboard.scores[columnName][entryName])
			}
		}
	}
	
	get entries(){ return this.scoreboard.entries }
	get online(){ return this.players }
	get nOnline(){ return Object.keys(this.players).length }
	
	isOnline = player => this.players[player] === true
	isAFK = player => this.players[player] === "afk"
	isOffline = player => !!this.players[player]
	getStatus = player => this.isOnline(player) ? "online"
		: (this.isAFK(player) ? "AFK" : "offline")
	
	setScoreboard(scoreboard){
		this.scoreboard = scoreboard
		this.columns = scoreboard.columns
			?? Object.keys(scoreboard.scores).sort()
	}
	setOnlineStatus = online => this.players = online
	setStats(data){
		this.setScoreboard(data.scoreboard)
		this.setOnlineStatus(data.online)
	}
	
	filter(){
		// Remove non-player / empty entries and sort
		this.scoreboard.entries = this.scoreboard.entries
			.filter(Data.isPlayer)
			.filter(this.isNonemptyEntry.bind(this))
			.sort(Intl.Collator().compare)
		
		// Remove empty columns
		this.scoreboard.scores = Data.filter(this.scoreboard.scores, Data.isNonemptyObjective)
	}
	
	sort(by, descending){
		// Pre-create collator for significant performance improvement
		// over `a.localeCompare(b, undefined, {sensitivity: "base"})`
		// funny / weird thing: for localeCompare, supplying an empty `options`
		// object is way slower than supplying nothing...
		const collator = new Intl.Collator(undefined, {sensitivity: "base"})
		
		// When a and b are both numbers, compare as numbers.
		// Otherwise, case-insensitive compare as string
		this.scores = this.scores.sort((a_row, b_row) => {
			const a = a_row[this.columns_[by]]
			const b = b_row[this.columns_[by]]
			if(!isNaN(Number(a)) && !isNaN(Number(b))){
				return (descending ? -1 : 1) * (a - b)
			}else{
				return (descending ? -1 : 1) * collator.compare(a, b)
			}
		})
	}
	
	// Ignore all entries which have no scores (armour stand book fix)
	isNonemptyEntry = entry => Object.entries(this.scoreboard.scores)
		.filter(([_,score]) => score[entry]).length > 0
	
	// Only entries which don't start with '#' and don't contain only digits are marked as players
	static isPlayer = entry => !entry.startsWith("#") && !entry.match(/^\d*$/)
	
	// Whether any entry has a value for this objective
	static isNonemptyObjective = objective =>
		Object.keys(objective).filter(Data.isPlayer).length > 0
	
	// Array-like filter function for objects
	// https://stackoverflow.com/a/37616104
	static filter = (obj, predicate) =>
		Object.fromEntries( Object.entries(obj).filter(([_,v]) => predicate(v)) )
	
}
