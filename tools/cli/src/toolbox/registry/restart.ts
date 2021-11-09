// @/toolbox/registry/restart
// View registry restart

import { RegistryContainer, Toolbox } from '../../types'

// Accept a toolbox, return a registry restart viewer
export default async (toolbox: Toolbox) => {
	const { events, system } = toolbox

	events.emit('registry.restart', {
		status: 'progress',
		operation: 'restart-registry',
		message: 'Restarting all registry containers',
	})

	await system
		.exec('docker compose up --force-recreate -d')
		.catch((error: Error) => {
			events.emit('registry.create', {
				status: 'error',
				operation: 'restart-registry',
				message: `An unexpected error occurred while restarting the registry: ${error.message}`,
			})
		})
	// Wait for 40 seconds for them to start
	await new Promise((resolve) => setTimeout(resolve, 40000))

	// All done!
	events.emit('registry.restart', {
		status: 'success',
		operation: 'restart-registry',
		message: 'Successfully restarted registry!',
	})
}
