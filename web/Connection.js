class Connection {
	constructor(ip, port){
		this.ip = ip
		this.port = port
		
		this.baseURL       = `http://${ip}:${port}`
		this.statsURL      = this.baseURL + "/stats.json"
		this.scoreboardURL = this.baseURL + "/scoreboard.json"
		this.onlineURL     = this.baseURL + "/online.json"
	}
	
	getStats      = () => fetch(this.statsURL).then(response => response.json())
	getScoreboard = () => fetch(this.scoreboardURL).then(response => response.json())
	getOnline     = () => fetch(this.onlineURL).then(response => response.json())
}