class Display {
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
		
		// Create header of columns
		this.headerElem = document.createElement("tr")
		const playerHeader = this.appendTh(this.headerElem, "Player")
		if(this.showSkins) playerHeader.setAttribute("colspan", 2)
		this.table.append(this.headerElem)
		for(const column of this.data.columns){
			this.appendTh(this.headerElem, column)
		}
		
		this.rows = []
		
		// Create rows of (empty) entries
		for(const entry of this.data.entries){
			let tr = document.createElement("tr")
			tr.setAttribute("entry", Display.quoteEscape(entry))
			
			// Append skin image
			if(this.showSkins){
				let img = this.appendElement(tr, "td")
				this.appendImg(img, "https://www.mc-heads.net/avatar/" + entry + ".png")
					.setAttribute("alt", entry)
				img.classList.add("sticky", "skin")
				img.setAttribute("title", entry)
			}
			
			// Append player name
			let name = this.appendTextElement(tr, "td", entry)
			name.setAttribute("objective", "Player")
			name.setAttribute("value", entry)
			
			// Prepend online/afk status
			let status = this.prependElement(name, "div")
			status.classList.add("status")
			
			// Append empty elements for alignment
			for(const objective of this.data.columns){
				let td = this.appendElement(tr, "td")
				td.classList.add("empty")
				td.setAttribute("objective", Display.quoteEscape(objective))
			}
			this.rows.push(tr)
		}
		
		// Fill entries
		this.updateStats(data)
	}
	
	updateScoreboard(scoreboard){
		this.data.setScoreboard(scoreboard)
		for(const row of this.data.scores){
			for(const column of this.data.columns){
				const value = row[this.data.columns_[column]]
				if(!value) continue
				const td = this.rows[row[0]].querySelector(`td[objective=${column}]`)
				td.classList.remove("empty")
				td.setAttribute("value", value)
				td.innerText = isNaN(value) ? value : Number(value).toLocaleString()
			}
		}
	}
	
	updateOnlineStatus(online){
		this.data.setOnlineStatus(online)
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
		if(this.displayCount > 0) this.show()
	}
	
	updateStats(data){
		this.updateScoreboard(data.scoreboard)
		this.updateOnlineStatus(data.online)
	}
	
	appendElement(base, type){
		let el = document.createElement(type)
		base.append(el)
		return el
	}
	
	prependElement(base, type){
		let el = document.createElement(type)
		base.prepend(el)
		return el
	}
	
	appendTextElement(base, type, name){
		let el = this.appendElement(base, type)
		el.innerText = name
		return el
	}
	
	appendTh(base, name){
		let th = this.appendTextElement(base, "th", name)
		th.onclick = this.thClick.bind(this)
		return th
	}
	
	appendImg(base, src){
		let img = this.appendElement(base, "img")
		img.src = src
		return img
	}
	
	changePage(page){
		this.currentPage = page
		this.show()
	}
	
	// Re-display table contents
	show(){
		this.table.innerHTML = ""
		this.table.append(this.headerElem)
		const min = (this.currentPage-1) * this.displayCount
		const max = (this.displayCount < 0)
			? this.rows.length
			: this.currentPage * this.displayCount
		const scores = this.hideOffline
			? this.data.scores.filter(row => this.data.isOnline(row[1]))
			: this.data.scores
		for(let i = min; i < max; i++){
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
		this.currentPage = 1
		this.sort()
		
		// Set URL query string, for sharing
		window.history.replaceState({}, "",
			location.pathname + "?sort=" + this.sortBy.replace(/\s/g, "+")
			+ "&order=" + (this.descending ? "desc" : "asc"))
	}
	
	// Replace single quotes by '&quot;' (html-escape)
	static quoteEscape = string => string.replace(/'/g, "&quot;")
	
}
