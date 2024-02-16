package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.authorization.SchemaAuthFilter;
import dev.sunbirdrc.pojos.UniqueIdentifierField;
import dev.sunbirdrc.registry.entities.SchemaStatus;
import dev.sunbirdrc.registry.exception.CustomException;
import dev.sunbirdrc.registry.exception.SchemaException;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.validators.IValidate;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static dev.sunbirdrc.registry.Constants.*;
import static dev.sunbirdrc.registry.helper.RegistryHelper.ROLE_ANONYMOUS;
import static dev.sunbirdrc.registry.exception.ErrorMessages.NOT_ALLOWED_FOR_PUBLISHED_SCHEMA;

@Service
public class SchemaService {
	private static final String STATUS = "status";
	@Autowired
	private IDefinitionsManager definitionsManager;

	@Autowired(required = false)
	private IIdGenService idGenService;
	@Value("${idgen.enabled:false}")
	private boolean idGenEnabled;

	@Autowired
	private boolean isElasticSearchEnabled;

	@Autowired
	private IValidate validator;

	@Autowired
	private SchemaAuthFilter schemaAuthFilter;

	public void deleteSchemaIfExists(Vertex vertex) throws SchemaException {
		if (vertex.property(STATUS) != null && vertex.property(STATUS).value().equals(SchemaStatus.PUBLISHED.toString())) {
			throw new SchemaException(NOT_ALLOWED_FOR_PUBLISHED_SCHEMA);
		}
		JsonNode schema = JsonNodeFactory.instance.textNode(vertex.property(Schema.toLowerCase()).value().toString());
		try {
			String schemaName = getSchemaName(schema);
			definitionsManager.removeDefinition(schemaName);
			validator.removeDefinition(schemaName);
			schemaAuthFilter.removeSchema(schemaName);
		} catch (JsonProcessingException e) {
			throw new SchemaException("Removing schemas from resources failed");
		}
	}

	private String getSchemaName(JsonNode jsonNode) throws JsonProcessingException {
		String schemaAsText = jsonNode.asText("{}");
		JsonNode schemaJsonNode = new ObjectMapper().readTree(schemaAsText);
		return schemaJsonNode.get(TITLE).asText();
	}


	public void addSchema(JsonNode schemaNode) throws IOException, SchemaException {
		if (schemaNode.get(Schema).get(STATUS) == null) {
			((ObjectNode) schemaNode.get(Schema)).put(STATUS, SchemaStatus.DRAFT.toString());
		}
		JsonNode schema = schemaNode.get(Schema).get(Schema.toLowerCase());
		if (schemaNode.get(Schema).get(STATUS).textValue().equals(SchemaStatus.PUBLISHED.toString())) {
			Definition definition = Definition.toDefinition(schema);
			if (definitionsManager.getDefinition(definition.getTitle()) == null) {
				definitionsManager.appendNewDefinition(definition);
				validator.addDefinitions(schema);
				addAnonymousSchemaToFilter(definition);
			} else {
				throw new SchemaException("Duplicate Error: Schema already exists");
			}
			saveIdFormat(definition.getTitle());
		}
	}

	private void addAnonymousSchemaToFilter(Definition definition) {
		if (definition.getOsSchemaConfiguration().getInviteRoles().contains(ROLE_ANONYMOUS)) {
			schemaAuthFilter.appendAnonymousInviteSchema(definition.getTitle());
		}
		if (definition.getOsSchemaConfiguration().getRoles().contains(ROLE_ANONYMOUS)) {
			schemaAuthFilter.appendAnonymousSchema(definition.getTitle());
		}
	}

	public void updateSchema(JsonNode updatedSchema) throws IOException, SchemaException {
		JsonNode schemaNode = updatedSchema.get(Schema);
		if (schemaNode.get(STATUS) != null && schemaNode.get(STATUS).textValue().equals(SchemaStatus.PUBLISHED.toString())) {
			JsonNode schema = schemaNode.get(Schema.toLowerCase());
			Definition definition = definitionsManager.appendNewDefinition(schema);
			addAnonymousSchemaToFilter(definition);
			validator.addDefinitions(schema);
			saveIdFormat(definition.getTitle());
		}
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

	public void validateNewSchema(JsonNode schemaNode) throws SchemaException {
		JsonNode schema = schemaNode.get(Schema).get(Schema.toLowerCase());
		try {
			Definition definition = Definition.toDefinition(schema);
			if (definitionsManager.getInternalSchemas().contains(definition.getTitle())) {
				throw new SchemaException(String.format("Duplicate Error: Internal schema \"%s\" already exists", definition.getTitle()));
			}
			if (definitionsManager.getDefinition(definition.getTitle()) != null) {
				throw new SchemaException(String.format("Duplicate Error: Schema \"%s\" already exists", definition.getTitle()));
			}
		} catch (JsonProcessingException e) {
			throw new SchemaException("Schema definition is not valid", e);
		}
	}


	public void validateUpdateSchema(JsonNode existingSchemaNode, JsonNode updatedSchemaNode) throws SchemaException, JsonProcessingException {
		JsonNode existingSchemaStatus = existingSchemaNode.get(Schema).get(STATUS);
		JsonNode updatedSchema = updatedSchemaNode.get(Schema).get(Schema.toLowerCase());
		try {
			Definition definition = Definition.toDefinition(updatedSchema);
			if (definitionsManager.getInternalSchemas().contains(definition.getTitle())) {
				throw new SchemaException(String.format("Duplicate Error: Internal schema \"%s\" already exists", definition.getTitle()));
			}
		} catch (JsonProcessingException e) {
			throw new SchemaException("Schema definition is not valid", e);
		}
		if (existingSchemaStatus != null) {
			checkIfSchemaDefinitionUpdatedForPublishedSchema(existingSchemaNode, updatedSchemaNode, existingSchemaStatus);
			checkIfSchemaStatusUpdatedForPublishedSchema(updatedSchemaNode, existingSchemaStatus);
		}
	}

	private void saveIdFormat(String title) throws SchemaException {
		if(!idGenEnabled) return;
		List<UniqueIdentifierField> identifierFieldList = definitionsManager.getUniqueIdentifierFields(title);
		try {
			idGenService.saveIdFormat(identifierFieldList);
		} catch (CustomException e) {
			throw new SchemaException(e.getMessage(), e.getCause());
		}
	}


}
