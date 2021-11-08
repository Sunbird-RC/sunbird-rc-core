// @/toolbox/environment/check
// Check the environment

import path from 'path'

import { Toolbox } from '../../types'

// Accept a toolbox, return an environment checker
export default (toolbox: Toolbox) => async (registryPath?: string) => {
	const { events, filesystem, system } = toolbox

	events.emit('environment.check', {
		status: 'progress',
		operation: 'checking-env',
		message: 'Checking environment',
	})

	// If the path to the cwd is provided, check if the directory is has a
	// registry.yaml file
	if (
		registryPath &&
		!(await filesystem.existsAsync(path.resolve(registryPath, 'registry.yaml')))
	) {
		events.emit('environment.check', {
			status: 'error',
			operation: 'missing-file',
			message:
				'This directory does not contain the necessary data to setup/manage a registry instance.',
		})

		return false
	}

	// Check that necessary tools are installed
	if (!system.which('docker')) {
		events.emit('environment.check', {
			status: 'error',
			operation: 'missing-tool',
			message: 'Could not find `docker` on your system.',
		})
		return false
	}
	if (!system.which('docker-compose')) {
		events.emit('environment.check', {
			status: 'error',
			operation: 'missing-tool',
			message: 'Could not find `docker-compose` on your system.',
		})
		return false
	}

	// All good!
	events.emit('environment.check', {
		status: 'success',
		operation: 'checking-env',
		message: 'All necessary tools installed!',
	})
	return true
}
