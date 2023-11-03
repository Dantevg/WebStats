import nodeResolve from "@rollup/plugin-node-resolve"
import typescript from "@rollup/plugin-typescript"
import terser from "@rollup/plugin-terser"

export default {
	input: "src/WebStats.ts",
	output: {
		file: "WebStats-dist.js",
		sourcemap: true,
		banner: `\
/*!
	WebStats version 1.10
	https://github.com/Dantevg/WebStats
	
	by RedPolygon
	
	Licence: MIT
	
	Includes modified version of @itsjavi/jsx-runtime
		(MIT licenced, https://github.com/itsjavi/jsx-runtime/)
*/
`,
	},
	treeshake: false,
	plugins: [
		nodeResolve(),
		typescript(),
		terser({format: {comments: /^!/}})
	],
}
