export default class RandomStats {
	static N_ENTRIES = 10
	static N_STATS   = 5
	
	constructor(n_entries = RandomStats.N_ENTRIES, n_stats = RandomStats.N_STATS){
		this.scores = { entries: [], scores: {} }
		
		this.scores.entries.push("#server")
		for(let i = 1; i < n_entries; i++){
			this.scores.entries.push(RandomStats.intToName(i, 3))
		}
		
		for(let i = 0; i < n_stats; i++){
			const stat = {}
			this.scores.scores[RandomStats.intToName(i, 1)] = stat
			for(const entry of this.scores.entries){
				stat[entry] = Math.floor(Math.random()*1000)
			}
		}
	}
	
	static intToName(n, length){
		const ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
		let name = ""
		for(let i = 0; i < length; i++){
			name = ALPHABET.charAt((n / (26**i)) % 26) + name
		}
		return name
	}
	
	getStats      = async () => ({
		online: await this.getOnline(),
		scoreboard: await this.getScoreboard(),
		playernames: [this.scores.entries[1]]
	})
	getScoreboard = async () => this.scores
	getOnline     = async () => {
		const online = {}
		for(let i = 0; i < this.scores.entries.length / 10; i++){
			const rand = Math.floor(Math.random()*this.scores.entries.length)
			online[RandomStats.intToName(rand, 3)] = true
		}
		return online
	}
	getTables     = async () => [{
		sortColumn: "Player",
		sortDirection: "ascending",
	}]
}
