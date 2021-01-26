class Data {
	constructor(data){
		this.scoreboard = data.scoreboard
		this.players = data.online
		this.filter()
	}
	
	get objectives(){
		if(this.objectivesmemo == undefined){
			this.objectivesmemo = Object.keys(this.scoreboard.scores).sort()
		}
		return this.objectivesmemo
	}
	get entries(){ return this.scoreboard.entries }
	get scores(){ return this.scoreboard.scores }
	get online(){ return this.players }
	get nOnline(){ return Object.keys(this.players).length }
	
	getScore = (entry, objective) => this.scoreboard.scores[objective][entry]
	
	isOnline = player => this.players[player] === true
	isAFK = player => this.players[player] === "afk"
	isOffline = player => !!this.players[player]
	getStatus = player => this.isOnline(player) ? "online" : (this.isAFK(player) ? "AFK" : "offline")
	
	setScoreboard(scoreboard){
		this.scoreboard = scoreboard
		this.objectivesmemo = undefined // Reset memoised array
	}
	setOnlineStatus = online => this.players = online
	setStats(data){
		this.setScoreboard(data.scoreboard)
		this.setOnlineStatus(data.online)
	}
	
	filter(){
		// Remove non-player entries and sort
		this.scoreboard.entries = this.scoreboard.entries.filter(Data.isPlayer).sort(Intl.Collator().compare)
		
		// Remove empty objectives
		this.scoreboard.scores = Data.filter(this.scoreboard.scores, Data.isNonemptyObjective)
	}
	
	// Only entries which don't start with '#' and don't contain only digits are marked as players
	static isPlayer = entry => !entry.startsWith("#") && !entry.match(/^\d*$/)
	
	// Whether any entry has a value for this objective
	static isNonemptyObjective = objective => Object.keys(objective).filter(Data.isPlayer).length > 0
	
	// Array-like filter function for objects
	// https://stackoverflow.com/a/37616104
	static filter = (obj, predicate) =>
		Object.fromEntries( Object.entries(obj).filter(([_k,v]) => predicate(v)) )
	
}