export default class Connection {
	all: string
	scores: string
	online: string
	tables: string

	constructor({ all, scores, online, tables }) {
		this.all = all
		this.scores = scores
		this.online = online
		this.tables = tables
	}

	static json(ip: string, port: string | number) {
		const baseURL = `http://${ip}:${port}`
		return new Connection({
			all: baseURL + "/stats.json",
			scores: baseURL + "/scoreboard.json",
			online: baseURL + "/online.json",
			tables: baseURL + "/tables.json",
		})
	}

	getStats = async () => {
		if (this.all) {
			return await (await fetch(this.all)).json()
		} else {
			const [online, scoreboard] = await Promise.all([this.getOnline(), this.getScoreboard()])
			return { online, scoreboard }
		}
	}
	getScoreboard = () => fetch(this.scores).then(response => response.json())
	getOnline = () => fetch(this.online).then(response => response.json())
	getTables = () => fetch(this.tables).then(response => response.json())
}
