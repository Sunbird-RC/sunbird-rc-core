// @/commands/status
// `registry status` command
// Shows the status of all containers of the registry instance

import { CLIEvent, RegistryContainer, Toolbox } from '../types'

export default {
	name: 'status',
	run: async (toolbox: Toolbox) => {
		const { environment, events, print, registry, handleEvent } = toolbox

		events.on('environment.check', handleEvent)
		events.on('registry.status', handleEvent)

		// Check that all tools are installed
		await environment.check(process.cwd())
		// Get the registry status
		const containers = await registry.status()

		// Print the name of the registry
		print.info('')
		print.info(print.colors.green.bold((await registry.config()).name))
		print.info('')

		// Print the container details as a table
		print.table(
			[
				['ID', 'Name', 'Status', 'Port'],
				...containers.map((container: RegistryContainer) => [
					print.colors.yellow(container.id),
					print.colors.green(container.name),
					print.colors.cyan(container.status),
					print.colors.green(container.ports.join(', ')),
				]),
			],
			{
				format: 'markdown',
			}
		)

		print.info('')
	},
}
