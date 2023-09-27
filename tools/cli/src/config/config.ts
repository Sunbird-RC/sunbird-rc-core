export interface Config {
	DEFAULT_ES_PROVIDER_NAME: string
	DEFAULT_NS_PROVIDER_NAME: string
	docker_service_name: {
		ES: string
		DB: string
		REGISTRY: string
		KEYCLOAK: string
		FILE_STORAGE: string
		CLAIMS_MS: string
		CERTIFICATE_SIGHNER: string
		PUBLIC_KEY_SERVICE: string
		CERTIFICATE_API: string
		NOTIFICATION_MS: string
		ZOOKEEPER: string
		KAFKA: string
		CONTEXT_PROXY_SERVICE: string
		NGINIX: string
		METRICS: string
		CLICKHOUSE: string
		REDIS: string
		DIGI_LOCKER: string
		BULK_ISSUANCE: string
	}
	auxiliary_services: AuxiliaryServices
	definationMangerTypes: DefinationsManager
	maximumRetries: number
}

interface AuxiliaryServices {
	[serviceName: string]: string
}

interface DefinationsManager {
	[name: string]: string
}

const auxiliary_services: AuxiliaryServices = {
	'Notifications Service': 'notification-ms',
	'Context Proxy Service': 'context-proxy-service',
	Nginix: 'nginx',
	'Metrics Service': 'kafka clickhouse metrics',
	'Bulk Issuance Service': 'bulk_issuance',
	'Digilocker Certificate Service': 'digilocker-certificate-api',
}

const definationsManagers = {
	'Definitions Manager': 'DefinitionsManager',
	'Distributed Definitions Manager': 'DistributedDefinitionsManager',
}

export let config: Config = {
	DEFAULT_ES_PROVIDER_NAME:
		'dev.sunbirdrc.registry.service.ElasticSearchService',
	DEFAULT_NS_PROVIDER_NAME:
		'dev.sunbirdrc.registry.service.NativeSearchService',
	docker_service_name: {
		ES: 'es',
		DB: 'db',
		REGISTRY: 'registry',
		KEYCLOAK: 'keycloak',
		FILE_STORAGE: 'file-storage',
		CLAIMS_MS: 'claim-ms',
		CERTIFICATE_SIGHNER: 'certificate-signer',
		PUBLIC_KEY_SERVICE: 'public-key-service',
		CERTIFICATE_API: 'certificate-api',
		NOTIFICATION_MS: 'notification-ms',
		ZOOKEEPER: 'zookeeper',
		KAFKA: 'kafka',
		CONTEXT_PROXY_SERVICE: 'context-proxy-service',
		NGINIX: 'nginx',
		METRICS: 'metrics',
		CLICKHOUSE: 'clickhouse',
		REDIS: 'redis',
		DIGI_LOCKER: 'digilocker-certificate-api',
		BULK_ISSUANCE: 'bulk_issuance',
	},
	auxiliary_services: auxiliary_services,
	definationMangerTypes: definationsManagers,
	maximumRetries: 100,
}
