export default class Connection {
	all: string
	scores: string
	online: string
	tables: string
	serverIcon: string

	constructor({ all, scores, online, tables, serverIcon }) {
		this.all = all
		this.scores = scores
		this.online = online
		this.tables = tables
		this.serverIcon = serverIcon
	}

	static json(host: string) {
		// Detect if the user has manually entered a protocol (could be https)
		const protocol = host.startsWith("http") ? "" : "http://"
		return new Connection({
			all:    `${protocol}${host}/stats.json`,
			scores: `${protocol}${host}/scoreboard.json`,
			online: `${protocol}${host}/online.json`,
			tables: `${protocol}${host}/tables.json`,
			serverIcon: `${protocol}${host}/server-icon.png`,
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
	getScoreboard = () => fetch(this.scores).then(response => response.json()).catch(() => {})
	getOnline = () => fetch(this.online).then(response => response.json()).catch(() => {})
	getTables = () => fetch(this.tables).then(response => response.json()).catch(() => {})
}
