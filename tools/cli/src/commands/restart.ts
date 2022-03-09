// @/commands/restart
// `registry restart` command
// Restarts all the containers that are part of a registry instance

import { CLIEvent, Toolbox } from '../types'

export default {
	name: 'restart',
	run: async (toolbox: Toolbox) => {
		const { environment, events, parameters, print, registry } = toolbox

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

			// Print and continue on success
			if (event.status === 'success') {
				// Stop the spinner if it is running...
				if (spinner.isSpinning) {
					// ...and print the success text in its place
					spinner.succeed(print.colors.success(event.message))
				} else {
					// Else just print the message
					print.success(
						print.colors.success(`${print.checkmark} ${event.message}`)
					)
				}
			}

			// If it is a progress event, show a spinner
			if (event.status === 'progress') {
				spinner.start(print.colors.highlight(`${event.message}...`))
			}
		}

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
