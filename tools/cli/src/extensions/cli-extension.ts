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
		restart: () => restart(toolbox),
		config: async (): Promise<RegistryConfiguration> => {
			return await yaml.parse(
				(await toolbox.filesystem.readAsync(
					path.resolve(process.cwd(), 'registry.yaml')
				)) ?? ''
			).registry
		},
	}

	return toolbox
}
