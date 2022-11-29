package dev.sunbirdrc.validators.json.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;
import dev.sunbirdrc.validators.IValidate;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonValidationServiceImpl implements IValidate {
	private static Logger logger = LoggerFactory.getLogger(JsonValidationServiceImpl.class);

	public final static String TITLE = "title";
	private final String REQUIRED_KEYWORD = "required";

	private Map<String, Schema> entitySchemaMap = new HashMap<>();
	private Map<String, String> definitionMap = new HashMap<>();;
	private final String schemaUrl;

	private final ObjectMapper objectMapper;

	public JsonValidationServiceImpl(String schemaUrl) {
		this.schemaUrl = schemaUrl;
		this.objectMapper = new ObjectMapper();
	}

	private Schema getEntitySchema(String entityType) throws MiddlewareHaltException {
		if (entitySchemaMap.containsKey(entityType)) {
			return entitySchemaMap.get(entityType);
		} else {
			Schema schema;
			try {
				String definitionContent = definitionMap.get(entityType);
				if (definitionContent != null) {
					JSONObject rawSchema = new JSONObject(definitionContent);

					SchemaLoader schemaLoader = SchemaLoader.builder().schemaJson(rawSchema).draftV7Support()
							.resolutionScope(schemaUrl).build();
					schema = schemaLoader.load().build();
					entitySchemaMap.put(entityType, schema);
				} else {
					return null;
				}
			} catch (Exception ioe) {
			    ioe.printStackTrace();
				throw new MiddlewareHaltException("can't validate, "+ entityType + ": schema has a problem!");
			}
			return schema;
		}
	}

	@Override
	public void validate(String entityType, String objString, boolean ignoreRequiredFields) throws MiddlewareHaltException {
		Schema schema = getEntitySchema(entityType);
		if (schema != null) {
			JSONObject obj = new JSONObject(objString);
			try {
				schema.validate(obj); // throws a ValidationException if this object is invalid
			} catch (ValidationException e) {
				logger.error("Validation Exception : " + e.getAllMessages());
				if (ignoreRequiredFields) {
					List<ValidationException> flattenedExceptions = flattenException(e).stream()
							.filter(ve -> !ve.getKeyword().equals(REQUIRED_KEYWORD))
							.collect(Collectors.toList());

					if (!flattenedExceptions.isEmpty()) {
						String errMsg = flattenedExceptions.stream()
								.map(ve -> String.format("%s : %s", e.getPointerToViolation(), e.getMessage()))
								.collect(Collectors.joining("; "));
						throw new MiddlewareHaltException("Validation Exception : " + errMsg);
					}
				} else {
					throw new MiddlewareHaltException("Validation Exception : " + String.join("; ", e.getAllMessages()));
				}
			}
		} else {
			logger.warn("{} schema not found for validation", entityType);
		}
	}

	/**
     * Store all list of known definitions as definitionMap.
     * Must get populated before creating the schema.
     *
     * @param definitionTitle
     * @param definitionContent
     */
    @Override
    public void addDefinitions(String definitionTitle, String definitionContent) {
        definitionMap.put(definitionTitle, definitionContent);
    }

	private List<ValidationException> flattenException(ValidationException e) {
		List<ValidationException> flattenedValidationExceptions = new ArrayList<>();
		if (!e.getCausingExceptions().isEmpty()) {
			for (ValidationException ve : e.getCausingExceptions()) {
				flattenedValidationExceptions.addAll(flattenException(ve));
			}
		} else {
			flattenedValidationExceptions.add(e);
		}
		return flattenedValidationExceptions;
	}

	@Override
	public void addDefinitions(JsonNode schema) throws IOException {
		JsonNode schemaJsonNode = objectMapper.readTree(schema.asText("{}"));
		String title = schemaJsonNode.get(TITLE).asText();
		this.addDefinitions(title, schema.asText("{}"));
	}

	@Override
	public void removeDefinition(JsonNode jsonNode) {
		try {
			String schemaAsText = jsonNode.asText("{}");
			JsonNode schemaJsonNode = objectMapper.readTree(schemaAsText);
			String schemaTitle = schemaJsonNode.get(TITLE).asText();
			definitionMap.remove(schemaTitle);
		} catch (Exception e) {
			logger.error("Failed removing schema from definition manager", e);
		}
	}
}
