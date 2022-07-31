export default {
	input: "rollup-index.js",
	output: {
		file: "WebStats-dist.js",
		banner: "/*\n\tWebStats version 1.7\n\thttps://github.com/Dantevg/WebStats\n\t\n\tby RedPolygon\n\t\n\tLicence: MIT\n*/\n",
	},
	treeshake: false,
}
