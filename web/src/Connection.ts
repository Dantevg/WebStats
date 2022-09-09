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

	static json(host: string) {
		return new Connection({
			all:  `http://${host}/stats.json`,
			scores:  `http://${host}/scoreboard.json`,
			online:  `http://${host}/online.json`,
			tables:  `http://${host}/tables.json`,
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
