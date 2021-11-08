// @/toolbox/registry/create
// Create a new registry instance in the current directory

import path from 'path'

import KeycloakWrapper from './helpers/keycloak'

import { RegistryConfig, Toolbox } from '../../types'

// Accept a toolbox and configuration, create a registry instance in return
export default async (toolbox: Toolbox, registryConfig: RegistryConfig) => {
	const { events, filesystem, patching, system, template } = toolbox
	const keycloak = new KeycloakWrapper({
		user: registryConfig.keycloakAdminUser,
		pass: registryConfig.keycloakAdminPass,
		realm: registryConfig.realmName,
	})

	// Copy over config files with the proper variables
	events.emit('registry.create', {
		status: 'progress',
		operation: 'copying-files',
		message: 'Copying over necessary files',
	})
	template.generate({
		template: 'registry.yaml',
		target: 'registry.yaml',
		props: registryConfig,
	})
	template.generate({
		template: 'docker-compose.yaml',
		target: 'docker-compose.yaml',
		props: registryConfig,
	})
	template.generate({
		template: 'config/keycloak/realm-export.json',
		target: 'config/keycloak/realm-export.json',
		props: registryConfig,
	})
	if (registryConfig.pathToEntitySchemas === 'use-example-config') {
		template.generate({
			template: 'config/schemas/student.json',
			target: 'config/schemas/student.json',
			props: registryConfig,
		})
		template.generate({
			template: 'config/schemas/teacher.json',
			target: 'config/schemas/teacher.json',
			props: registryConfig,
		})
	} else {
		await filesystem
			.copyAsync(
				path.resolve(registryConfig.pathToEntitySchemas),
				path.resolve(process.cwd(), 'config/schemas/'),
				{ overwrite: true }
			)
			.catch((error: Error) => {
				events.emit('registry.create', {
					status: 'error',
					operation: 'copying-files',
					message: `An unexpected error occurred while copying the schema files: ${error.message}`,
				})
			})
	}
	if (registryConfig.pathToConsentConfiguration === 'use-example-config') {
		template.generate({
			template: 'config/consent.json',
			target: 'config/consent.json',
			props: registryConfig,
		})
	} else {
		await filesystem
			.copyAsync(
				path.resolve(registryConfig.pathToConsentConfiguration),
				path.resolve(process.cwd(), 'config/consent.json'),
				{ overwrite: true }
			)
			.catch((error: Error) => {
				events.emit('registry.create', {
					status: 'error',
					operation: 'copying-files',
					message: `An unexpected error occurred while copying the consent configuration: ${error.message}`,
				})
			})
	}
	events.emit('registry.create', {
		status: 'success',
		operation: 'copying-files',
		message: 'Successfully copied over necessary files',
	})

	// Start containers
	events.emit('registry.create', {
		status: 'progress',
		operation: 'starting-containers',
		message: 'Starting all services',
	})
	await system.exec('docker compose up -d').catch((error: Error) => {
		events.emit('registry.create', {
			status: 'error',
			operation: 'starting-containers',
			message: `An unexpected error occurred while starting the registry: ${error.message}`,
		})
	})
	// Wait for 40 seconds for them to start
	await new Promise((resolve) => setTimeout(resolve, 40000))
	events.emit('registry.create', {
		status: 'success',
		operation: 'starting-containers',
		message: 'Successfully started all services!',
	})

	// Configure keycloak
	// The realm-export file is automatically imported by keycloak on startup.
	// However, keycloak does not export client secret along with it. So we need to
	// regenerate it and set it in the docker-compose file before starting the registry
	events.emit('registry.create', {
		status: 'progress',
		operation: 'configuring-keycloak-admin-api',
		message: 'Configuring keycloak',
	})
	try {
		// Regenerate the client secret
		const clientSecret = await keycloak.regenerateClientSecret(
			await keycloak.getInternalClientId(registryConfig.keycloakAdminClientId)
		)
		// Replace the old client secret with the new one
		await patching.replace(
			path.resolve(process.cwd(), 'docker-compose.yaml'),
			'INSERT_SECRET_HERE',
			clientSecret
		)

		events.emit('registry.create', {
			status: 'success',
			operation: 'configuring-keycloak-admin-api',
			message: 'Successfully configured keycloak!',
		})
	} catch (errorObject: unknown) {
		const error = errorObject as Error

		events.emit('registry.create', {
			status: 'error',
			operation: 'configuring-keycloak-admin-api',
			message: `An unexpected error occurred while configuring keycloak: ${error.message}`,
		})
	}

	// Check if we need to create scopes in keycloak
	if (
		await filesystem.existsAsync(
			path.resolve(process.cwd(), 'config/consent.json')
		)
	) {
		events.emit('registry.create', {
			status: 'progress',
			operation: 'configuring-keycloak-consent',
			message: 'Setting up consent provider',
		})

		try {
			// See ../../templates/examples/student-teacher/config/consent.json for
			// a sample consent configuration file
			const consentData = JSON.parse(
				(await filesystem.readAsync(
					path.resolve(process.cwd(), 'config/consent.json')
				))!
			)

			const clientId = await keycloak.getInternalClientId(
				registryConfig.keycloakFrontendClientId
			)
			for (const scope of consentData.scopes) {
				const scopeId = await keycloak.createClientScope(scope)

				await keycloak.addOptionalClientScope(clientId, scopeId)
			}

			events.emit('registry.create', {
				status: 'success',
				operation: 'configuring-keycloak-consent',
				message: 'Successfully setup keycloak as a consent provider!',
			})
		} catch (rawError: any) {
			const error = rawError as Error

			events.emit('registry.create', {
				status: 'error',
				operation: 'configuring-keycloak-consent',
				message: `An unexpected error occurred while setting up the consent provider: ${error.message}`,
			})
		}
	}

	// Restart containers
	events.emit('registry.create', {
		status: 'progress',
		operation: 'restarting-containers',
		message: 'Restarting all services',
	})
	await system
		.exec('docker compose up --force-recreate -d')
		.catch((error: Error) => {
			events.emit('registry.create', {
				status: 'error',
				operation: 'restarting-containers',
				message: `An unexpected error occurred while restarting the registry: ${error.message}`,
			})
		})
	// Wait for 40 seconds for them to start
	await new Promise((resolve) => setTimeout(resolve, 40000))

	// All done!
	events.emit('registry.create', {
		status: 'success',
		operation: 'create-registry',
		message: 'Created new registry instance successfully!',
	})
}
