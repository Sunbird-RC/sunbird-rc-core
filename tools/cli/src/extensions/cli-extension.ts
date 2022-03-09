// @/extensions/cli-extension.ts
// Register extensions to the toolbox

import EventEmitter from 'events'
import path from 'path'

import yaml from 'yaml'

import check from '../toolbox/environment/check'
import create from '../toolbox/registry/create'
import status from '../toolbox/registry/status'
import restart from '../toolbox/registry/restart'

import { RegistryConfiguration, RegistrySetupOptions, Toolbox } from '../types'

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
	}

	return toolbox
}
