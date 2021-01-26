/* 
	WebStats version 1.2
	https://github.com/Dantevg/WebStats
	
	by RedPolygon
*/

class WebStats {
	static updateInterval = 10000
	
	constructor({table, ip, port, sortBy, descending}){
		this.display = new Display(table, sortBy, descending)
		this.connection = new Connection(ip, port)
		
		// Set online status update interval
		if(WebStats.updateInterval > 0) this.startUpdateInterval(true)
		document.addEventListener("visibilitychange", () => document.hidden
			? this.stopUpdateInterval() : this.startUpdateInterval())
		
		// Get data and init
		this.connection.getStats().then(data => this.init(data))
	}
	
	init(data){
		// Remove non-player entries and sort
		data.scoreboard.entries = data.scoreboard.entries.filter(WebStats.isPlayer).sort(Intl.Collator().compare)
		
		// Remove empty objectives
		data.scoreboard.scores = WebStats.filter(data.scoreboard.scores, WebStats.isNonemptyObjective)
		
		// Create a sorted list of all objectives
		data.scoreboard.objectives = Object.keys(data.scoreboard.scores).sort()
		
		// Display data in table
		this.display.init(data)
		
		// Get sorting from url params, if present
		const params = (new URL(document.location)).searchParams
		let sortBy = params.get("sort") ?? this.display.sortBy
		let order = params.get("order")
		let descending = order ? order.startsWith("d") : this.display.descending
		this.display.sort(sortBy, descending)
	}
	
	update(){
		this.connection.getStats().then(this.display.updateStats.bind(this.display))
	}
	
	startUpdateInterval(first){
		this.interval = setInterval(this.update.bind(this), WebStats.updateInterval)
		if(!first) this.update()
	}
	
	stopUpdateInterval(){
		clearInterval(this.interval)
	}
	
	// Only entries which don't start with '#' and don't contain only digits are marked as players
	static isPlayer = entry => !entry.startsWith("#") && !entry.match(/^\d*$/)
	
	// Whether any entry has a value for this objective
	static isNonemptyObjective = objective => Object.keys(objective).filter(WebStats.isPlayer).length > 0
	
	// Array-like filter function for objects
	// https://stackoverflow.com/a/37616104
	static filter = (obj, predicate) =>
		Object.fromEntries( Object.entries(obj).filter(([_k,v]) => predicate(v)) )
	
}