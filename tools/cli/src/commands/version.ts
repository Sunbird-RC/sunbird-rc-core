// @/commands/version
// `registry version` command
// Show the user the version of the CLI

import { Toolbox } from '../types'

export default {
	name: 'version',
	run: async (toolbox: Toolbox) => {
		const { meta, print } = toolbox

		print.info(
			// prettier-ignore
			`
${print.colors.green(print.colors.bold('cli'))}  ${print.colors.cyan(meta.version())}
${print.colors.green(print.colors.bold('node'))} ${print.colors.cyan(process.versions.node)}
${print.colors.green(print.colors.bold('os'))}   ${print.colors.cyan(process.platform + ' ' + process.arch)}
			`
		)
	},
}
