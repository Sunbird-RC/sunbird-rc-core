// Helper methods to interact with docker

import Docker from 'dockerode'
import Config from '../utils/config'

// Create a docker instance
const docker = new Docker()

// Type representing a container
type Container = {
	// Name of the container
	name: string
	// 12 character ID of the container
	id: string
	// Image that is running inside the container
	image: string
	// Current state of the container
	status: string
	// Exposed ports (on the system)
	ports: number[]
}

// Method to list and return registry-related containers
export const listContainers = async (
	config?: Record<string, unknown>
): Promise<Container[]> => {
	return docker
		.listContainers(config)
		.then((containers) => {
			// Convert the returned object to our Container type
			return containers.map((container) => {
				return {
					name: container.Names[0],
					id: container.Id.slice(0, 12),
					image: container.Image,
					status: container.Status.toLowerCase().includes('starting')
						? 'starting'
						: container.State,
					ports: [
						...new Set(
							container.Ports.map((port) => port.PublicPort).filter(
								(port) => !!port
							)
						),
					],
				}
			})
		})
		.then((containers) => {
			// Filter out those that are not registry-related
			return containers.filter((container) =>
				(Config.get('containers.images') as string[]).some((image) =>
					container.image.includes(image)
				)
			)
		})
		.then((containers) => {
			// Check if there are any registry-related containers
			if (containers?.length <= 0) {
				throw new Error('Could not find any registry related containers')
			}

			return containers
		})
}

// Methods to restart, start and stop a container
export const restartContainer = (id: string): Promise<void> => {
	return docker.getContainer(id).restart()
}
export const startContainer = (id: string): Promise<void> => {
	return docker.getContainer(id).start()
}
export const stopContainer = (id: string): Promise<void> => {
	return docker.getContainer(id).stop()
}
