// @/types
// Export necessary types for the CLI

import EventEmitter from 'events'

import { GluegunToolbox } from 'gluegun'

export interface RegistryContainer {
	id: string
	name: string
	registry: string
	status: 'running' | 'stopped' | 'starting'
	ports: number[]
}

export interface Environment {
	check(registryPath?: string): Promise<boolean>
}
export interface Registry {
	create(): void
	status(): Promise<RegistryContainer[]>
}

export interface Toolbox extends GluegunToolbox {
	events: EventEmitter
	environment: Environment
	registry: Registry
}

export interface CLIEvent {
	status: 'progress' | 'success' | 'error'
	operation: string
	message: string
}
