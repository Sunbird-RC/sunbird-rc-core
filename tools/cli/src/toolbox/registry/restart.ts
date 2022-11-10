// @/toolbox/registry/restart
// View registry restart

import { allUp } from './status'

import { Toolbox } from '../../types'

// Accept a toolbox, return a registry restart viewer
export default async (toolbox: Toolbox, soft: boolean) => {
	const { events, system, until } = toolbox
	
	events.emit('registry.restart', {
		status: 'progress',
		operation: 'restart-registry',
		message: 'Restarting all registry containers',
	})

	await system
		.exec(
			soft ? 'docker compose restart' : 'docker compose up --force-recreate -d'
		)
		.catch((error: Error) => {
			events.emit('registry.create', {
				status: 'error',
				operation: 'restart-registry',
				message: `An unexpected error occurred while restarting the registry: ${error.message}`,
			})
		})
	// Wait for the containers to start
	await until(allUp)

	// All done!
	events.emit('registry.restart', {
		status: 'success',
		operation: 'restart-registry',
		message: 'Successfully restarted registry!',
	})
}
