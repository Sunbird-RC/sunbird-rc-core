package io.opensaber.registry.middleware.util;

public class Constants {
	
	public static final String REQUEST_ATTRIBUTE_NAME = "dataObject";
	public static final String RESPONSE_ATTRIBUTE_NAME = "dataObject";
	public static final String ATTRIBUTE_NAME = "dataObject";
	public static final String REQUEST_ATTRIBUTE= "requestModel";
	public static final String RDF_OBJECT = "rdf";
	public static final String RDF_VALIDATION_OBJECT = "rdfValidationResult";
	public static final String SHEX_PROPERTY_NAME = "shex.file";
	public static final String SUBJECT_LABEL_TYPE = "subject.label.type";
	public static final String SHAPE_NAME = "shape.name";
	public static final String SHAPE_TYPE = "shape.type";
	public static final String RDF_VALIDATION_MAPPER_OBJECT = "rdfValidationMapper";
	public static final String REGISTRY_CONTEXT_BASE = "registry.context.base";

	public static final String DATABASE_PROVIDER = "database.provider";
	public static final String NEO4J_DIRECTORY = "neo4j.directory";
	public static final String ORIENTDB_DIRECTORY = "orientdb.directory";

	public static final String TEST_ENVIRONMENT = "test";
	public static final String PROD_ENVIRONMENT = "prod";
	public static final String BASE_URL = "base.url";
	public static final String INTEGRATION_TEST_BASE_URL = "http://localhost:8080/";
	public static final String TARGET_NODE_IRI = "http://www.w3.org/ns/shacl#targetNode";
	
	public static final String DUPLICATE_RECORD_MESSAGE = "Cannot insert duplicate record";
	public static final String FAILED_INSERTION_MESSAGE = "Failed to insert record";
	public static final String INVALID_TYPE_MESSAGE = "Failed to insert due to invalid type";
	
	public static final String ENTITY_NOT_FOUND = "Entity does not exist";
	public static final String JSONLD_PARSE_ERROR = "Unable to parse JSON-LD";
	public static final String RDF_VALIDATION_ERROR = "Unable to validate RDF";

	public enum GraphDatabaseProvider {
		NEO4J("NEO4J"),
		ORIENTDB("ORIENTDB"),
		SQLG("SQLG"),
		TINKERGRAPH("TINKERGRAPH");

		private String name;

		private GraphDatabaseProvider(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
	
	public enum ValidationMapper {
		SCHOOL("school."),
		ADDRESS("address."),
		TEACHER("teacher."),
		TEACHER_ROLE("teacher.role.");
		
		private String name;

		private ValidationMapper(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
		
	}

}
