// @/toolbox/registry/helpers/keycloak
// Wrapper around keycloak API

import { http } from 'gluegun'

import { ApisauceInstance } from 'apisauce'
import { ApiResponse } from '../../../types'
import { config } from '../../../config/config'

// Utility methods
const convertToUrlEncodedForm = (data: Record<string, any>): string => {
	return Object.keys(data)
		.map((key: string) => `${key}=${encodeURIComponent(data[key])}`)
		.join('&')
}

// Wrapper around keycloak API calls
class KeycloakWrapper {
	httpClient: ApisauceInstance
	user: string
	pass: string
	realm: string

	constructor(options: { user: string; pass: string; realm: string }) {
		this.httpClient = http.create({
			baseURL: 'http://localhost:8080',
		})
		this.user = options.user
		this.pass = options.pass
		this.realm = options.realm
	}

	// Return an access token
	async getAccessToken(): Promise<string> {
		let maxRetries = config.maximumRetries
		let retryCount = 0
		while (retryCount < maxRetries) {
			try {
				const response = (await this.httpClient.post(
					'/auth/realms/master/protocol/openid-connect/token',
					convertToUrlEncodedForm({
						client_id: 'admin-cli',
						username: this.user,
						password: this.pass,
						grant_type: 'password',
					}),
					{
						headers: {
							'content-type': 'application/x-www-form-urlencoded',
						},
					}
				)) as ApiResponse

				if (response.ok) {
					return response.data.access_token
				} else {
					if (retryCount === maxRetries - 1)
						console.debug(response.originalError)
					throw new Error(
						`There was an error while retrieving an access token from Keycloak: ${
							response.originalError ?? response.problem
						}`
					)
				}
			} catch (error) {
				// console.error(`API call failed. Retrying... (${retryCount + 1}/${maxRetries})`);
				retryCount++
				// You can adjust the delay time as needed
				await new Promise((resolve) => setTimeout(resolve, 1000)) // 1 second delay
			}
		}

		throw new Error(
			`API call failed to fetch token from keycloak after ${maxRetries} retries.`
		)
	}

	// Get the keycloak client ID of a client
	async getInternalClientId(clientId: string): Promise<string> {
		const response = (await this.httpClient.get(
			`/auth/admin/realms/${this.realm}/clients`,
			{ clientId },
			{
				headers: {
					authorization: `Bearer ${await this.getAccessToken()}`,
				},
			}
		)) as ApiResponse
		if (!response.ok) {
			console.debug(response.originalError)
			throw new Error(
				`There was an error while retrieving the internal client ID from keycloak: ${
					response.originalError ?? response.problem
				}`
			)
		}
		if (!response.data[0]?.id) {
			throw new Error(`Could not find a client with ID ${clientId} in keycloak`)
		}

		return response.data[0].id
	}

	// Regenerate the client secret for a client in keycloak
	async regenerateClientSecret(internalClientId: string): Promise<string> {
		let token = await this.getAccessToken()
		const response = (await this.httpClient.post(
			`/auth/admin/realms/${this.realm}/clients/${internalClientId}/client-secret`,
			{},
			{
				headers: {
					authorization: `Bearer ${token}`,
				},
			}
		)) as ApiResponse
		if (!response.ok) {
			console.debug(response.originalError)
			throw new Error(
				`There was an error while regenerating the client secret for a client in keycloak: ${
					response.originalError ?? response.problem
				}`
			)
		}

		return response.data.value
	}

	// Add a client scope in keycloak
	async createClientScope(scope: {
		name: string
		description: string
		protocol: 'openid-connect'
		mapper: {
			type: string
			attribute: {
				path: string
				type: 'String' | 'int' | 'long' | 'boolean' | 'JSON'
			}
		}
	}): Promise<string> {
		let createScopeResponse = (await this.httpClient.post(
			`/auth/admin/realms/${this.realm}/client-scopes`,
			{
				name: scope.name,
				description: scope.description,
				protocol: scope.protocol,
				protocolMappers: [
					{
						name: scope.name,
						protocol: scope.protocol,
						protocolMapper: scope.mapper.type,
						consentRequired: true,
						config: {
							'userinfo.token.claim': true,
							'user.attribute': scope.mapper.attribute.path,
							'id.token.claim': true,
							'access.token.claim': true,
							'claim.name': scope.mapper.attribute.path,
							'jsonType.label': scope.mapper.attribute.type,
						},
					},
				],
			},
			{
				headers: {
					authorization: `Bearer ${await this.getAccessToken()}`,
				},
			}
		)) as ApiResponse
		if (!createScopeResponse.ok) {
			console.debug(createScopeResponse.originalError)
			throw new Error(
				`There was an error while creating a client scope in keycloak: ${
					createScopeResponse.originalError ?? createScopeResponse.problem
				}`
			)
		}

		const getScopeResponse = (await this.httpClient.get(
			`/auth/admin/realms/${this.realm}/client-scopes`,
			{},
			{
				headers: {
					authorization: `Bearer ${await this.getAccessToken()}`,
				},
			}
		)) as ApiResponse
		if (!getScopeResponse.ok) {
			console.debug(getScopeResponse.originalError)
			throw new Error(
				`There was an error while regenerating the client secret for a client in keycloak: ${
					getScopeResponse.originalError ?? getScopeResponse.problem
				}`
			)
		}

		const clientScope = getScopeResponse.data.filter(
			(scopeInfo: { name: string }) => scopeInfo.name === scope.name
		)[0]

		if (!clientScope) {
			throw new Error(
				`Could not find a client scope with name ${scope.name} in keycloak`
			)
		}

		return clientScope.id
	}

	// Make a client scope an optional scope for a client
	async addOptionalClientScope(
		internalClientId: string,
		internalScopeId: string
	): Promise<void> {
		const response = (await this.httpClient.put(
			`/auth/admin/realms/${this.realm}/clients/${internalClientId}/optional-client-scopes/${internalScopeId}`,
			{},
			{
				headers: {
					authorization: `Bearer ${await this.getAccessToken()}`,
				},
			}
		)) as ApiResponse
		if (!response.ok) {
			console.debug(response.originalError)
			throw new Error(
				`There was an error while making client scope optional for a client in keycloak: ${
					response.originalError ?? response.problem
				}`
			)
		}
	}
}

export default KeycloakWrapper
