import Connection from "./Connection"
import Data from "./Data"
import Display from "./Display"

declare global {
	interface Window { webstats: WebStats }
}

export default class WebStats {
	display: Display
	connection: Connection
	data: Data
	updateInterval: number
	interval: number
	
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
			const el = document.querySelector("input.webstats-option#" + property) as HTMLInputElement
			if(el) el.checked = (value == "true")
		})
		
		// On config option toggle, set the html element's class and store cookie
		document.querySelectorAll("input.webstats-option").forEach(el =>
			el.addEventListener("change", () => {
				document.documentElement.classList.toggle(el.id, (el as HTMLInputElement).checked)
				// Set a cookie which expires in 10 years
				document.cookie = `${el.id}=${(el as HTMLInputElement).checked}; max-age=${60*60*24*365*10}; SameSite=Lax`
			})
		)
		
		const optionHideOffline = document.querySelector("input.webstats-option#hide-offline") as HTMLInputElement
		if(optionHideOffline){
			// Re-show if displayCount is set
			optionHideOffline.addEventListener("change", (e) => {
				if(this.display.displayCount > 0) this.display.changeHideOffline((e.target as HTMLInputElement).checked)
			})
			this.display.hideOffline = optionHideOffline.checked
		}
		
		window.webstats = this
	}
	
	init(data: Data){
		this.data = new Data(data)
		this.display.init(this.data) // Display data in table
		
		// Get sorting from url params, if present
		const params = (new URL(document.location.href)).searchParams
		let sortBy = params.get("sort") ?? this.display.sortBy
		let order = params.get("order")
		let descending = order ? order.startsWith("d") : this.display.descending
		this.display.sort(sortBy, descending)
	}
	
	update(){
		// When nobody is online, assume scoreboard does not change
		if(this.data.nOnline > 0){
			this.connection.getStats().then(data => {
				this.data.setStats(data)
				this.display.updateStats()
			})
		}else{
			this.connection.getOnline().then(data => {
				this.data.setOnlineStatus(data)
				this.display.updateOnlineStatus()
			})
		}
	}
	
	startUpdateInterval(first?: boolean){
		this.interval = setInterval(this.update.bind(this), this.updateInterval)
		if(!first) this.update()
	}
	
	stopUpdateInterval(){
		clearInterval(this.interval)
	}
	
}
