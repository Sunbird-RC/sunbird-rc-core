// @/commands/health
// `registry health` command
// Shows the health of all containers of the registry instance

import { RegistryHealth, Toolbox } from '../types'

export default {
	name: 'health',
	run: async (toolbox: Toolbox) => {
		const { environment, events, print, registry, handleEvent } = toolbox

		events.on('environment.check', handleEvent)
		events.on('registry.health', handleEvent)

		// Check that all tools are installed
		await environment.check(process.cwd())
		// Get the registry health
		const containers = await registry.health()

		// Print the name of the registry
		print.info('')
		print.info(print.colors.green.bold((await registry.config()).name))
		print.info('')

		// Print the container details as a table
		print.table(
			[
				['Name', 'Health', 'Error'],
				...containers.map((container: RegistryHealth) => [
					print.colors.green(container.name),
					print.colors.cyan(container.status),
					print.colors.cyan(container.err),
				]),
			],
			{
				format: 'markdown',
			}
		)

		print.info('')
	},
}
