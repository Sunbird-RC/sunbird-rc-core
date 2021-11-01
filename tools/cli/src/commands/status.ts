// @/commands/status
// `registry status` command
// Shows the status of all containers of the registry instance

import { CLIEvent, RegistryContainer, Toolbox } from '../types'

export default {
	name: 'status',
	alias: ['s'],
	run: async (toolbox: Toolbox) => {
		const { environment, events, print, registry } = toolbox

		// Listen to events and show progress
		const spinner = print.spin('Loading...').stop()
		const handleEvent = (event: CLIEvent) => {
			// Print and exit on error
			if (event.status === 'error') {
				// Stop the spinner if it is running...
				if (spinner.isSpinning) {
					// ...and print the error text in its place
					spinner.fail(print.colors.error(event.message))
				} else {
					// Else just print the message
					print.error(print.colors.error(`${print.xmark} ${event.message}`))
				}

				process.exit(1)
			}

			// If the function has finished, stop loading
			if (event.status === 'success') {
				if (spinner.isSpinning) {
					spinner.stop()
				}
			}

			// If it is a progress event, show a spinner
			if (event.status === 'progress') {
				spinner.start(print.colors.highlight(`${event.message}...`))
			}
		}

		events.on('environment.check', handleEvent)
		events.on('registry.status', handleEvent)

		// Check that all tools are installed
		await environment.check(process.cwd())
		// Get the registry status
		const containers = await registry.status()

		// Print the name of the registry
		print.info('')
		print.info(print.colors.green.bold(containers[0].registry))
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
