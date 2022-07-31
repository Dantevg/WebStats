export default {
	input: "rollup-index.js",
	output: {
		file: "WebStats-dist.js",
		banner: `\
/*
	WebStats version 1.7
	https://github.com/Dantevg/WebStats
	
	by RedPolygon
	
	Licence: MIT
*/
`,
	},
	treeshake: false,
}
