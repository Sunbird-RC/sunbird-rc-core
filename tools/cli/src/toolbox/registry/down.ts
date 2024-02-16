// @/toolbox/registry/down
// Stop registry instance in the current directory

import { allUp } from './status'

import { Toolbox } from '../../types'
import { system } from 'gluegun'
import path from 'path'

// Accept a toolbox and stop the registry instance in return
export default async (toolbox: Toolbox) => {
	const { events, system, until, filesystem } = toolbox

	events.emit('registry.down', {
		status: 'progress',
		operation: 'stopping-containers',
		message: 'Stopping and removing docker instances',
	})

	await system.exec('docker compose down').catch((error: Error) => {
		events.emit('registry.down', {
			status: 'error',
			operation: 'stopping-containers',
			message: `An unexpected error occurred while stopping the registry: ${error.message} ${error.stack}`,
		})
	})
	// Wait for the containers to stop
	await until(allDown)
	events.emit('registry.down', {
		status: 'progress',
		operation: 'remove-file',
		message: 'Removing init files',
	})
	filesystem.remove(path.resolve(process.cwd(), 'registry.yaml'))
	events.emit('registry.down', {
		status: 'success',
		operation: 'remove-file',
		message: 'Removing init files',
	})
	events.emit('registry.down', {
		status: 'success',
		operation: 'stopping-containers',
		message: 'Successfully stopped all services!',
	})
}

const allDown = async (): Promise<boolean> => {
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

	for (const rawInfo of rawJson) {
		if (rawInfo.State !== 'exited') {
			return false
		}
	}

	return true
}
