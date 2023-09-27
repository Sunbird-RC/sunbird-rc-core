// @/commands/restart
// `registry restart` command
// Restarts all the containers that are part of a registry instance

import { CLIEvent, Toolbox } from '../types'

export default {
	name: 'restart',
	run: async (toolbox: Toolbox) => {
		const { environment, events, parameters, print, registry, handleEvent } =
			toolbox

		// Listen to events and show progress

		events.on('environment.check', handleEvent)
		events.on('registry.restart', handleEvent)

		// Print the name of the registry
		print.info('')
		print.info(print.colors.green.bold((await registry.config()).name))
		print.info('')

		// Check that all tools are installed
		await environment.check(process.cwd())
		// Restart the registry
		await registry.restart(parameters.options.s ?? parameters.options.soft)

		print.info('')
	},
}
