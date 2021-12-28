/* 
	WebStats version 1.6
	https://github.com/Dantevg/WebStats
	
	by RedPolygon
	
	Licence: MIT
*/

class WebStats {
	constructor(config){
		this.display = new Display(config)
		this.connection = config.connection ?? Connection.json(config.ip, config.port)
		this.updateInterval = config.updateInterval ?? 10000
		
		// Set online status update interval
		if(this.updateInterval > 0) this.startUpdateInterval(true)
		document.addEventListener("visibilitychange", () => document.hidden
			? this.stopUpdateInterval() : this.startUpdateInterval())
		
		// Get data and init
		this.connection.getStats().then(data => this.init(data))
		
		// Get saved toggles from cookies
		const cookies = document.cookie.split("; ") ?? []
		cookies.filter(str => str.length > 0).forEach(cookie => {
			const [property, value] = cookie.match(/[^=]+/g)
			document.documentElement.classList.toggle(property, value == "true")
			const el = document.querySelector("input.webstats-option#" + property)
			if(el) el.checked = (value == "true")
		})
		
		// On config option toggle, set the html element's class and store cookie
		document.querySelectorAll("input.webstats-option").forEach(el =>
			el.addEventListener("change", () => {
				document.documentElement.classList.toggle(el.id, el.checked)
				// Set a cookie which expires in 10 years
				document.cookie = `${el.id}=${el.checked}; max-age=${60*60*24*365*10}; SameSite=Lax`
			})
		)
		
		const optionHideOffline = document.querySelector("input.webstats-option#hide-offline")
		if(optionHideOffline){
			// Re-show if displayCount is set
			optionHideOffline.addEventListener("change", (e) => {
				if(this.display.displayCount > 0) this.display.changeHideOffline(e.target.checked)
			})
			this.display.hideOffline = optionHideOffline.checked
		}
	}
	
	init(data){
		this.data = new Data(data)
		this.display.init(this.data) // Display data in table
		
		// Get sorting from url params, if present
		const params = (new URL(document.location)).searchParams
		let sortBy = params.get("sort") ?? this.display.sortBy
		let order = params.get("order")
		let descending = order ? order.startsWith("d") : this.display.descending
		this.display.sort(sortBy, descending)
	}
	
	update(){
		// When nobody is online, assume scoreboard does not change
		if(this.data.nOnline > 0){
			this.connection.getStats().then(this.display.updateStats.bind(this.display))
		}else{
			this.connection.getOnline().then(this.display.updateOnlineStatus.bind(this.display))
		}
	}
	
	startUpdateInterval(first){
		this.interval = setInterval(this.update.bind(this), this.updateInterval)
		if(!first) this.update()
	}
	
	stopUpdateInterval(){
		clearInterval(this.interval)
	}
	
}
