package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.entities.SchemaStatus;
import dev.sunbirdrc.registry.exception.SchemaException;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.validators.IValidate;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static dev.sunbirdrc.registry.Constants.PATH;
import static dev.sunbirdrc.registry.Constants.Schema;

@Service
public class SchemaService {
	private static final String STATUS = "status";
	@Autowired
	private IDefinitionsManager definitionsManager;

	@Autowired
	private boolean isElasticSearchEnabled;

	@Autowired
	private IValidate validator;

	public void deleteSchemaIfExists(Vertex vertex) throws SchemaException {
		if (vertex.property(STATUS) != null && vertex.property(STATUS).value().equals(SchemaStatus.PUBLISHED.toString())) {
			throw new SchemaException("Schema delete not allowed for a published schema");
		}
		JsonNode schema = JsonNodeFactory.instance.textNode(vertex.property(Schema.toLowerCase()).value().toString());
		definitionsManager.removeDefinition(schema);
		validator.removeDefinition(schema);
	}


	public void addSchema(JsonNode schemaNode) throws IOException {
		if (schemaNode.get(Schema).get(STATUS) == null) {
			((ObjectNode) schemaNode.get(Schema)).put(STATUS, SchemaStatus.DRAFT.toString());
		}
		JsonNode schema = schemaNode.get(Schema).get(Schema.toLowerCase());
		if (schemaNode.get(Schema).get(STATUS).textValue().equals(SchemaStatus.PUBLISHED.toString())) {
			definitionsManager.appendNewDefinition(schema);
			validator.addDefinitions(schema);
		}
	}

	public void updateSchema(JsonNode existingSchema, JsonNode updatedSchema) throws IOException, SchemaException {
		JsonNode existingSchemaStatus = existingSchema.get(Schema).get(STATUS);
		if (existingSchemaStatus != null) {
			checkIfSchemaDefinitionUpdatedForPublishedSchema(existingSchema, updatedSchema, existingSchemaStatus);
			checkIfSchemaStatusUpdatedForPublishedSchema(updatedSchema, existingSchemaStatus);
		}
		JsonNode schema = updatedSchema.get(Schema).get(Schema.toLowerCase());
		definitionsManager.appendNewDefinition(schema);
		validator.addDefinitions(schema);
	}

	private void checkIfSchemaStatusUpdatedForPublishedSchema(JsonNode updatedSchema, JsonNode existingSchemaStatus) throws SchemaException {
		JsonNode updatedSchemaStatus = updatedSchema.get(Schema).get(STATUS);
		if (existingSchemaStatus.textValue().equalsIgnoreCase(SchemaStatus.PUBLISHED.toString()) ) {
			if (updatedSchemaStatus != null && updatedSchemaStatus.textValue().equalsIgnoreCase(SchemaStatus.DRAFT.toString())) {
				throw new SchemaException("Schema status update not allowed for a published schema");
			}
		}
	}

	private void checkIfSchemaDefinitionUpdatedForPublishedSchema(JsonNode existingSchema, JsonNode updatedSchema, JsonNode existingSchemaStatus) throws JsonProcessingException, SchemaException {
		if (existingSchemaStatus.textValue().equalsIgnoreCase(SchemaStatus.PUBLISHED.toString())) {
			JsonNode existingSchemaDefinition = new ObjectMapper().readTree(existingSchema.get(Schema).get(Schema.toLowerCase()).asText());
			JsonNode updatedSchemaDefinition = new ObjectMapper().readTree(updatedSchema.get(Schema).get(Schema.toLowerCase()).asText());
			JsonNode diffJsonNode = JSONUtil.diffJsonNode(existingSchemaDefinition, updatedSchemaDefinition);
			for (JsonNode jsonNode : diffJsonNode) {
				if (jsonNode.get(PATH).textValue().startsWith("/definition")) {
					throw new SchemaException("Schema definition update not allowed for a published schema");
				}
			}
		}
	}
}
