// @/commands/default
// The default command to run

import { GluegunCommand } from 'gluegun'

const command: GluegunCommand = {
	name: 'registry-cli',
	run: async (toolbox) => {
		const { print } = toolbox

		print.info(
			`Run ${print.colors.cyan(
				'registry help'
			)} for a list of commands you can run`
		)
	},
}

export default command
