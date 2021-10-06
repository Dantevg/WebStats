# WebStats
WebStats is a plugin that can gather player statistics from multiple sources
and display it on a webpage. It can get data from the **scoreboard**, from any
plugin that stores its data in a **database**, from **PlaceholderAPI** and
player **online/AFK** status.

## Requirements
- A Spigot Minecraft server
- A web server (note: the plugin will not work over https, so make sure the
  webpage isn't served over https either)

## Basic usage
**On Minecraft server:** download the [latest release][1] and place in the
`plugins/` directory.

**On web server:**
Include the source files:
```html
<script src="Data.js"></script>
<script src="Display.js"></script>
<script src="Connection.js"></script>
<script src="Stats.js"></script>
```

Initialise WebStats after page load: (put this in a `<script>` tag in `<head>`)
```js
// You can set the update interval (in ms) *before* initialising WebStats (optional, default 10000)
WebStats.updateInterval = 0 // Set to 0 to disable auto-updating

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

## MySQL database functionality
Since version 1.4, the plugin also supports getting data from a MySQL database.
Because plugins can use wildly different formats to store their data, WebStat's
configuration format is also very flexible.
For every table in the database to use, you can specify a list of conversion
functions, which will be executed one after the other. Currently, the
conversion functions available (with the arguments they take) are:
- `filter` (`column`, ...): only keeps columns which appear in the list of arguments.
- `remove` (`column`, ...): removes all columns which appear in the list of arguments.
- `rename` (`from-column`, `to-column`): renames the `from-column` column to `to-column`.
- `json` (`column`): extracts the JSON from column `column` and creates columns
  for all keys in the JSON object (the JSON needs to be an object, not an array)
- `key-value` (`key-column`, `value-column`): takes a key-value pair stored in
  two separate columns, makes a column for the key (with name `key-column`) and
  stores the value from the `value-column` in that column.
- `uuid` (`column`): converts the UUIDs in `column` to player names and renames
  the column to `player`.

## Plugin config file
- `port`: the port number to use. Make sure the plugin can use the port,
  you may need to open the port in your server's control panel first.
- `columns`: a list of columns, to specify a custom column order. Columns not
  present in this list will be hidden (you don't need to specify the 'player'
  column; it is always present, and always as the first column). By default,
  all columns are displayed in alphabetical order.
- `objectives`: the list of scoreboard objectives to send to the webpage.
  `*` means all objectives.
- `database`: the configuration for the MySQL database connectivity:
  - `hostname`: if you use the database on the same server as your Minecraft
    server, this will be `localhost`. Otherwise, this is the IP or URL to the
    database server.
  - `username` and `password`: you know what to do.
  - `config`: a list of database-table configurations. Each item contains:
    - `database`: the name of the database to use.
    - `table`: the name of the table within the database to use.
    - `convert`: a list of conversion commands. Each item (each command) is
      in itself also a list of the command followed by its arguments. See the
      segment on database functionality for command-specific information. The
      `config.yml` contains an example usage which may be helpful.
- `placeholders`: the configuration for the PlaceholderAPI connectivity. The
  key of every entry here specifies the placeholder to use, the value sets the
  displayed name.
- `store-placeholders-database`: the name of the database to use for storing
  offline players' placeholders (many placeholderAPI sources don't have data
  for offline players). When you use this, you need to set the `hostname`,
  `username` and `password` fields under the `database` config. (see above,
  you can leave the `config` option commented out)

[1]: https://github.com/Dantevg/WebStats/releases
