# WebStats
Exposes scoreboard and player online/AFK status to a webpage.

## Requirements
- A Spigot Minecraft server
- A web server (note: the plugin will not work over https, so make sure the webpage isn't served over https either)

## Usage
**On Minecraft server:** download the [latest release][1] and place in the `plugins/` directory.

**On web server:**
Include the source files:
```html
<script src="Display.js"></script>
<script src="Connection.js"></script>
<script src="Stats.js"></script>
```

Initialise WebStats after page load:
```js
// You can set the update interval (in ms) *before* initialising WebStats (optional, default 10000)
WebStats.updateInterval = 0 // Set to 0 to disable auto-updating online player list

window.onload = () => {
	new WebStats({
		table: document.querySelector("table"), // The <table> element to use (required)
		ip: "203.0.113.42",                     // The IP of the server (required)
		port: 8080,                             // The port set in the config.yml on the server (required)
		sortBy: "Active Play Minutes",          // The initial sorted objective (optional, default "Player")
		descending: true,                       // The initial sorting direction (optional, default false)
	})
}
```

[1]: https://github.com/Dantevg/WebStats/releases
