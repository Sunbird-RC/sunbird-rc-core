package io.opensaber.registry.middleware.util;

public class Constants {

	public static final String SIGN_ENTITY = "entity";
	public static final String SIGN_VALUE = "value";
	public static final String TOKEN_OBJECT = "x-authenticated-user-token";
	public static final String LD_JSON_MEDIA_TYPE = "application/ld+json";


	public static final String OPENSABER_REGISTRY_API_NAME = "opensaber-registry-api";
	public static final String SUNBIRD_ENCRYPTION_SERVICE_NAME = "sunbird.encryption.service";
	public static final String SUNBIRD_SIGNATURE_SERVICE_NAME = "sunbird.signature.service";
	public static final String OPENSABER_DATABASE_NAME = "opensaber.database";
	public static final String GRAPH_GLOBAL_CONFIG = "graph_global_config";
	public static final String PERSISTENT_GRAPH = "persisten_graph";

	// Internal properties
	public static final String STATUS_KEYWORD = "_status";
	public static final String STATUS_INACTIVE = "false";
	public static final String STATUS_ACTIVE = "true";
	public static final String AUDIT_KEYWORD = "_audit";
	public static final String ARRAY_NODE_KEYWORD = "_array_node";
	public static final String ARRAY_ITEM = "_item";
	public static final String INTERNAL_TYPE_KEYWORD = "_intType";
	public static final String ROOT_KEYWORD = "_osroot";
	
	//Audit Fields Constant
	public static final String ACTION="action";
	public static final String ID="id";
	public static final String START_DATE="startDate";
	public static final String END_DATE="endDate";
	public static final String LIMIT="limit";
	public static final String OFFSET="offset";

	// JSON LD specific
	public static final String CONTEXT_KEYWORD = "@context";
	public static final String TYPE_STR_JSON_LD = "@type";
	
	// Parent Vertex Properies
	public static final String INDEX_FIELDS = "indexFields";
	public static final String UNIQUE_INDEX_FIELDS = "uniqueIndexFields";


	// Configuration constants
	public static final String FIELD_CONFIG_SCEHEMA_FILE = "config.schema.file";
	public static final String DATABASE_PROVIDER = "database.provider";
	public static final String NEO4J_DIRECTORY = "database.neo4j.database_directory";
	public static final String ORIENTDB_DIRECTORY = "orientdb.directory";


	public static final String TEST_ENVIRONMENT = "test";
	public static final String INTEGRATION_TEST_BASE_URL = "http://localhost:8080/";

	// Error messages.
	public static final String CUSTOM_EXCEPTION_ERROR = "Something went wrong!! Please try again later";
	public static final String SCHEMA_CONFIGURATION_MISSING = "Configuration for schema file is missing";
	public static final String SIGN_ERROR_MESSAGE = "Unable to get signature for data";
	public static final String VERIFY_SIGN_ERROR_MESSAGE = "Unable to verify signature for data";
	public static final String KEY_RETRIEVE_ERROR_MESSAGE = "Unable to retrieve key";

	// List of predicates introduced for digital signature.
	public static final String SIGNATURES_STR = "signatures";
	public static final String SIGNATURE_FOR = "signatureFor";
	public static final String SIGN_CREATOR = "creator";
	public static final String SIGN_CREATED_TIMESTAMP = "created";
	public static final String SIGN_NONCE = "nonce";
	public static final String SIGN_SIGNATURE_VALUE = "signatureValue";

	// List of request endpoints for post calls to validate request id
	public static final String REGISTRY_ADD_ENDPOINT = "/add";
	public static final String REGISTRY_UPDATE_ENDPOINT = "/update";
	public static final String REGISTRY_READ_ENDPOINT = "/read";
	public static final String REGISTRY_SEARCH_ENDPOINT = "/search";
	public static final String SIGNATURE_SIGN_ENDPOINT = "/utils/sign";
	public static final String SIGNATURE_VERIFY_ENDPOINT = "/utils/verify";
	public static final String REGISTRY_AUDT_READ_ENDPOINT="/audit";
	
	//class path for json resources from _schemas folder
	public static final String RESOURCE_LOCATION = "classpath*:public/_schemas/*.json";

	//elastic search document type
	public static final String ES_DOC_TYPE = "_doc";

	public static final String AUDIT_ACTION_READ = "READ";
	public static final String AUDIT_ACTION_ADD = "ADD";
	public static final String AUDIT_ACTION_ADD_OP = "add";
	public static final String AUDIT_ACTION_UPDATE = "UPDATE";
	public static final String AUDIT_ACTION_UPDATE_OP = "update";
	public static final String AUDIT_ACTION_SEARCH_OP = "search";
	public static final String AUDIT_ACTION_SEARCH = "SEARCH";
	public static final String AUDIT_ACTION_DELETE = "DELETE";
	public static final String AUDIT_ACTION_DELETE_OP = "delete";
	public static final String AUDIT_ACTION_READ_OP = "read";
	public static final String AUDIT_ACTION_AUDIT = "AUDIT";
	public static final String AUDIT_ACTION_AUDIT_OP = "audit";

	public static final String ELASTIC_SEARCH_ACTOR = "ElasticSearchActor";
	public static final String AUDIT_ACTOR = "AuditActor";
	public static final String OS_ACTOR = "OSActor";
	
	//Audit Data Store Type
	public static final String FILE="FILE";
	public static final String DATABASE="DATABASE";

	public enum GraphDatabaseProvider {
		NEO4J("NEO4J"), ORIENTDB("ORIENTDB"), SQLG("SQLG"), CASSANDRA("CASSANDRA"), TINKERGRAPH("TINKERGRAPH");

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

	public enum Direction {	
		IN, OUT
	}
	
	public enum SchemaType {
	    JSON
	}

	public static class JsonldConstants {
		public static final String CONTEXT = "@context";
		public static final String ID = "@id";
		public static final String TYPE = "@type";
		public static final String VALUE = "@value";
		public static final String GRAPH = "@graph";
	}

}
