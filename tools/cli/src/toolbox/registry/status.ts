// @/toolbox/registry/status
// View registry status

import { RegistryContainer, Toolbox} from '../../types';
// A helper function to check if all the containers are up and running
import { system } from 'gluegun'
// Accept a toolbox, return a registry status viewer
export default async (toolbox: Toolbox): Promise<RegistryContainer[]> => {
	const { events, system } = toolbox

	events.emit('registry.status', {
		status: 'progress',
		operation: 'get-registry-status',
		message: 'Checking on registry',
	})

	// List containers
	let rawJson;
	try {
		let dockerFunciton = await system.run('docker compose ps --format json')
		rawJson = JSON.parse(dockerFunciton);
	} catch (error) {
		let dockerFunciton = await system.run('docker compose ps --format json')
		let jsonArray = []
		if (dockerFunciton) {
			// Convert the string response into array of JSON Strings
			let jsonStringArray = dockerFunciton.trim().split('\n');
			// Convert each JSON string into a JavaScript object
			jsonArray = jsonStringArray?.map(jsonString => JSON.parse(jsonString));
		}
		rawJson = jsonArray;
	}

	// Parse the JSON
	let containers = []
	for (const rawInfo of rawJson) {
		containers.push({
			// First twelve digits of the looongg ID are also okay
			id: rawInfo.ID.slice(0, 12),
			// The name of the service (rg, kc, db, es, etc)
			name: rawInfo.Service,
			// The name of the registry
			registry: rawInfo.Project,
			// Container status
			status: rawInfo.Health?.includes('starting') ? 'starting' : rawInfo.State,
			// Ports exposed on localhost
			ports: [
				// Deduplicate the ports
				...new Set(
					// Get all the ports
					rawInfo.Publishers?.map((portInfo: { PublishedPort: number }) =>
						portInfo.PublishedPort ? portInfo.PublishedPort : undefined
					).filter((port?: number) => !!port) // No port should be '0' or NaN
				),
			] as number[], // TSC can't infer this
		})
	}

	// All done!
	events.emit('registry.status', {
		status: 'success',
		operation: 'get-registry-status',
		message: 'Successfully retrieved registry status',
	})
	return containers
}

export const allUp = async (): Promise<boolean> => {
	// List the containers
	
	let rawJson = [];

	try {
		let dockerFunciton = await system.run('docker compose ps --format json')
		rawJson = JSON.parse(dockerFunciton);
	} catch (error) {
		let dockerFunciton = await system.run('docker compose ps --format json')
		let jsonArray = []
		if (dockerFunciton) {
			// Convert the string response into array of JSON Strings
			let jsonStringArray = dockerFunciton.trim().split('\n');
			// Convert each JSON string into a JavaScript object
			jsonArray = jsonStringArray?.map(jsonString => JSON.parse(jsonString));
		}
		rawJson = jsonArray;
	}

	for (let rawInfo of rawJson) {
		if (rawInfo.State !== 'running') {
			return false
		}
	}

	return true
}
