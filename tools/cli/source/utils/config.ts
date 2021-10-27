// Configuration for the application

import Fs from 'fs/promises'
import Conf from 'conf'

// Default configuration
const defaults = {
	setup: {
		repo: 'https://github.com/gamemaker1/registry-setup-files',
	},
	keycloak: {
		uri: 'http://kc:8080',
		user: 'admin',
		pass: 'admin',
		realm: 'sunbird-rc',
		'client-secret-var': 'sunbird_sso_admin_client_secret',
		'admin-client-id': 'admin-api',
	},
	containers: {
		names: ['rg', 'es', 'db', 'kc'],
		images: [
			'dockerhub/sunbird-rc-core',
			'docker.elastic.co/elasticsearch/elasticsearch:7.10.1',
			'postgres',
			'dockerhub/ndear-keycloak',
		],
	},
}

// Export the Conf instance
const ConfigManager = new Conf({
	projectName: 'registry-cli',
	projectSuffix: '',
	schema: {
		setup: {
			type: 'object',
			properties: {
				repo: {
					type: 'string',
				},
			},
		},
		keycloak: {
			type: 'object',
			properties: {
				uri: {
					type: 'string',
					default: 'http://kc:8080',
				},
				user: {
					type: 'string',
					default: 'admin',
				},
				pass: {
					type: 'string',
					default: 'admin',
				},
				realm: {
					type: 'string',
					default: 'sunbird-rc',
				},
				'client-secret-var': {
					type: 'string',
					default: 'sunbird_sso_admin_client_secret',
				},
			},
		},
		containers: {
			type: 'object',
			properties: {
				names: {
					type: 'array',
					default: ['rg', 'es', 'db', 'kc'],
				},
				images: {
					type: 'array',
					default: [
						'dockerhub/sunbird-rc-core',
						'docker.elastic.co/elasticsearch/elasticsearch:7.10.1',
						'postgres',
						'dockerhub/ndear-keycloak',
					],
				},
			},
		},
	},
	defaults,
})

// If the config file is empty, write the defaults to it
const configFileContents = await Fs.readFile(ConfigManager.path).catch(() => {
	// Do nothing, instead create and write to the file
})
if (!configFileContents) {
	await Fs.writeFile(ConfigManager.path, JSON.stringify(defaults, null, 4))
}

// Export the Conf instance
export default ConfigManager
