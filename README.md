# WebStats
Exposes scoreboard and player online/AFK status to a webpage.

## Requirements
- A Spigot Minecraft server
- A web server

## Usage
**On Minecraft server:** download the [latest release][1] and place in the `plugins/` directory.

**On web server:**
Include the source files:
```html
<script src="/js/Display.js"></script>
<script src="/js/Connection.js"></script>
<script src="/js/Stats.js"></script>
```

Initialise WebStats after page load:
```js
window.onload = () => {
	new WebStats({
		table: document.querySelector("table"), // The <table> element to use (required)
		ip: "203.0.113.42",                     // The IP of the server (required)
		port: 8080,                             // The port set in the config.yml on the server (required)
		sortBy: "Active Play Minutes",          // The initial sorted objective (optional)
		descending: true,                       // The initial sorting direction (optional)
	})
}
```

[1]: https://github.com/Dantevg/WebStats/releases
