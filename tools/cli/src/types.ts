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
	create(registryConfig: RegistryConfig): Promise<void>
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

export interface ApiResponse {
	data: any
	status: number
	ok: boolean
	problem?:
		| 'CLIENT_ERROR'
		| 'SERVER_ERROR'
		| 'TIMEOUT_ERROR'
		| 'CONNECTION_ERROR'
		| 'NETWORK_ERROR'
		| 'CANCEL_ERROR'
	originalError?: Error
}

export interface RegistryConfig {
	registryName: string
	realmName: string
	keycloakAdminClientId: string
	keycloakFrontendClientId: string
	keycloakAdminUser: string
	keycloakAdminPass: string
	pathToEntitySchemas: 'use-example-config' | string
	pathToConsentConfiguration: 'use-example-config' | string
}
