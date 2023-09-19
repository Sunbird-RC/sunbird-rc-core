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
	create(registryConfig: RegistrySetupOptions): Promise<void>
	status(): Promise<RegistryContainer[]>
	restart(soft: boolean): Promise<void>
	config(): Promise<RegistryConfiguration>
	down(): Promise<void>
	health(): Promise<RegistryHealth[]>
}

export interface Toolbox extends GluegunToolbox {
	events: EventEmitter
	environment: Environment
	registry: Registry
	handleEvent: (event: CLIEvent) => void
	until: (conditionFunction: () => Promise<boolean>) => Promise<any>
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

export interface RegistryConfiguration {
	type: 'sunbird-rc'
	name: string
}

export interface RegistrySetupOptions {
	registryName: string
	realmName: string
	keycloakAdminClientId: string
	keycloakFrontendClientId: string
	keycloakAdminUser: string
	keycloakAdminPass: string
	pathToEntitySchemas: 'use-example-config' | string
	pathToConsentConfiguration: 'use-example-config' | string
	enableRegistryAuthentication: boolean
	elasticSearchEnabled: boolean
	enableVCIssuance: boolean
	asyncEnabled: boolean
	managerType: string
	searchProvideName:
		| 'dev.sunbirdrc.registry.service.NativeSearchService'
		| string
	auxiliaryServicesToBeEnabled: Array<string> | Array<any>
	autoGenerateKeys: boolean
	signatureEnabled: boolean
	eventEnabled: boolean
	enableAttestation: boolean
	fileStorageEnabled: boolean
}

export interface RegistryTearDownOptions {
	stopConfirmation: string
}

export interface RegistryHealthResponse {
	id: string
	ver: string
	ets: number
	params: Params
	responseCode: string
	result: Result
}

interface Params {
	resmsgid: string
	msgid: string
	err: string
	status: string
	errmsg: string
}

interface Result {
	name: string
	healthy: boolean
	checks: Check[]
}

interface Check {
	name: string
	healthy: boolean
	err: string
	errmsg: string
}

export interface RegistryHealth {
	name: string
	status: 'UP' | 'DOWN'
	err: string
}

export interface SignatureOptions {
	signatureEnabled: boolean
}

export interface GitRawJson {
	ID: string
	Name: string
	Image: string
	Command: string
	Project: string
	Service: string
	Created: number
	State: string
	Status: string
	Health: string
	ExitCode: number
	Publishers: any[] // You can specify a more specific type if needed
}
