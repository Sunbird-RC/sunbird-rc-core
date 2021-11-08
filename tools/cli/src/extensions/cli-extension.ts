// @/extensions/cli-extension.ts
// Register extensions to the toolbox

import EventEmitter from 'events'

import check from '../toolbox/environment/check'
import create from '../toolbox/registry/create'
import status from '../toolbox/registry/status'

import { RegistryConfig, Toolbox } from '../types'

export default (toolbox: Toolbox) => {
	// Event emmitter
	toolbox.events = new EventEmitter()

	toolbox.environment = {
		// Check if necessary tools are installed
		check: check(toolbox),
	}

	toolbox.registry = {
		// Create a new registry instance in the current directory
		create: (registryConfig: RegistryConfig) => create(toolbox, registryConfig),
		// View registry status
		status: status(toolbox),
	}

	return toolbox
}
