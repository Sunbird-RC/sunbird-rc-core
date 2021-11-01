// @/toolbox/registry/helpers/keycloak
// Wrapper around keycloak API

import { http } from 'gluegun'

// @ts-expect-error: not a direct dependency
import { ApisauceInstance } from 'apisauce'

// Utility methods
const convertToUrlEncodedForm = (data: Record<string, any>): string => {
	return Object.keys(data)
		.map((key: string) => `${key}=${encodeURIComponent(data[key])}`)
		.join('&')
}

class KeycloakWrapper {
	httpClient: ApisauceInstance

	constructor() {
		this.httpClient = http.create({
			baseURL: 'http://localhost:8080',
		})
	}

	// Return an access token
	async getAccessToken(): Promise<string> {
		// Fetch an access token
		const response = (await this.httpClient.post(
			'/auth/realms/master/protocol/openid-connect/token',
			convertToUrlEncodedForm({
				client_id: 'admin-cli',
				username: 'admin',
				password: 'admin',
				grant_type: 'password',
			}),
			{
				headers: {
					'content-type': 'application/x-www-form-urlencoded',
				},
			}
		)) as {
			data: {
				access_token: string
				expires_in: number
			}
			originalError: Error
		}
		if (response.originalError) {
			console.log(response.originalError)
			throw response.originalError
		}

		return response.data.access_token
	}

	// Get the keycloak client ID of a client
	async getInternalClientId(clientId: string): Promise<string> {
		const response = (await this.httpClient.get(
			'/auth/admin/realms/sunbird-rc/clients',
			{ clientId },
			{
				headers: {
					authorization: `Bearer ${await this.getAccessToken()}`,
				},
			}
		)) as {
			data: {
				id: string
			}[]
			originalError?: Error
		}
		if (response.originalError) {
			console.log(response.originalError)
			throw response.originalError
		}
		if (!response.data[0].id) {
			throw new Error('Could not find that client in keycloak')
		}

		return response.data[0].id
	}

	async regenerateClientSecret(internalClientId: string): Promise<string> {
		const response = (await this.httpClient.post(
			`/auth/admin/realms/sunbird-rc/clients/${internalClientId}/client-secret`,
			{},
			{
				headers: {
					authorization: `Bearer ${await this.getAccessToken()}`,
				},
			}
		)) as {
			data: {
				value: string
			}
			originalError?: Error
		}
		if (response.originalError) {
			console.log(response.originalError)
			throw response.originalError
		}

		return response.data.value
	}

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
			'/auth/admin/realms/sunbird-rc/client-scopes',
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
		)) as { originalError?: Error }
		if (createScopeResponse.originalError)
			throw createScopeResponse.originalError

		const getScopeResponse = (await this.httpClient.get(
			'/auth/admin/realms/sunbird-rc/client-scopes',
			{},
			{
				headers: {
					authorization: `Bearer ${await this.getAccessToken()}`,
				},
			}
		)) as {
			data: {
				id: string
				name: string
			}[]
			originalError?: Error
		}
		if (getScopeResponse.originalError) throw getScopeResponse.originalError

		const clientScope = getScopeResponse.data.filter(
			(scopeInfo) => scopeInfo.name === scope.name
		)[0]

		if (!clientScope) {
			throw new Error('Could not find that client scope')
		}

		return clientScope.id
	}

	async addOptionalClientScope(
		internalClientId: string,
		internalScopeId: string
	): Promise<void> {
		const response = (await this.httpClient.put(
			`/auth/admin/realms/sunbird-rc/clients/${internalClientId}/optional-client-scopes/${internalScopeId}`,
			{},
			{
				headers: {
					authorization: `Bearer ${await this.getAccessToken()}`,
				},
			}
		)) as { originalError?: Error }
		if (response.originalError) {
			console.log(response.originalError)
			throw response.originalError
		}
	}
}

export default new KeycloakWrapper()
