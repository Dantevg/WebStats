type ColourCode =
	| "§0"
	| "§1"
	| "§2"
	| "§3"
	| "§4"
	| "§5"
	| "§6"
	| "§7"
	| "§8"
	| "§9"
	| "§a"
	| "§b"
	| "§c"
	| "§d"
	| "§e"
	| "§f"

type FormattingCode =
	| "§k"
	| "§l"
	| "§m"
	| "§n"
	| "§o"
	| "§r"

type FormattingCodePart = {
	text: string
	format?: FormattingCode
	colour?: ColourCode | string
	colourType?: "simple" | "hex"
}

const COLOUR_CODES: {[code in ColourCode]: string} = {
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

const FORMATTING_CODES: {[code in FormattingCode]: string} = {
	["§k"]: "obfuscated",
	["§l"]: "bold",
	["§m"]: "strikethrough",
	["§n"]: "underline",
	["§o"]: "italic",
	["§r"]: "reset",
}

// § followed by a single character, or of the form §x§r§r§g§g§b§b
// (also capture rest of string, until next §)
const FORMATTING_CODE_REGEX = /(§x§.§.§.§.§.§.|§.)([^§]*)/gm

// Convert a single formatting code to a <span> element
function convertFormattingCode(part: FormattingCodePart) {
	if (!part.format && !part.colour) return part.text
	if (part.text.length == 0) return part.text
	
	const classes = ["mc-format"]
	if (part.format) classes.push(`mc-${part.format}`)
	if (part.colourType == "simple") classes.push(`mc-${part.colour}`)
	
	return (
		<span className={classes} style={part.colourType == "hex" && `color: ${part.colour}`}>
			{part.text}
		</span>
	)
}

function parseFormattingCodes(value: string): FormattingCodePart[] {
	const parts = []

	const firstIdx = value.matchAll(FORMATTING_CODE_REGEX).next().value?.index
	if (firstIdx == undefined || firstIdx > 0) {
		parts.push({ text: value.substring(0, firstIdx) })
	}

	for (const match of value.matchAll(FORMATTING_CODE_REGEX)) {
		parts.push(parseFormattingCode(match[1], match[2], parts[parts.length - 1]))
	}

	return parts
}

function parseFormattingCode(code: string, text: string, prev: FormattingCodePart): FormattingCodePart {
	// Simple colour codes and formatting codes
	if (COLOUR_CODES[code]) {
		return {
			text,
			colour: COLOUR_CODES[code],
			colourType: "simple",
		}
	}
	if (FORMATTING_CODES[code]) {
		if (FORMATTING_CODES[code] == "reset") return { text }
		return {
			text,
			format: FORMATTING_CODES[code],
			colour: prev?.colour,
			colourType: prev?.colourType,
		}
	}

	// Hex colour codes
	const matches = code.match(/§x§(.)§(.)§(.)§(.)§(.)§(.)/m)
	if (matches) {
		return {
			text,
			colour: "#" + matches.slice(1).join(""),
			colourType: "hex",
		}
	}

	// Not a valid formatting code, just return the input unaltered
	return { text }
}

// Replace all formatting codes by <span> elements
export const convertFormattingCodes = (value: string) =>
	parseFormattingCodes(value).map(convertFormattingCode)
