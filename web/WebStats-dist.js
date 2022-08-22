/* 
	WebStats version 1.7
	https://github.com/Dantevg/WebStats
	
	by RedPolygon
	
	Licence: MIT
*/

class Data {
	constructor(data){
		this.setStats(data)
	}
	
	get entries(){ return this.scoreboard.entries }
	get online(){ return this.players }
	get nOnline(){ return Object.keys(this.players).length }
	
	isOnline = player => this.players[player] === true
	isAFK = player => this.players[player] === "afk"
	isOffline = player => !!this.players[player]
	getStatus = player => this.isOnline(player) ? "online"
		: (this.isAFK(player) ? "AFK" : "offline")
	isCurrentPlayer = player => this.playernames?.includes(player) ?? false
	
	setScoreboard(scoreboard){
		this.scoreboard = scoreboard
		this.columns = scoreboard.columns
			?? Object.keys(scoreboard.scores).sort()
		
		this.filter()
		
		this.scores = []
		for(const entryName of this.entries){
			const entry = []
			entry.push(this.scores.push(entry) - 1)
			entry.push(entryName)
			for(const columnName of this.columns){
				entry.push(this.scoreboard.scores[columnName]?.[entryName] ?? 0)
			}
		}
		
		// Reverse-map column names to indices
		// (index 0 contains the original index, before sorting)
		this.columns_ = {Player: 1}
		this.columns.forEach((val, idx) => this.columns_[val] = idx + 2)
	}
	setOnlineStatus(online){ this.players = online }
	setPlayernames(playernames){ this.playernames = playernames }
	setStats(data){
		this.setScoreboard(data.scoreboard)
		this.setOnlineStatus(data.online)
		this.setPlayernames(data.playernames)
	}
	
	filter(){
		// Remove non-player / empty entries and sort
		this.scoreboard.entries = this.scoreboard.entries
			.filter(Data.isPlayer)
			.filter(this.isNonemptyEntry.bind(this))
			.sort(Intl.Collator().compare)
		
		// Remove empty columns
		this.scoreboard.scores = Data.filter(this.scoreboard.scores, Data.isNonemptyObjective)
	}
	
	sort(by, descending){
		// Pre-create collator for significant performance improvement
		// over `a.localeCompare(b, undefined, {sensitivity: "base"})`
		// funny / weird thing: for localeCompare, supplying an empty `options`
		// object is way slower than supplying nothing...
		const collator = new Intl.Collator(undefined, {sensitivity: "base"})
		
		// When a and b are both numbers, compare as numbers.
		// Otherwise, case-insensitive compare as string
		this.scores = this.scores.sort((a_row, b_row) => {
			const a = a_row[this.columns_[by]]
			const b = b_row[this.columns_[by]]
			if(!isNaN(Number(a)) && !isNaN(Number(b))){
				return (descending ? -1 : 1) * (a - b)
			}else{
				return (descending ? -1 : 1) * collator.compare(a, b)
			}
		})
	}
	
	// Ignore all entries which have no scores (armour stand book fix)
	// (also hides entries with only 0 values)
	isNonemptyEntry = entry => Object.entries(this.scoreboard.scores)
		.filter(([_,score]) => score[entry] && score[entry] != "0").length > 0
	
	// Valid player names only contain between 3 and 16 characters [A-Za-z0-9_],
	// entries with only digits are ignored as well (common for datapacks)
	static isPlayer = entry => entry.match(/^\w{3,16}$/) && !entry.match(/^\d*$/)
	
	// Whether any entry has a value for this objective
	static isNonemptyObjective = objective =>
		Object.keys(objective).filter(Data.isPlayer).length > 0
	
	// Array-like filter function for objects
	// https://stackoverflow.com/a/37616104
	static filter = (obj, predicate) =>
		Object.fromEntries( Object.entries(obj).filter(([_,v]) => predicate(v)) )
	
	// Likewise, array-like map function for objects
	static map = (obj, mapper) =>
		Object.fromEntries( Object.entries(obj).map(([k, v]) => [k, mapper(k, v)]) )
	
}





class Display {
	static COLOUR_CODES = {
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
	}
	
	static FORMATTING_CODES = {
		["§k"]: "obfuscated",
		["§l"]: "bold",
		["§m"]: "strikethrough",
		["§n"]: "underline",
		["§o"]: "italic",
		["§r"]: "reset",
	}
	
	// § followed by a single character, or of the form §x§r§r§g§g§b§b
	// (also capture rest of string, until next §)
	static FORMATTING_CODE_REGEX = /(§x§.§.§.§.§.§.|§.)([^§]*)/gm
	
	constructor({table, sortBy = "Player", descending = false, showSkins = true, displayCount = 100}){
		this.table = table
		this.sortBy = sortBy
		this.descending = descending
		this.showSkins = showSkins
		this.displayCount = displayCount
		this.hideOffline = false
		this.currentPage = 1
	}
	
	init(data){
		this.data = data
		
		// Set pagination controls
		if(this.displayCount > 0){
			this.initPagination()
		}else{
			// Hide pagination controls when pagination is disabled
			const paginationSpanElem = document.querySelector("span.webstats-pagination")
			if(paginationSpanElem) paginationSpanElem.style.display = "none"
		}
		
		// Create header of columns
		this.headerElem = document.createElement("tr")
		this.table.append(this.headerElem)
		Display.appendTh(this.headerElem, "Player", this.thClick.bind(this),
			this.showSkins ? 2 : undefined)
		for(const column of this.data.columns){
			Display.appendTh(this.headerElem, column, this.thClick.bind(this))
		}
		
		// Create rows of (empty) entries
		this.rows = []
		for(const entry of this.data.entries){
			this.appendEntry(entry)
		}
		
		// Fill entries
		this.updateStats()
	}
	
	initPagination(){
		this.maxPage = Math.ceil(this.data.entries.length / this.displayCount)
		
		// Page selector
		this.selectElem = document.querySelector("select.webstats-pagination")
		if(this.selectElem) this.selectElem.onchange = (e) => this.changePage(Number(e.target.value))
		else console.warn("WebStats: no/invalid page control elements")
		
		// "Prev" button
		this.prevButton = document.querySelector("button.webstats-pagination[name=prev]")
		if(this.prevButton) this.prevButton.onclick = () => this.changePage(this.currentPage-1)
		else console.warn("WebStats: no/invalid page control elements")
		
		// "Next" button
		this.nextButton = document.querySelector("button.webstats-pagination[name=next]")
		if(this.nextButton) this.nextButton.onclick = () => this.changePage(this.currentPage+1)
		else console.warn("WebStats: no/invalid page control elements")
		
		this.updatePagination()
	}
	
	updatePagination(){
		const entries = this.hideOffline
			? this.data.entries.filter(entry => this.data.isOnline(entry))
			: this.data.entries
		this.maxPage = Math.ceil(entries.length / this.displayCount)
		
		// Page selector
		if(this.selectElem){
			this.selectElem.innerHTML = ""
			for(let i = 1; i <= this.maxPage; i++){
				const optionElem = document.createElement("option")
				optionElem.innerText = i
				this.selectElem.append(optionElem)
			}
			this.selectElem.value = String(this.currentPage)
		}
		
		// "Prev" button
		if(this.prevButton) this.prevButton.toggleAttribute("disabled", this.currentPage <= 1)
		
		// "Next" button
		if(this.nextButton) this.nextButton.toggleAttribute("disabled", this.currentPage >= this.maxPage)
	}
	
	appendEntry(entry){
		let tr = document.createElement("tr")
		tr.setAttribute("entry", Display.quoteEscape(entry))
		
		// Append skin image
		if(this.showSkins){
			let img = Display.appendElement(tr, "td")
			Display.appendImg(img, "")
			img.classList.add("sticky", "skin")
			img.setAttribute("title", entry)
		}
		
		// Append player name
		let name = Display.appendTextElement(tr, "td", entry)
		name.setAttribute("objective", "Player")
		name.setAttribute("value", entry)
		
		// Prepend online/afk status
		let status = Display.prependElement(name, "div")
		status.classList.add("status")
		
		// Highlight current player
		if(this.data.isCurrentPlayer(entry)) tr.classList.add("current-player")
		
		// Append empty elements for alignment
		for(const objective of this.data.columns){
			let td = Display.appendElement(tr, "td")
			td.classList.add("empty")
			td.setAttribute("objective", Display.quoteEscape(objective))
		}
		this.rows.push(tr)
	}
	
	setSkin(entry, row){
		const img = row.getElementsByTagName("img")[0]
		if(img) img.src = `https://www.mc-heads.net/avatar/${entry}.png`
	}
	
	updateScoreboard(){
		for(const row of this.data.scores){
			for(const column of this.data.columns){
				let value = row[this.data.columns_[column]]
				if(!value) continue
				const td = this.rows[row[0]].querySelector(`td[objective='${column}']`)
				td.classList.remove("empty")
				td.setAttribute("value", value)
				
				// Convert numbers to locale
				value = isNaN(value) ? value : Number(value).toLocaleString()
				
				// Convert Minecraft formatting codes
				td.innerHTML = ""
				td.append(...Display.convertFormattingCodes(value))
			}
		}
	}
	
	updateOnlineStatus(){
		for(const row of this.rows){
			const statusElement = row.querySelector("td .status")
			if(!statusElement) continue
			const entry = row.getAttribute("entry")
			row.classList.remove("online", "afk", "offline")
			statusElement.classList.remove("online", "afk", "offline")
			
			const status = this.data.getStatus(entry)
			row.classList.add(status.toLowerCase())
			statusElement.classList.add(status.toLowerCase())
			statusElement.setAttribute("title", this.data.getStatus(entry))
		}
		// Re-display if pagination is enabled
		if(this.displayCount > 0) this.show()
	}
	
	updateStats(){
		this.updateScoreboard()
		this.updateOnlineStatus()
	}
	
	// Change the page, re-display if `show` is not false and set page controls
	changePage(page, show){
		page = Math.max(1, Math.min(page, this.maxPage))
		this.currentPage = page
		if(show != false) this.show()
		
		if(this.displayCount > 0) this.updatePagination()
	}
	
	changeHideOffline(hideOffline){
		this.hideOffline = hideOffline
		if(this.displayCount > 0){
			this.updatePagination()
			this.changePage(1)
		}
	}
	
	// Re-display table contents
	show(){
		this.table.innerHTML = ""
		this.table.append(this.headerElem)
		const scores = this.hideOffline
			? this.data.scores.filter(row => this.data.isOnline(row[1]))
			: this.data.scores
		const min = (this.currentPage-1) * this.displayCount
		const max = (this.displayCount > 0)
			? Math.min(this.currentPage * this.displayCount, scores.length)
			: scores.length
		for(let i = min; i < max; i++){
			if(this.showSkins) this.setSkin(scores[i][1], this.rows[scores[i][0]])
			this.table.append(this.rows[scores[i][0]])
		}
	}
	
	// Sort a HTML table element
	sort(by = this.sortBy, descending = this.descending){
		this.data.sort(by, descending)
		this.show()
	}
	
	// When a table header is clicked, sort by that header
	thClick(e){
		let objective = e.target.innerText
		this.descending = (objective === this.sortBy) ? !this.descending : true
		this.sortBy = objective
		if(this.displayCount > 0) this.changePage(1, false)
		this.sort()
		
		// Set URL query string, for sharing
		window.history.replaceState({}, "",
			location.pathname + "?sort=" + this.sortBy.replace(/\s/g, "+")
			+ "&order=" + (this.descending ? "desc" : "asc"))
	}
	
	// Replace single quotes by '&quot;' (html-escape)
	static quoteEscape = string => string.replace(/'/g, "&quot;")
	
	// Replace all formatting codes by <span> elements
	static convertFormattingCodes = (value) =>
		Display.parseFormattingCodes(value).map(Display.convertFormattingCode)
	
	// Convert a single formatting code to a <span> element
	static convertFormattingCode(part){
		if(!part.format && !part.colour) return part.text
		
		const span = document.createElement("span")
		span.innerText = part.text
		span.classList.add("mc-format")
		
		if(part.format) span.classList.add("mc-" + part.format)
		if(part.colour){
			if(part.colourType == "simple") span.classList.add("mc-" + part.colour)
			if(part.colourType == "hex") span.style.color = part.colour
		}
		
		return span
	}
	
	static parseFormattingCodes(value){
		const parts = []
		
		const firstIdx = value.matchAll(Display.FORMATTING_CODE_REGEX).next().value?.index
		if(firstIdx == undefined || firstIdx > 0){
			parts.push({text: value.substring(0, firstIdx)})
		}
		
		for(const match of value.matchAll(Display.FORMATTING_CODE_REGEX)){
			parts.push(Display.parseFormattingCode(match[1], match[2], parts[parts.length-1]))
		}
		
		return parts
	}
	
	static parseFormattingCode(code, text, prev){
		// Simple colour codes and formatting codes
		if(Display.COLOUR_CODES[code]){
			return {
				text,
				colour: Display.COLOUR_CODES[code],
				colourType: "simple",
			}
		}
		if(Display.FORMATTING_CODES[code]){
			return {
				text,
				format: Display.FORMATTING_CODES[code],
				colour: prev?.colour,
				colourType: prev?.colourType,
			}
		}
		
		// Hex colour codes
		const matches = code.match(/§x§(.)§(.)§(.)§(.)§(.)§(.)/m)
		if(matches){
			return {
				text,
				colour: "#" + matches.slice(1).join(""),
				colourType: "hex",
			}
		}
		
		// Not a valid formatting code, just return the input unaltered
		return {text}
	}
	
	static appendElement(base, type){
		let el = document.createElement(type)
		base.append(el)
		return el
	}
	
	static prependElement(base, type){
		let el = document.createElement(type)
		base.prepend(el)
		return el
	}
	
	static appendTextElement(base, type, name){
		let el = Display.appendElement(base, type)
		el.innerText = name
		return el
	}
	
	static appendTh(base, name, onclick, colspan){
		let th = Display.appendTextElement(base, "th", name)
		th.onclick = onclick
		if(colspan != undefined) th.setAttribute("colspan", colspan)
		return th
	}
	
	static appendImg(base, src){
		let img = Display.appendElement(base, "img")
		img.src = src
		return img
	}
	
}





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
		
		window.webstats = this
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
	
	startUpdateInterval(first){
		this.interval = setInterval(this.update.bind(this), this.updateInterval)
		if(!first) this.update()
	}
	
	stopUpdateInterval(){
		clearInterval(this.interval)
	}
	
}
