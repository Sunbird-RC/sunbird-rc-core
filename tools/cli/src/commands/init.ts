// @/commands/init
// `registry init` command
// Creates a new registry instance

import path from 'path'

import { CLIEvent, RegistrySetupOptions, Toolbox } from '../types'

export default {
	name: 'init',
	run: async (toolbox: Toolbox) => {
		const {
			environment,
			events,
			filesystem,
			print,
			prompt,
			registry,
			strings,
			handleEvent
		} = toolbox
		
		events.on('environment.check', handleEvent)
		events.on('registry.create', handleEvent)

		// Print the title
		print.info('')
		print.info(print.colors.green.bold('Registry Setup'))
		print.info('')

		// Check that all tools are installed
		await environment.check()

		// Check if a registry already exists in the current directory
		if (
			await filesystem.existsAsync(path.resolve(process.cwd(), 'registry.yaml'))
		) {
			events.emit('registry.create', {
				status: 'error',
				operation: 'checking-env',
				message: `A registry has already been setup in the current directory.`,
			})
		}

		// Get neccesary information
		print.info('')
		const options = await prompt.ask([
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
		await registry.create(options as unknown as RegistrySetupOptions)

		print.info('')
		print.highlight('Sunbird-RC is configured with test/default keys for signing. It is required to be updated `imports/config.json` before going live/production')
	},
}
