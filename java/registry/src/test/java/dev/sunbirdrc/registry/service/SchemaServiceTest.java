package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.entities.SchemaStatus;
import dev.sunbirdrc.registry.exception.SchemaException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import dev.sunbirdrc.validators.IValidate;
import org.apache.commons.io.IOUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static dev.sunbirdrc.registry.Constants.Schema;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class SchemaServiceTest {
	private static final String TRAINING_CERTIFICATE = "TrainingCertificate";
	private final ObjectMapper objectMapper = new ObjectMapper();
	DefinitionsManager definitionsManager = new DefinitionsManager();


	@Mock
	IValidate validator;
	@InjectMocks
	SchemaService schemaService;

	String trainingCertificateSchema;

	@Before
	public void setup() throws IOException {
		definitionsManager = new DefinitionsManager();
		Map<String, Definition> definitionMap = new HashMap<>();
		trainingCertificateSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("TrainingCertificate.json"), Charset.defaultCharset());
		definitionMap.put(TRAINING_CERTIFICATE, new Definition(objectMapper.readTree(trainingCertificateSchema)));
		ReflectionTestUtils.setField(definitionsManager, "definitionMap", definitionMap);
		ReflectionTestUtils.setField(definitionsManager, "objectMapper", objectMapper);
		ReflectionTestUtils.setField(schemaService, "definitionsManager", definitionsManager);
	}

	@Test
	public void shouldDeleteSchemaFromDefinitionManger() throws SchemaException {
		assertEquals(1, definitionsManager.getAllKnownDefinitions().size());
		Vertex vertex = mock(Vertex.class);
		VertexProperty vertexProperty = mock(VertexProperty.class);
		Mockito.when(vertex.property(Schema.toLowerCase())).thenReturn(vertexProperty);
		Mockito.when(vertexProperty.value()).thenReturn(trainingCertificateSchema);
		schemaService.deleteSchemaIfExists(vertex);
		assertEquals(0, definitionsManager.getAllKnownDefinitions().size());
	}

	@Test
	public void shouldNotDeleteSchemaFromDefinitionManger() throws IOException, SchemaException {
		assertEquals(1, definitionsManager.getAllKnownDefinitions().size());
		Vertex vertex = mock(Vertex.class);
		VertexProperty vertexProperty = mock(VertexProperty.class);
		Mockito.when(vertex.property(Schema.toLowerCase())).thenReturn(vertexProperty);
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		Mockito.when(vertexProperty.value()).thenReturn(schema);
		schemaService.deleteSchemaIfExists(vertex);
		assertEquals(1, definitionsManager.getAllKnownDefinitions().size());
	}

	@Test(expected = SchemaException.class)
	public void shouldNotDeleteSchemasIfPublished() throws IOException, SchemaException {
		Vertex vertex = mock(Vertex.class);
		VertexProperty schemaProperty = mock(VertexProperty.class);
		Mockito.when(schemaProperty.value()).thenReturn(SchemaStatus.PUBLISHED.toString());
		Mockito.when(vertex.property("status")).thenReturn(schemaProperty);
		schemaService.deleteSchemaIfExists(vertex);
	}

	@Test
	public void shouldDeleteSchemasIfPublished() throws IOException, SchemaException {
		Vertex vertex = mock(Vertex.class);
		VertexProperty vertexProperty = mock(VertexProperty.class);
		VertexProperty schemaProperty = mock(VertexProperty.class);
		Mockito.when(schemaProperty.value()).thenReturn(SchemaStatus.DRAFT.toString());
		Mockito.when(vertex.property("status")).thenReturn(schemaProperty);
		Mockito.when(vertex.property(Schema.toLowerCase())).thenReturn(vertexProperty);
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		Mockito.when(vertexProperty.value()).thenReturn(schema);
		schemaService.deleteSchemaIfExists(vertex);
	}

	@Test
	public void shouldAddSchemaToDefinitionManager() throws IOException {
		assertEquals(1, definitionsManager.getAllKnownDefinitions().size());
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
		ObjectNode object = JsonNodeFactory.instance.objectNode();
		object.put(Schema.toLowerCase(), schema);
		object.put("status", SchemaStatus.PUBLISHED.toString());
		schemaNode.set(Schema, object);
		schemaService.addSchema(schemaNode);
		assertEquals(2, definitionsManager.getAllKnownDefinitions().size());
	}

	@Test
	public void shouldAddSchemaToDefinitionManagerAndAddEntityToElasticSearch() throws IOException {
		ReflectionTestUtils.setField(schemaService, "isElasticSearchEnabled", true);
		assertEquals(1, definitionsManager.getAllKnownDefinitions().size());
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
		ObjectNode object = JsonNodeFactory.instance.objectNode();
		object.put(Schema.toLowerCase(), schema);
		object.put("status", SchemaStatus.PUBLISHED.toString());
		schemaNode.set(Schema, object);
		schemaService.addSchema(schemaNode);
		assertEquals(2, definitionsManager.getAllKnownDefinitions().size());
	}


	@Test
	public void shouldAddStatusToNewSchemasIfNotPresent() throws IOException {
		assertEquals(1, definitionsManager.getAllKnownDefinitions().size());
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
		ObjectNode object = JsonNodeFactory.instance.objectNode();
		object.put(Schema.toLowerCase(), schema);
		schemaNode.set(Schema, object);
		assertNull(schemaNode.get("status"));
		schemaService.addSchema(schemaNode);
		assertNotNull(schemaNode.get(Schema).get("status"));
		assertEquals(1, definitionsManager.getAllKnownDefinitions().size());
	}

	@Test
	public void shouldAddSchemaToDefinitionManagerOnlyForPublishedStatus() throws IOException {
		assertEquals(1, definitionsManager.getAllKnownDefinitions().size());
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
		ObjectNode object = JsonNodeFactory.instance.objectNode();
		object.put(Schema.toLowerCase(), schema);
		object.put("status", SchemaStatus.PUBLISHED.toString());
		schemaNode.set(Schema, object);
		schemaService.addSchema(schemaNode);
		assertEquals(2, definitionsManager.getAllKnownDefinitions().size());
	}

	@Test
	public void shouldNotAddSchemaToDefinitionManagerForDraftStatus() throws IOException {
		assertEquals(1, definitionsManager.getAllKnownDefinitions().size());
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
		ObjectNode object = JsonNodeFactory.instance.objectNode();
		object.put(Schema.toLowerCase(), schema);
		schemaNode.set(Schema, object);
		schemaService.addSchema(schemaNode);
		assertEquals(1, definitionsManager.getAllKnownDefinitions().size());
	}

	@Test
	public void shouldUpdateSchemaIfStatusIsNotUpdated() throws IOException, SchemaException {
		JsonNode existingDefinition = objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());
		assertEquals(1, existingDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("contact").size());
		assertNull(existingDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("mobile"));
		JsonNode existingSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/TrainingCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"TrainingCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"TrainingCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/TrainingCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The TrainingCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"contact\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"trainingTitle\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"contact\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date\\\": {\\n          \\\"type\\\": \\\"string\\\",\\n          \\\"format\\\": \\\"date\\\"\\n        },\\n        \\\"note\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"contact\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        \\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"trainedOn\\\": \\\"{{trainingTitle}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\\\",\\n      \\\"svg\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\\\"\\n    }\\n  }\\n}\",\n" +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema24\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ]\n" +
				"  }\n" +
				"}");
		JsonNode updatedSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/TrainingCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"TrainingCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"TrainingCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/TrainingCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The TrainingCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"contact\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"trainingTitle\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"mobile\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date\\\": {\\n          \\\"type\\\": \\\"string\\\",\\n          \\\"format\\\": \\\"date\\\"\\n        },\\n        \\\"note\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"contact\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        \\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"trainedOn\\\": \\\"{{trainingTitle}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\\\",\\n      \\\"svg\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\\\"\\n    }\\n  }\\n}\",\n" +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema_new\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ]\n" +
				"  }\n" +
				"}");
		schemaService.updateSchema(existingSchema, updatedSchema);
		JsonNode updatedDefinition = objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());
		assertNull(updatedDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("contact"));
		assertEquals(1, updatedDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("mobile").size());
	}

	@Test(expected = SchemaException.class)
	public void shouldNotUpdateSchemaIfSchemaDefinitionUpdated() throws SchemaException, IOException {
		JsonNode existingDefinition = objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());
		assertEquals(1, existingDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("contact").size());
		assertNull(existingDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("mobile"));
		JsonNode existingSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/TrainingCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"TrainingCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"TrainingCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/TrainingCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The TrainingCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"contact\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"trainingTitle\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"contact\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date\\\": {\\n          \\\"type\\\": \\\"string\\\",\\n          \\\"format\\\": \\\"date\\\"\\n        },\\n        \\\"note\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"contact\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        \\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"trainedOn\\\": \\\"{{trainingTitle}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\\\",\\n      \\\"svg\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\\\"\\n    }\\n  }\\n}\",\n" +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema_new\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ],\n" +
				"    \"status\": \"PUBLISHED\"\n" +
				"  }\n" +
				"}");
		JsonNode updatedSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/TrainingCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"TrainingCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"TrainingCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/TrainingCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The TrainingCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"contact\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"trainingTitle\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"mobile\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date\\\": {\\n          \\\"type\\\": \\\"string\\\",\\n          \\\"format\\\": \\\"date\\\"\\n        },\\n        \\\"note\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"contact\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        \\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"trainedOn\\\": \\\"{{trainingTitle}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\\\",\\n      \\\"svg\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\\\"\\n    }\\n  }\\n}\",\n" +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema_new\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ],\n" +
				"    \"status\": \"PUBLISHED\"\n" +
				"  }\n" +
				"}");
		schemaService.updateSchema(existingSchema, updatedSchema);
		JsonNode updatedDefinition = objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());
		assertNull(updatedDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("contact"));
		assertEquals(1, updatedDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("mobile").size());
	}

	@Test(expected = SchemaException.class)
	public void shouldNotUpdateSchemaStatusIfSchemaAlreadyPublished() throws SchemaException, IOException {
		JsonNode existingDefinition = objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());
		assertEquals(1, existingDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("contact").size());
		assertNull(existingDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("mobile"));
		JsonNode existingSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/TrainingCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"TrainingCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"TrainingCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/TrainingCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The TrainingCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"contact\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"trainingTitle\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"contact\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date\\\": {\\n          \\\"type\\\": \\\"string\\\",\\n          \\\"format\\\": \\\"date\\\"\\n        },\\n        \\\"note\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"contact\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        \\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"trainedOn\\\": \\\"{{trainingTitle}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\\\",\\n      \\\"svg\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\\\"\\n    }\\n  }\\n}\",\n" +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema_new\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ],\n" +
				"    \"status\": \"PUBLISHED\"\n" +
				"  }\n" +
				"}");
		JsonNode updatedSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/TrainingCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"TrainingCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"TrainingCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/TrainingCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The TrainingCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"contact\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"trainingTitle\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"contact\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date\\\": {\\n          \\\"type\\\": \\\"string\\\",\\n          \\\"format\\\": \\\"date\\\"\\n        },\\n        \\\"note\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"contact\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        \\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"trainedOn\\\": \\\"{{trainingTitle}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\\\",\\n      \\\"svg\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\\\"\\n    }\\n  }\\n}\",\n" +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema_new\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ],\n" +
				"    \"status\": \"DRAFT\"\n" +
				"  }\n" +
				"}");
		schemaService.updateSchema(existingSchema, updatedSchema);
		objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());
	}

	@Test
	public void shouldUpdateSchemaStatusIfInDraft() throws IOException, SchemaException {
		JsonNode existingDefinition = objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());
		assertEquals(1, existingDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("contact").size());
		assertNull(existingDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("mobile"));
		JsonNode existingSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/TrainingCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"TrainingCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"TrainingCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/TrainingCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The TrainingCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"contact\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"trainingTitle\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"contact\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date\\\": {\\n          \\\"type\\\": \\\"string\\\",\\n          \\\"format\\\": \\\"date\\\"\\n        },\\n        \\\"note\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"contact\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        \\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"trainedOn\\\": \\\"{{trainingTitle}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\\\",\\n      \\\"svg\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\\\"\\n    }\\n  }\\n}\",\n" +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema_new\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ],\n" +
				"    \"status\": \"DRAFT\"\n" +
				"  }\n" +
				"}");
		JsonNode updatedSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/TrainingCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"TrainingCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"TrainingCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/TrainingCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The TrainingCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"mobile\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"trainingTitle\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"mobile\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date\\\": {\\n          \\\"type\\\": \\\"string\\\",\\n          \\\"format\\\": \\\"date\\\"\\n        },\\n        \\\"note\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"mobile\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        \\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"trainedOn\\\": \\\"{{trainingTitle}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\\\",\\n      \\\"svg\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\\\"\\n    }\\n  }\\n}\",\n" +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema_new\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ],\n" +
				"    \"status\": \"DRAFT\"\n" +
				"  }\n" +
				"}");
		schemaService.updateSchema(existingSchema, updatedSchema);
		JsonNode updatedDefinition = objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());
		assertNull(updatedDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("contact"));
		assertEquals(1, updatedDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("mobile").size());
	}

	@Test
	public void shouldUpdateSchemaStatus() throws IOException, SchemaException {
		JsonNode existingDefinition = objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());
		assertEquals(1, existingDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("contact").size());
		assertNull(existingDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("mobile"));
		JsonNode existingSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/TrainingCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"TrainingCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"TrainingCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/TrainingCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The TrainingCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"contact\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"trainingTitle\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"contact\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date\\\": {\\n          \\\"type\\\": \\\"string\\\",\\n          \\\"format\\\": \\\"date\\\"\\n        },\\n        \\\"note\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"contact\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        \\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"trainedOn\\\": \\\"{{trainingTitle}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\\\",\\n      \\\"svg\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\\\"\\n    }\\n  }\\n}\",\n" +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema_new\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ],\n" +
				"    \"status\": \"DRAFT\"\n" +
				"  }\n" +
				"}");
		JsonNode updatedSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/TrainingCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"TrainingCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"TrainingCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/TrainingCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The TrainingCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"mobile\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"trainingTitle\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"mobile\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date\\\": {\\n          \\\"type\\\": \\\"string\\\",\\n          \\\"format\\\": \\\"date\\\"\\n        },\\n        \\\"note\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"mobile\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        \\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"trainedOn\\\": \\\"{{trainingTitle}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\\\",\\n      \\\"svg\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.svg\\\"\\n    }\\n  }\\n}\",\n" +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema_new\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ],\n" +
				"    \"status\": \"PUBLISHED\"\n" +
				"  }\n" +
				"}");
		schemaService.updateSchema(existingSchema, updatedSchema);
		JsonNode updatedDefinition = objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());
		assertNull(updatedDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("contact"));
		assertEquals(1, updatedDefinition.get("definitions").get(TRAINING_CERTIFICATE).get("properties").get("mobile").size());
	}

	@Test
	public void shouldUpdateSchemaConfigIfSchemaIsPublished() throws SchemaException, IOException {
		JsonNode existingDefinition = objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());

		assertNull(existingDefinition.get("_osConfig").get("certificateTemplates"));
		JsonNode existingSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\\"$schema\\\":\\\"http://json-schema.org/draft-07/schema\\\",\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"TrainingCertificate\\\":{\\\"$ref\\\":\\\"#/definitions/TrainingCertificate\\\"}},\\\"required\\\":[\\\"TrainingCertificate\\\"],\\\"title\\\":\\\"TrainingCertificate\\\",\\\"definitions\\\":{\\\"TrainingCertificate\\\":{\\\"$id\\\":\\\"#/properties/TrainingCertificate\\\",\\\"type\\\":\\\"object\\\",\\\"title\\\":\\\"The TrainingCertificate Schema\\\",\\\"required\\\":[\\\"name\\\",\\\"contact\\\"],\\\"properties\\\":{\\\"name\\\":{\\\"type\\\":\\\"string\\\"},\\\"trainingTitle\\\":{\\\"type\\\":\\\"string\\\"},\\\"contact\\\":{\\\"type\\\":\\\"string\\\"},\\\"date\\\":{\\\"type\\\":\\\"string\\\",\\\"format\\\":\\\"date\\\"},\\\"note\\\":{\\\"type\\\":\\\"string\\\"}}}},\\\"_osConfig\\\":{\\\"uniqueIndexFields\\\":[\\\"contact\\\"],\\\"ownershipAttributes\\\":[{\\\"mobile\\\":\\\"/contact\\\",\\\"userId\\\":\\\"/contact\\\",\\\"email\\\":\\\"/contact\\\"}],\\\"roles\\\":[\\\"admin\\\"],\\\"inviteRoles\\\":[\\\"anonymous\\\"],\\\"enableLogin\\\":false,\\\"credentialTemplate\\\":{\\\"@context\\\":[\\\"https://www.w3.org/2018/credentials/v1\\\",\\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"],\\\"type\\\":[\\\"VerifiableCredential\\\"],\\\"issuanceDate\\\":\\\"2021-08-27T10:57:57.237Z\\\",\\\"credentialSubject\\\":{\\\"type\\\":\\\"Person\\\",\\\"name\\\":\\\"{{name}}\\\",\\\"trainedOn\\\":\\\"{{trainingTitle}}\\\"},\\\"issuer\\\":\\\"did:web:sunbirdrc.dev/vc/skill\\\"}}}\",\n" +
				"    " +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema_new\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ],\n" +
				"    \"status\": \"PUBLISHED\"\n" +
				"  }\n" +
				"}");
		JsonNode updatedSchema = objectMapper.readTree("{\n" +
				"  \"Schema\": {\n" +
				"    \"schema\": \"{\\n  \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema\\\",\\n  \\\"type\\\": \\\"object\\\",\\n  \\\"properties\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$ref\\\": \\\"#/definitions/TrainingCertificate\\\"\\n    }\\n  },\\n  \\\"required\\\": [\\n    \\\"TrainingCertificate\\\"\\n  ],\\n  \\\"title\\\": \\\"TrainingCertificate\\\",\\n  \\\"definitions\\\": {\\n    \\\"TrainingCertificate\\\": {\\n      \\\"$id\\\": \\\"#/properties/TrainingCertificate\\\",\\n      \\\"type\\\": \\\"object\\\",\\n      \\\"title\\\": \\\"The TrainingCertificate Schema\\\",\\n      \\\"required\\\": [\\n        \\\"name\\\",\\n        \\\"contact\\\"\\n      ],\\n      \\\"properties\\\": {\\n        \\\"name\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"trainingTitle\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"contact\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        },\\n        \\\"date\\\": {\\n          \\\"type\\\": \\\"string\\\",\\n          \\\"format\\\": \\\"date\\\"\\n        },\\n        \\\"note\\\": {\\n          \\\"type\\\": \\\"string\\\"\\n        }\\n      }\\n    }\\n  },\\n  \\\"_osConfig\\\": {\\n    \\\"uniqueIndexFields\\\": [\\n      \\\"contact\\\"\\n    ],\\n    \\\"ownershipAttributes\\\": [],\\n    \\\"roles\\\": [],\\n    \\\"inviteRoles\\\": [\\n      \\\"anonymous\\\"\\n    ],\\n    \\\"enableLogin\\\": false,\\n    \\\"credentialTemplate\\\": {\\n      \\\"@context\\\": [\\n        \\\"https://www.w3.org/2018/credentials/v1\\\",\\n        \\\"https://gist.githubusercontent.com/dileepbapat/eb932596a70f75016411cc871113a789/raw/498e5af1d94784f114b32c1ab827f951a8a24def/skill\\\"\\n      ],\\n      \\\"type\\\": [\\n        \\\"VerifiableCredential\\\"\\n      ],\\n      \\\"issuanceDate\\\": \\\"2021-08-27T10:57:57.237Z\\\",\\n      \\\"credentialSubject\\\": {\\n        \\\"type\\\": \\\"Person\\\",\\n        \\\"name\\\": \\\"{{name}}\\\",\\n        \\\"trainedOn\\\": \\\"{{trainingTitle}}\\\"\\n      },\\n      \\\"issuer\\\": \\\"did:web:sunbirdrc.dev/vc/skill\\\"\\n    },\\n    \\\"certificateTemplates\\\": {\\n      \\\"html\\\": \\\"https://raw.githubusercontent.com/dileepbapat/ref-sunbirdrc-certificate/main/schemas/templates/TrainingCertificate.html\\\"\\n    }\\n  }\\n}\",\n" +
				"    \"osUpdatedAt\": \"2022-09-14T05:38:41.909Z\",\n" +
				"    \"osCreatedAt\": \"2022-09-14T05:34:04.862Z\",\n" +
				"    \"osUpdatedBy\": \"anonymous\",\n" +
				"    \"@type\": \"Schema\",\n" +
				"    \"name\": \"schema_new\",\n" +
				"    \"osCreatedBy\": \"anonymous\",\n" +
				"    \"osid\": \"756cea4b-93a0-44d5-affd-bb605cf30abd\",\n" +
				"    \"osOwner\": [\n" +
				"      \"anonymous\"\n" +
				"    ],\n" +
				"    \"status\": \"PUBLISHED\"\n" +
				"  }\n" +
				"}");
		schemaService.updateSchema(existingSchema, updatedSchema);
		JsonNode updatedDefinition = objectMapper.readTree(definitionsManager.getDefinition(TRAINING_CERTIFICATE).getContent());
		assertNotNull(updatedDefinition.get("_osConfig").get("certificateTemplates"));
		assertEquals(1, updatedDefinition.get("_osConfig").get("certificateTemplates").size());
	}
}