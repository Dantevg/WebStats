class Connection {
	constructor({all, scores, online}){
		this.all    = all
		this.scores = scores
		this.online = online
	}
	
	static json(ip, port){
		const baseURL = `http://${ip}:${port}`
		return new Connection({
			all: baseURL + "/stats.json",
			scores: baseURL + "/scoreboard.json",
			online: baseURL + "/online.json"
		})
	}
	
	getStats      = async () => {
		if(this.all){
			return await (await fetch(this.all)).json()
		}else{
			const [online, scoreboard] = await Promise.all([this.getOnline(), this.getScoreboard()])
			return {online, scoreboard}
		}
	}
	getScoreboard = () => fetch(this.scores).then(response => response.json())
	getOnline     = () => fetch(this.online).then(response => response.json())
}
