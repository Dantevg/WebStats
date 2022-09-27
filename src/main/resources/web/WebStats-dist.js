/*
	WebStats version 1.7
	https://github.com/Dantevg/WebStats
	
	by RedPolygon
	
	Licence: MIT
*/

class Connection {
    constructor({ all, scores, online, tables }) {
        this.getStats = async () => {
            if (this.all) {
                return await (await fetch(this.all)).json();
            }
            else {
                const [online, scoreboard] = await Promise.all([this.getOnline(), this.getScoreboard()]);
                return { online, scoreboard };
            }
        };
        this.getScoreboard = () => fetch(this.scores).then(response => response.json()).catch(() => { });
        this.getOnline = () => fetch(this.online).then(response => response.json()).catch(() => { });
        this.getTables = () => fetch(this.tables).then(response => response.json()).catch(() => { });
        this.all = all;
        this.scores = scores;
        this.online = online;
        this.tables = tables;
    }
    static json(host) {
        return new Connection({
            all: `http://${host}/stats.json`,
            scores: `http://${host}/scoreboard.json`,
            online: `http://${host}/online.json`,
            tables: `http://${host}/tables.json`,
        });
    }
}

class Data {
    constructor(data) {
        this.isOnline = (player) => this.players[player] === true;
        this.isAFK = (player) => this.players[player] === "afk";
        this.isOffline = (player) => !!this.players[player];
        this.getStatus = (player) => this.isOnline(player) ? "online"
            : (this.isAFK(player) ? "AFK" : "offline");
        this.isCurrentPlayer = (player) => this.playernames?.includes(player) ?? false;
        // Ignore all entries which have no scores (armour stand book fix)
        // (also hides entries with only 0 values)
        this.isNonemptyEntry = (entry) => Object.entries(this.scoreboard.scores)
            .filter(([_, score]) => score[entry] && score[entry] != "0").length > 0;
        this.setStats(data);
    }
    get entries() { return this.scoreboard.entries; }
    get online() { return this.players; }
    get nOnline() { return Object.keys(this.players).length; }
    setScoreboard(scoreboard) {
        this.scoreboard = scoreboard;
        this.columns = Object.keys(scoreboard.scores).sort();
        this.filter();
        this.scores = [];
        for (const entryName of this.entries) {
            const entry = [];
            entry.push(this.scores.push(entry) - 1);
            entry.push(entryName);
            for (const columnName of this.columns) {
                entry.push(this.scoreboard.scores[columnName]?.[entryName] ?? 0);
            }
        }
        // Reverse-map column names to indices
        // (index 0 contains the original index, before sorting)
        this.columns_ = { Player: 1 };
        this.columns.forEach((val, idx) => this.columns_[val] = idx + 2);
    }
    setOnlineStatus(online) { this.players = online; }
    setPlayernames(playernames) { this.playernames = playernames; }
    setStats(data) {
        this.setScoreboard(data.scoreboard);
        this.setOnlineStatus(data.online);
        this.setPlayernames(data.playernames);
    }
    filter() {
        // Remove non-player / empty entries and sort
        this.scoreboard.entries = this.scoreboard.entries
            .filter(Data.isPlayerOrServer)
            .filter(this.isNonemptyEntry.bind(this))
            .sort(Intl.Collator().compare);
        // Remove empty columns
        this.scoreboard.scores = Data.filter(this.scoreboard.scores, Data.isNonemptyObjective);
    }
    sort(by, descending) {
        // Pre-create collator for significant performance improvement
        // over `a.localeCompare(b, undefined, {sensitivity: "base"})`
        // funny / weird thing: for localeCompare, supplying an empty `options`
        // object is way slower than supplying nothing...
        const collator = new Intl.Collator(undefined, { sensitivity: "base" });
        // When a and b are both numbers, compare as numbers.
        // Otherwise, case-insensitive compare as string
        this.scores = this.scores.sort((a_row, b_row) => {
            const a = a_row[this.columns_[by]];
            const b = b_row[this.columns_[by]];
            if (!isNaN(Number(a)) && !isNaN(Number(b))) {
                return (descending ? -1 : 1) * (a - b);
            }
            else {
                return (descending ? -1 : 1) * collator.compare(a, b);
            }
        });
    }
}
// Valid player names only contain between 3 and 16 characters [A-Za-z0-9_],
// entries with only digits are ignored as well (common for datapacks)
Data.isPlayerOrServer = (entry) => entry == "#server" || (entry.match(/^\w{3,16}$/) && !entry.match(/^\d*$/));
// Whether any entry has a value for this objective
Data.isNonemptyObjective = (objective) => Object.keys(objective).filter(Data.isPlayerOrServer).length > 0;
// Array-like filter function for objects
// https://stackoverflow.com/a/37616104
Data.filter = (obj, predicate) => Object.fromEntries(Object.entries(obj).filter(([_, v]) => predicate(v)));
// Likewise, array-like map function for objects
Data.map = (obj, mapper) => Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, mapper(k, v)]));

class FormattingCodes {
    // Convert a single formatting code to a <span> element
    static convertFormattingCode(part) {
        if (!part.format && !part.colour)
            return part.text;
        const span = document.createElement("span");
        span.innerText = part.text;
        span.classList.add("mc-format");
        if (part.format)
            span.classList.add("mc-" + part.format);
        if (part.colour) {
            if (part.colourType == "simple")
                span.classList.add("mc-" + part.colour);
            if (part.colourType == "hex")
                span.style.color = part.colour;
        }
        return span;
    }
    static parseFormattingCodes(value) {
        const parts = [];
        const firstIdx = value.matchAll(FormattingCodes.FORMATTING_CODE_REGEX).next().value?.index;
        if (firstIdx == undefined || firstIdx > 0) {
            parts.push({ text: value.substring(0, firstIdx) });
        }
        for (const match of value.matchAll(FormattingCodes.FORMATTING_CODE_REGEX)) {
            parts.push(FormattingCodes.parseFormattingCode(match[1], match[2], parts[parts.length - 1]));
        }
        return parts;
    }
    static parseFormattingCode(code, text, prev) {
        // Simple colour codes and formatting codes
        if (FormattingCodes.COLOUR_CODES[code]) {
            return {
                text,
                colour: FormattingCodes.COLOUR_CODES[code],
                colourType: "simple",
            };
        }
        if (FormattingCodes.FORMATTING_CODES[code]) {
            return {
                text,
                format: FormattingCodes.FORMATTING_CODES[code],
                colour: prev?.colour,
                colourType: prev?.colourType,
            };
        }
        // Hex colour codes
        const matches = code.match(/§x§(.)§(.)§(.)§(.)§(.)§(.)/m);
        if (matches) {
            return {
                text,
                colour: "#" + matches.slice(1).join(""),
                colourType: "hex",
            };
        }
        // Not a valid formatting code, just return the input unaltered
        return { text };
    }
}
FormattingCodes.COLOUR_CODES = {
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
};
FormattingCodes.FORMATTING_CODES = {
    ["§k"]: "obfuscated",
    ["§l"]: "bold",
    ["§m"]: "strikethrough",
    ["§n"]: "underline",
    ["§o"]: "italic",
    ["§r"]: "reset",
};
// § followed by a single character, or of the form §x§r§r§g§g§b§b
// (also capture rest of string, until next §)
FormattingCodes.FORMATTING_CODE_REGEX = /(§x§.§.§.§.§.§.|§.)([^§]*)/gm;
// Replace all formatting codes by <span> elements
FormattingCodes.convertFormattingCodes = (value) => FormattingCodes.parseFormattingCodes(value).map(FormattingCodes.convertFormattingCode);

class Display {
    constructor({ table, pagination, showSkins = true }, { columns, sortBy = "Player", sortDescending = false }) {
        this.table = table;
        this.pagination = pagination;
        this.columns = columns;
        this.sortBy = sortBy;
        this.descending = sortDescending;
        this.showSkins = showSkins;
        this.hideOffline = false;
        if (this.pagination)
            this.pagination.onPageChange = (page) => {
                this.updatePagination();
                this.show();
            };
    }
    init(data) {
        this.data = data;
        // Set pagination controls
        if (this.pagination)
            this.updatePagination();
        // Create header of columns
        this.headerElem = document.createElement("tr");
        this.table.append(this.headerElem);
        Display.appendTh(this.headerElem, "Player", this.thClick.bind(this), this.showSkins ? 2 : undefined);
        for (const column of this.columns ?? this.data.columns) {
            Display.appendTh(this.headerElem, column, this.thClick.bind(this));
        }
        // Create rows of (empty) entries
        this.rows = new Map();
        for (const entry of this.getEntries()) {
            this.appendEntry(entry);
        }
        // Fill entries
        this.updateStatsAndShow();
    }
    getEntries() {
        const entriesHere = this.data.entries.filter((entry) => (this.columns ?? this.data.columns).some((column) => this.data.scoreboard.scores[column][entry]
            && this.data.scoreboard.scores[column][entry] != "0"));
        return this.hideOffline
            ? entriesHere.filter(entry => this.data.isOnline(entry))
            : entriesHere;
    }
    getScores() {
        const scoresHere = this.data.scores.filter(row => this.rows.has(row[1]));
        return this.hideOffline
            ? scoresHere.filter(row => this.data.isOnline(row[1]))
            : scoresHere;
    }
    updatePagination() {
        this.pagination.update(this.getEntries().length);
    }
    appendEntry(entry) {
        let tr = document.createElement("tr");
        tr.setAttribute("entry", Display.quoteEscape(entry));
        // Append skin image
        if (this.showSkins) {
            let img = Display.appendElement(tr, "td");
            Display.appendImg(img, "");
            img.classList.add("sticky", "skin");
            img.setAttribute("title", entry);
        }
        // Append player name
        let name = Display.appendTextElement(tr, "td", entry == "#server" ? "Server" : entry);
        name.setAttribute("objective", "Player");
        name.setAttribute("value", entry);
        // Prepend online/afk status
        let status = Display.prependElement(name, "div");
        status.classList.add("status");
        // Highlight current player
        if (this.data.isCurrentPlayer(entry))
            tr.classList.add("current-player");
        // Append empty elements for alignment
        for (const column of this.columns ?? this.data.columns) {
            let td = Display.appendElement(tr, "td");
            td.classList.add("empty");
            td.setAttribute("objective", Display.quoteEscape(column));
        }
        this.rows.set(entry, tr);
    }
    setSkin(entry, row) {
        const img = row.getElementsByTagName("img")[0];
        if (img) {
            if (entry == "#server")
                img.src = Display.CONSOLE_IMAGE;
            else
                img.src = `https://www.mc-heads.net/avatar/${entry}.png`;
        }
    }
    updateScoreboard() {
        for (const row of this.data.scores) {
            for (const column of this.columns ?? this.data.columns) {
                let value = row[this.data.columns_[column]];
                if (!value)
                    continue;
                const td = this.rows.get(row[1]).querySelector(`td[objective='${column}']`);
                td.classList.remove("empty");
                td.setAttribute("value", value);
                // Convert numbers to locale
                value = isNaN(value) ? value : Number(value).toLocaleString();
                // Convert Minecraft formatting codes
                td.innerHTML = "";
                td.append(...FormattingCodes.convertFormattingCodes(value));
            }
        }
    }
    updateScoreboardAndShow() {
        this.updateScoreboard();
        this.show();
    }
    updateOnlineStatus() {
        for (const [_, row] of this.rows) {
            const statusElement = row.querySelector("td .status");
            if (!statusElement)
                continue;
            const entry = row.getAttribute("entry");
            row.classList.remove("online", "afk", "offline");
            statusElement.classList.remove("online", "afk", "offline");
            const status = this.data.getStatus(entry);
            row.classList.add(status.toLowerCase());
            statusElement.classList.add(status.toLowerCase());
            statusElement.setAttribute("title", this.data.getStatus(entry));
        }
    }
    updateOnlineStatusAndShow() {
        this.updateOnlineStatus();
        if (this.pagination && this.hideOffline)
            this.show();
    }
    updateStats() {
        this.updateScoreboard();
        this.updateOnlineStatus();
    }
    updateStatsAndShow() {
        this.updateScoreboard();
        this.updateOnlineStatus();
        this.show();
    }
    changeHideOffline(hideOffline) {
        this.hideOffline = hideOffline;
        if (this.pagination) {
            this.pagination.changePage(1);
            this.show();
        }
    }
    // Re-display table contents
    show() {
        this.table.innerHTML = "";
        this.table.append(this.headerElem);
        const scores = this.getScores();
        const [min, max] = this.pagination
            ? this.pagination.getRange(scores.length)
            : [0, scores.length];
        for (let i = min; i < max; i++) {
            if (this.showSkins)
                this.setSkin(scores[i][1], this.rows.get(scores[i][1]));
            this.table.append(this.rows.get(scores[i][1]));
        }
    }
    // Sort a HTML table element
    sort(by = this.sortBy, descending = this.descending) {
        this.data.sort(by, descending);
        this.show();
    }
    // When a table header is clicked, sort by that header
    thClick(e) {
        let objective = e.target.innerText;
        this.descending = (objective === this.sortBy) ? !this.descending : true;
        this.sortBy = objective;
        if (this.pagination)
            this.pagination.changePage(1);
        this.sort();
    }
    static appendElement(base, type) {
        let el = document.createElement(type);
        base.append(el);
        return el;
    }
    static prependElement(base, type) {
        let el = document.createElement(type);
        document.createElement;
        base.prepend(el);
        return el;
    }
    static appendTextElement(base, type, name) {
        let el = Display.appendElement(base, type);
        el.innerText = name;
        return el;
    }
    static appendTh(base, name, onclick, colspan) {
        let th = Display.appendTextElement(base, "th", name);
        th.onclick = onclick;
        if (colspan != undefined)
            th.setAttribute("colspan", String(colspan));
        return th;
    }
    static appendImg(base, src) {
        let img = Display.appendElement(base, "img");
        img.src = src;
        return img;
    }
}
Display.CONSOLE_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAPElEQVQ4T2NUUlL6z0ABYBw1gGE0DBioHAZ3795lUFZWJildosQCRQaQoxnkVLgL0A2A8dFpdP8NfEICAMkiK2HeQ9JUAAAAAElFTkSuQmCC";
// Replace single quotes by '&quot;' (html-escape)
Display.quoteEscape = (string) => string.replace(/'/g, "&quot;");

class Pagination {
    constructor(displayCount, elem) {
        this.displayCount = displayCount;
        this.currentPage = 1;
        this.parentElem = elem;
        this.selectElem = elem.querySelector("select.webstats-pagination[name=page]");
        this.prevButton = elem.querySelector("button.webstats-pagination[name=prev]");
        this.nextButton = elem.querySelector("button.webstats-pagination[name=next]");
        this.selectElem.addEventListener("change", (e) => this.changePageAndCallback(Number(e.target.value)));
        this.prevButton.addEventListener("click", () => this.changePageAndCallback(this.currentPage - 1));
        this.nextButton.addEventListener("click", () => this.changePageAndCallback(this.currentPage + 1));
    }
    static create(displayCount, elem) {
        const prevButton = elem.appendChild(document.createElement("button"));
        prevButton.classList.add("webstats-pagination");
        prevButton.name = "prev";
        prevButton.innerText = "Prev";
        const pageSelect = elem.appendChild(document.createElement("select"));
        pageSelect.classList.add("webstats-pagination");
        pageSelect.name = "page";
        const nextButton = elem.appendChild(document.createElement("button"));
        nextButton.classList.add("webstats-pagination");
        nextButton.name = "next";
        nextButton.innerText = "Next";
        return new Pagination(displayCount, elem);
    }
    update(nEntries) {
        this.maxPage = Math.ceil(nEntries / this.displayCount);
        // Hide all controls when there is only one page
        if (this.maxPage == 1) {
            this.parentElem.classList.add("pagination-hidden");
        }
        else {
            this.parentElem.classList.remove("pagination-hidden");
        }
        // Page selector
        if (this.selectElem) {
            this.selectElem.innerHTML = "";
            for (let i = 1; i <= this.maxPage; i++) {
                const optionElem = document.createElement("option");
                optionElem.innerText = String(i);
                this.selectElem.append(optionElem);
            }
            this.selectElem.value = String(this.currentPage);
        }
        // "Prev" button
        if (this.prevButton)
            this.prevButton.toggleAttribute("disabled", this.currentPage <= 1);
        // "Next" button
        if (this.nextButton)
            this.nextButton.toggleAttribute("disabled", this.currentPage >= this.maxPage);
    }
    changePage(page) {
        page = Math.max(1, Math.min(page, this.maxPage));
        this.currentPage = page;
    }
    changePageAndCallback(page) {
        this.changePage(page);
        console.log("callback");
        if (this.onPageChange)
            this.onPageChange(this.currentPage);
    }
    getRange(nEntries) {
        const min = (this.currentPage - 1) * this.displayCount;
        const max = (this.displayCount > 0)
            ? Math.min(this.currentPage * this.displayCount, nEntries)
            : nEntries;
        return [min, max];
    }
}

class WebStats {
    constructor(config) {
        this.displays = [];
        this.connection = config.connection ?? Connection.json(config.host);
        this.updateInterval = config.updateInterval ?? 10000;
        // Status HTML elements
        const statusElem = document.querySelector(".webstats-status");
        this.loadingElem = statusElem?.querySelector(".webstats-loading-indicator");
        this.errorElem = statusElem?.querySelector(".webstats-error-message");
        this.setLoadingStatus(true);
        // Get data and init
        const statsPromise = this.connection.getStats();
        const tableConfigsPromise = this.connection.getTables();
        Promise.all([statsPromise, tableConfigsPromise])
            .then(([stats, tableConfigs]) => this.init(stats, tableConfigs, config))
            .catch(e => {
            console.error(e);
            console.warn(WebStats.CONNECTION_ERROR_MSG);
            this.setErrorMessage(WebStats.CONNECTION_ERROR_MSG, config);
            this.setLoadingStatus(false);
        });
        // Get saved toggles from cookies
        const cookies = document.cookie.split("; ") ?? [];
        cookies.filter(str => str.length > 0).forEach(cookie => {
            const [property, value] = cookie.match(/[^=]+/g);
            document.documentElement.classList.toggle(property, value == "true");
            const el = document.querySelector("input.webstats-option#" + property);
            if (el)
                el.checked = (value == "true");
        });
        // On config option toggle, set the html element's class and store cookie
        document.querySelectorAll("input.webstats-option").forEach(el => el.addEventListener("change", () => {
            document.documentElement.classList.toggle(el.id, el.checked);
            // Set a cookie which expires in 10 years
            document.cookie = `${el.id}=${el.checked}; max-age=${60 * 60 * 24 * 365 * 10}; SameSite=Lax`;
        }));
        const optionHideOffline = document.querySelector("input.webstats-option#hide-offline");
        if (optionHideOffline) {
            // Re-show if displayCount is set
            optionHideOffline.addEventListener("change", (e) => {
                this.displays.forEach(display => display.changeHideOffline(optionHideOffline.checked));
            });
            this.displays.forEach(display => display.changeHideOffline(optionHideOffline.checked));
        }
        window.webstats = this;
    }
    init(data, tableConfigs, config) {
        if (config.tables) {
            for (const tableName in config.tables) {
                const tableConfig = tableConfigs
                    ? tableConfigs.find(tc => (tc.name ?? "") == tableName)
                    : { colums: data.scoreboard.columns };
                if (tableConfig)
                    this.addTableManual(config, tableConfig);
            }
        }
        else {
            if (tableConfigs) {
                for (const tableConfig of tableConfigs) {
                    this.addTableAutomatic(config, tableConfig);
                }
            }
            else {
                this.addTableAutomatic(config, { colums: data.scoreboard.columns });
            }
        }
        this.data = new Data(data);
        this.displays.forEach(display => {
            display.init(this.data);
            display.sort();
        });
        // Set update interval
        if (this.updateInterval > 0)
            this.startUpdateInterval(true);
        document.addEventListener("visibilitychange", () => document.hidden
            ? this.stopUpdateInterval() : this.startUpdateInterval());
        this.setLoadingStatus(false);
    }
    update() {
        // When nobody is online, assume scoreboard does not change
        if (this.data.nOnline > 0) {
            this.connection.getStats().then(data => {
                this.data.setStats(data);
                this.displays.forEach(display => display.updateStatsAndShow());
            }).catch(e => {
                console.error(e);
                console.warn(WebStats.CONNECTION_ERROR_MSG);
                this.setErrorMessage(WebStats.CONNECTION_ERROR_MSG);
                this.stopUpdateInterval();
            });
        }
        else {
            this.connection.getOnline().then(data => {
                this.data.setOnlineStatus(data);
                this.displays.forEach(display => display.updateOnlineStatusAndShow());
            }).catch(e => {
                console.error(e);
                console.warn(WebStats.CONNECTION_ERROR_MSG);
                this.setErrorMessage(WebStats.CONNECTION_ERROR_MSG);
                this.stopUpdateInterval();
            });
        }
    }
    startUpdateInterval(first) {
        this.interval = setInterval(this.update.bind(this), this.updateInterval);
        if (!first)
            this.update();
    }
    stopUpdateInterval() {
        clearInterval(this.interval);
    }
    addTableManual(config, tableConfig) {
        let pagination;
        if (config.displayCount > 0 && config.tables[tableConfig.name ?? ""].pagination) {
            const paginationParent = config.tables[tableConfig.name ?? ""].pagination;
            pagination = new Pagination(config.displayCount, paginationParent);
        }
        this.displays.push(new Display({ ...config, table: config.tables[tableConfig.name ?? ""].table, pagination: pagination }, tableConfig));
    }
    addTableAutomatic(config, tableConfig) {
        const headerElem = config.tableParent
            .appendChild(document.createElement("div"));
        headerElem.classList.add("webstats-tableheading");
        if (tableConfig.name) {
            headerElem.innerText = tableConfig.name;
            headerElem.setAttribute("webstats-table", tableConfig.name);
        }
        let pagination;
        if (config.displayCount > 0) {
            const paginationControls = headerElem.appendChild(document.createElement("span"));
            paginationControls.classList.add("webstats-pagination");
            pagination = Pagination.create(config.displayCount, paginationControls);
        }
        const tableElem = config.tableParent
            .appendChild(document.createElement("table"));
        if (tableConfig.name)
            tableElem.setAttribute("webstats-table", tableConfig.name);
        this.displays.push(new Display({ ...config, table: tableElem, pagination: pagination }, tableConfig));
    }
    setLoadingStatus(loading) {
        if (!this.loadingElem)
            return;
        this.loadingElem.style.display = loading ? "inline" : "none";
    }
    setErrorMessage(msg, config) {
        if (this.errorElem)
            this.errorElem.innerText = msg;
        else {
            const spanElem = document.createElement("span");
            spanElem.innerText = msg;
            spanElem.classList.add("webstats-error-message");
            if (config?.tableParent) {
                config.tableParent.appendChild(spanElem);
            }
            else if (config?.tables) {
                for (const tablename in config.tables) {
                    if (config.tables[tablename].table)
                        config.tables[tablename].table.appendChild(spanElem);
                }
            }
        }
    }
}
WebStats.CONNECTION_ERROR_MSG = "No connection to server. Either the server is offline, or the 'host' setting in index.html is incorrect.";

export { WebStats as default };
//# sourceMappingURL=WebStats-dist.js.map
