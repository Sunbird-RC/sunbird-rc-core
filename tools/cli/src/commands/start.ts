// @/commands/start
// `registry start` command
// Starts all the containers that are part of a registry instance

import { CLIEvent, Toolbox } from '../types'

export default {
	name: 'start',
	run: async (toolbox: Toolbox) => {
		const { environment, events, handleEvent, print, registry } = toolbox

		events.on('environment.check', handleEvent)
		events.on('registry.restart', handleEvent)

		// Print the name of the registry
		print.info('')
		print.info(print.colors.green.bold((await registry.config()).name))
		print.info('')

		// Check that all tools are installed
		await environment.check(process.cwd())
		// Restart the registry
		await registry.restart(true)

		print.info('')
	},
}
