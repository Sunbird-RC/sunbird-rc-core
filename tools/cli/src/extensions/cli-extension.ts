// @/extensions/cli-extension.ts
// Register extensions to the toolbox

import EventEmitter from 'events'
import path from 'path'

import yaml from 'yaml'

import check from '../toolbox/environment/check'
import create from '../toolbox/registry/create'
import status from '../toolbox/registry/status'
import restart from '../toolbox/registry/restart'
import down from '../toolbox/registry/down'
import health from '../toolbox/registry/health'

import {
	CLIEvent,
	RegistryConfiguration,
	RegistrySetupOptions,
	Toolbox,
} from '../types'

export default (toolbox: Toolbox) => {
	// Event emmitter
	toolbox.events = new EventEmitter()

	toolbox.environment = {
		// Check if necessary tools are installed
		check: check(toolbox),
	}

	toolbox.registry = {
		// Create a new registry instance in the current directory
		create: (registryConfig: RegistrySetupOptions) =>
			create(toolbox, registryConfig),
		// View registry status
		status: () => status(toolbox),
		// Restart all containers
		restart: (soft: boolean) => restart(toolbox, soft),

		// Read and return the registry configuration
		config: async (): Promise<RegistryConfiguration> => {
			const registryMetadata = await yaml.parse(
				(await toolbox.filesystem.readAsync(
					path.resolve(process.cwd(), 'registry.yaml')
				)) ?? ''
			)

			if (!registryMetadata) {
				throw new Error(
					'This directory does not contain the necessary data to setup/manage a registry instance.'
				)
			}

			return registryMetadata.registry
		},
		down: () => down(toolbox),
		health: () => health(toolbox),
	}
	const { print } = toolbox
	const spinner = print.spin('Loading...').stop()
	toolbox.handleEvent = (event: CLIEvent) => {
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

			print.error('')
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

	toolbox.until = async (conditionFunction: () => Promise<boolean>) => {
		const poll = async (resolve: (value: unknown) => void) => {
			if (await conditionFunction()) resolve(true)
			else setTimeout((_) => poll(resolve), 400)
		}

		return new Promise(poll)
	}

	return toolbox
}
