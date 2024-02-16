// @/toolbox/registry/health
// View registry health

import {
	ApiResponse,
	RegistryContainer,
	RegistryHealth,
	RegistryHealthResponse,
	Toolbox,
} from '../../types'

// Accept a toolbox, return a registry health viewer
export default async (toolbox: Toolbox): Promise<RegistryHealth[]> => {
	const { events, system, print, http } = toolbox

	events.emit('registry.health', {
		status: 'progress',
		operation: 'get-registry-health',
		message: 'Checking on registry',
	})
	const registryApi = http.create({
		baseURL: 'http://localhost:8081',
		headers: { Accept: 'application/json' },
	})
	let ok: true | false, data: unknown
	;({ ok, data } = await registryApi.get('/health'))
	const response = data as RegistryHealthResponse
	// List containers
	print.info(ok)
	const health: RegistryHealth[] = response.result.checks.map((check) => {
		return {
			name: check.name,
			status: check.healthy && check.err.length === 0 ? 'UP' : 'DOWN',
			err:
				check.healthy && check.err.length === 0
					? ''
					: check.err + ' : ' + check.errmsg,
		}
	})

	// All done!
	events.emit('registry.health', {
		status: 'success',
		operation: 'get-registry-health',
		message: 'Successfully retrieved registry health',
	})
	return health
}

// A helper function to check if all the containers are up and running
import { system } from 'gluegun'
import { ApiOkResponse } from 'apisauce'
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
	for (const rawInfo of rawJson) {
		if (rawInfo.State !== 'running') {
			return false
		}
	}

	return true
}
