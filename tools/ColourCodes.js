const FORMATTING_CODES = {
	["§0"]: "black",
	["§1"]: "dark_blue",
	["§2"]: "dark_green",
	["§3"]: "dark_aqua",
	["§4"]: "dark_red",
	["§5"]: "dark_purple",
	["§6"]: "gold",
	["§7"]: "gray",
	["§8"]: "dark_gray",
	["§9"]: "blue",
	["§a"]: "green",
	["§b"]: "aqua",
	["§c"]: "red",
	["§d"]: "light_purple",
	["§e"]: "yellow",
	["§f"]: "white",
	
	["§k"]: "obfuscated",
	["§l"]: "bold",
	["§m"]: "strikethrough",
	["§n"]: "underline",
	["§o"]: "italic",
	["§r"]: "reset",
}

class ColourCodes {
	constructor(){
		this.scores = { entries: [], scores: {} }
		
		for(const code in FORMATTING_CODES){
			this.scores.entries.push(FORMATTING_CODES[code])
		}
		this.scores.entries.push("hex")
		this.scores.entries.push("bold_red")
		
		const stat = {}
		this.scores.scores.Colour = stat
		for(const code in FORMATTING_CODES){
			const name = FORMATTING_CODES[code]
			stat[name] = "before" + code + "after"
		}
		stat.hex = "before§x§f§f§4§4§4§4after"
		stat["bold_red"] = "before§c§lafter"
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
			online[this.scores.entries[rand]] = true
		}
		return online
	}
}
