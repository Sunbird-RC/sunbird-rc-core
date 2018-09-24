package io.opensaber.registry.middleware.util;

public class Constants {
	
	public static final String REQUEST_ATTRIBUTE_NAME = "dataObject";
	public static final String RESPONSE_ATTRIBUTE = "responseModel";
	public static final String ATTRIBUTE_NAME = "dataObject";
	public static final String REQUEST_ATTRIBUTE= "requestModel";
	public static final String RDF_OBJECT = "rdf";
	public static final String RDF_VALIDATION_OBJECT = "rdfValidationResult";
	public static final String METHOD_ORIGIN = "methodOrigin";
	public static final String TOKEN_OBJECT = "x-authenticated-user-token";
	public static final String SHEX_CREATE_PROPERTY_NAME = "validations.create.file";
	public static final String SHEX_UPDATE_PROPERTY_NAME = "validations.update.file";
	public static final String FIELD_CONFIG_SCEHEMA_FILE = "config.schema.file";
	public static final String SHAPE_NAME = "validations.entity.shape.name";
	public static final String SHAPE_TYPE = "validations.entity.shape.type";
	public static final String FEATURE_TOGGLING = "feature.toggling";
	public static final String RDF_VALIDATION_MAPPER_OBJECT = "rdfValidationMapper";
	public static final String REGISTRY_CONTEXT_BASE = "registry.base";
	public static final String PRIVACY_PROPERTY = "privateProperties";
	public static final String SIGNED_PROPERTY = "signedProperties";

	public static final String DATABASE_PROVIDER = "database.provider";
	public static final String NEO4J_DIRECTORY = "database.neo4j.database_directory";
	public static final String ORIENTDB_DIRECTORY = "orientdb.directory";

	public static final String TEST_ENVIRONMENT = "test";
	public static final String PROD_ENVIRONMENT = "prod";
	public static final String INTEGRATION_TEST_BASE_URL = "http://localhost:8080/";
	public static final String TARGET_NODE_IRI = "http://www.w3.org/ns/shacl#targetNode";
	public static final String XSD_PREFIX = "xsd";
	public static final String XSD_SCHEMA = "http://www.w3.org/2001/XMLSchema#";
	public static final String CONTEXT_KEYWORD = "@context";
	public static final String TYPE_KEYWORD = "@type";

	public static final String DUPLICATE_RECORD_MESSAGE = "Cannot insert duplicate record";
	public static final String FAILED_INSERTION_MESSAGE = "Failed to insert record";
	public static final String NO_ENTITY_AVAILABLE_MESSAGE = "No entity available";
	public static final String ENTITY_NOT_FOUND = "Entity does not exist";
	public static final String DELETE_UNSUPPORTED_OPERATION_ON_ENTITY = "Delete operation not supported";
	public static final String READ_ON_DELETE_ENTITY_NOT_SUPPORTED = "Read on deleted entity not supported";
	public static final String TOKEN_EXTRACTION_ERROR = "Unable to extract auth token";
	public static final String JSONLD_PARSE_ERROR = "Unable to parse JSON-LD";
	public static final String RDF_VALIDATION_ERROR = "Unable to validate RDF";
	public static final String SIGNATURE_VALIDATION_ERROR = "Unable to validate presence of signatures";
	public static final String RDF_VALIDATION_MAPPING_ERROR = "Unable to map validations";
	public static final String CUSTOM_EXCEPTION_ERROR = "Something went wrong!! Please try again later";
	public static final String ADD_UPDATE_MULTIPLE_ENTITIES_MESSAGE = "Cannot add/update/view more than one entity";
	public static final String AUDIT_IS_DISABLED = "Audit is disabled";
	public static final String VALIDATION_CONFIGURATION_MISSING = "Configuration for validation file is missing";
	public static final String SCHEMA_CONFIGURATION_MISSING = "Configuration for schema file is missing";
	public static final String ENTITY_TYPE_NOT_PROVIDED = "Entity type is not provided in the input";

	public static final String OPENSABER_REGISTRY_API_NAME = "opensaber-registry-api";
	public static final String SUNBIRD_ENCRYPTION_SERVICE_NAME = "sunbird.encryption.service";
    public static final String SUNBIRD_SIGNATURE_SERVICE_NAME = "sunbird.signature.service";
	public static final String OPENSABER_DATABASE_NAME = "opensaber.database";
	public static final String GRAPH_GLOBAL_CONFIG = "graph_global_config";
	public static final String PERSISTENT_GRAPH = "persisten_graph";
	public static final String STATUS_INACTIVE = "false";
	public static final String STATUS_ACTIVE = "true";
	public static final String STATUS_KEYWORD = "@status";
	public static final String AUDIT_KEYWORD = "@audit";
	public static final String CREATE_METHOD_ORIGIN = "create";
	public static final String READ_METHOD_ORIGIN = "read";
	public static final String UPDATE_METHOD_ORIGIN = "update";
	public static final String SEARCH_METHOD_ORIGIN = "search";
    public static final String FORWARD_SLASH = "/";

    // List of predicates introduced for digital signature.
    public static final String SIGNATURES = "signatures";
	public static final String SIGNATURE_OF = "signatureOf";
	public static final String SIGNATURE_FOR = "signatureFor";
    public static final String SIGN_CREATOR = "creator";
    public static final String SIGN_CREATED_TIMESTAMP = "created";
    public static final String SIGN_NONCE = "nonce";
    public static final String SIGN_TYPE = "type";
    public static final String SIGN_SIGNATURE_VALUE = "signatureValue";

	public enum GraphDatabaseProvider {
		NEO4J("NEO4J"),
		ORIENTDB("ORIENTDB"),
		SQLG("SQLG"),
		CASSANDRA("CASSANDRA"),
		TINKERGRAPH("TINKERGRAPH");

		private String name;

		private GraphDatabaseProvider(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
	
	public enum AuditProperties {
		createdAt, lastUpdatedAt, createdBy, lastUpdatedBy
	}
	
	public enum GraphParams {
		properties, userId, operationType, label, requestId, nodeId, removedRelations, addedRelations, ets, createdAt, transactionData, CREATE, UPDATE, DELETE
	}

}
