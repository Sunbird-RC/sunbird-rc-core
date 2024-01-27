// @/commands/down
// `registry down` command
// Creates a new registry instance

import path from 'path'

import { RegistryTearDownOptions, Toolbox } from '../types'

export default {
	name: 'down',
	run: async (toolbox: Toolbox) => {
		const {
			environment,
			events,
			filesystem,
			print,
			prompt,
			registry,
			strings,
			handleEvent,
		} = toolbox

		events.on('environment.check', handleEvent)
		events.on('registry.down', handleEvent)

		// Print the title
		print.info('')
		print.info(print.colors.green.bold('Registry Teardown'))
		print.info('')

		// Check that all tools are installed
		await environment.check()

		// Check if a registry already exists in the current directory
		if (
			!(await filesystem.existsAsync(
				path.resolve(process.cwd(), 'registry.yaml')
			))
		) {
			events.emit('registry.create', {
				status: 'error',
				operation: 'checking-env',
				message: `A registry has not been setup in the current directory.`,
			})
		}

		// Get necessary information
		print.info('')
		const options: RegistryTearDownOptions = await prompt.ask([
			{
				type: 'input',
				message: print.colors.reset(
					'Do you really want to stop and remove the containers?'
				),
				name: 'stopConfirmation',
				initial: 'Y/N',
				required: true,
			},
		])
		print.info(options.stopConfirmation)
		if (options.stopConfirmation === 'Y' || options.stopConfirmation === 'y') {
			await registry.down()
		} else {
			print.error('Invalid option')
		}
		print.info('')
	},
}
