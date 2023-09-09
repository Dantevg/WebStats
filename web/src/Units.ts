const timeUnits = ["ticks", "milliseconds", "ms", "seconds", "s", "minutes", "min", "hours", "h", "days", "d"]
const distanceUnits = ["blocks", "millimetres", "millimeters", "mm", "centimetres", "centimeters", "cm", "metres", "meters", "m", "kilometres", "kilometers", "km"]
const dateUnits = ["timestamp", "formatted date"]
const itemUnits = ["items", "items16"]

const offsets = {
	milliseconds: 0.001,
	ms: 0.001,
	ticks: 0.05,
	seconds: 1,
	s: 1,
	minutes: 60,
	min: 60,
	hours: 60 * 60,
	h: 60 * 60,
	days: 24 * 60 * 60,
	d: 24 * 60 * 60,
	millimetres: 0.001,
	millimeters: 0.001,
	mm: 0.001,
	centimetres: 0.01,
	centimeters: 0.01,
	cm: 0.01,
	blocks: 1,
	metres: 1,
	meters: 1,
	m: 1,
	kilometres: 1000,
	kilometers: 1000,
	km: 1000,
	items: 1,
	items16: 1,
}

function convertTimeUnit(source: number, unit: string): string {
	if (source == 0) return "0"

	const inSeconds = source * offsets[unit]

	const date = new Date(inSeconds * 1000)
	const d = Math.floor(inSeconds / offsets.days)
	const h = date.getUTCHours()
	const m = date.getUTCMinutes()
	const s = date.getUTCSeconds() + date.getUTCMilliseconds() / 1000

	if (h > 0 || d > 0) {
		return `${h + d*24}:${String(m).padStart(2, "0")} h`
	} else if (m > 0) {
		return `${m}:${String(Math.floor(s)).padStart(2, "0")}`
	} else {
		return `${s} s`
	}
}

function convertDistanceUnit(source: number, unit: string): string {
	if (source == 0) return "0"

	const inBlocks = source * offsets[unit]

	if (inBlocks < offsets.cm) {
		return Math.floor(inBlocks / offsets.mm) + " mm"
	} else if (inBlocks < offsets.m) {
		return Math.floor(inBlocks / offsets.cm) + " cm"
	} else if (inBlocks < offsets.km) {
		return Math.floor(inBlocks) + " m"
	} else {
		return Math.floor(inBlocks / offsets.km) + " km"
	}
}

function convertDateUnit(source: string, unit: string): string {
	const asTimestamp = (unit == "timestamp") ? new Date(Number(source)) : new Date(source)

	return asTimestamp.toLocaleString()
}

function convertItemUnit(source: number, unit: string): string {
	if (source == 0) return "0"

	const inItems = source
	const stackSize = (unit == "items16") ? 16 : 64

	const stacks = Math.floor(inItems / stackSize)
	const items = inItems % stackSize
	if (stacks > 0) {
		return (items > 0) ? `${stacks} st ${items}` : `${stacks} st`
	} else {
		return String(items)
	}
}

export function autoConvertUnits(source: string, unit: string): string {
	if (timeUnits.includes(unit)) {
		return convertTimeUnit(Number(source), unit)
	} else if (distanceUnits.includes(unit)) {
		return convertDistanceUnit(Number(source), unit)
	} else if (dateUnits.includes(unit)) {
		return convertDateUnit(source, unit)
	} else if (itemUnits.includes(unit)) {
		return convertItemUnit(Number(source), unit)
	} else return isNaN(source as any) ? source : Number(source).toLocaleString()
}
