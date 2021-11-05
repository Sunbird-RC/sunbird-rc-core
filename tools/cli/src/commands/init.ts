// @/commands/init
// `registry init` command
// Creates a new registry instance

import { CLIEvent, RegistryConfig, Toolbox } from '../types'

export default {
	name: 'init',
	alias: ['i'],
	run: async (toolbox: Toolbox) => {
		const { environment, events, print, prompt, registry, strings } = toolbox

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
		events.on('registry.create', handleEvent)

		// Print the title
		print.info('')
		print.info(print.colors.green.bold('Registry Setup'))
		print.info('')

		// Check that all tools are installed
		await environment.check()

		// Get neccesary information
		print.info('')
		const config = await prompt.ask([
			{
				type: 'input',
				message: print.colors.reset('Enter the name of the registry'),
				name: 'registryName',
				initial: strings.startCase(
					process.cwd().split('/').slice(-1).join('/')
				),
			},
			{
				type: 'input',
				message: print.colors.reset(
					'Enter the name of the Keycloak realm to create'
				),
				name: 'realmName',
				initial: 'sunbird-rc',
			},
			{
				type: 'input',
				message: print.colors.reset(
					'Enter the ID to assign to the admin client in Keycloak'
				),
				name: 'keycloakAdminClientId',
				initial: 'admin-api',
			},
			{
				type: 'input',
				message: print.colors.reset(
					'Enter the ID to assign to the frontend client in Keycloak'
				),
				name: 'keycloakFrontendClientId',
				initial: 'registry-frontend',
			},
			{
				type: 'input',
				message: print.colors.reset(
					'Enter a username for the admin account in Keycloak'
				),
				name: 'keycloakAdminUser',
				initial: 'admin',
			},
			{
				type: 'input',
				message: print.colors.reset(
					'Enter a password for the admin account in Keycloak'
				),
				name: 'keycloakAdminPass',
				initial: 'admin',
			},
			{
				type: 'input',
				message: print.colors.reset(
					'Enter the path to a directory containing entity schemas'
				),
				name: 'pathToEntitySchemas',
				initial: 'use-example-config',
			},
			{
				type: 'input',
				message: print.colors.reset(
					'Enter the path to a file containing consent configuration'
				),
				name: 'pathToConsentConfiguration',
				initial: 'use-example-config',
			},
		])
		print.info('')

		// Setup the registry
		await registry.create(config as unknown as RegistryConfig)

		print.info('')
	},
}
