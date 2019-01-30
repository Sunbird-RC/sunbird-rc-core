package io.opensaber.validators.json.jsonschema;

import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.validators.IValidate;
import java.util.HashMap;
import java.util.Map;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonValidationServiceImpl implements IValidate {
	private static Logger logger = LoggerFactory.getLogger(JsonValidationServiceImpl.class);

	private Map<String, Schema> entitySchemaMap = new HashMap<>();
	private Map<String, String> definitionMap = new HashMap<>();;

	private Schema getEntitySchema(String entityType) throws MiddlewareHaltException {

		if (entitySchemaMap.containsKey(entityType)) {
			return entitySchemaMap.get(entityType);
		} else {
			Schema schema;
			try {
				String definitionContent = definitionMap.get(entityType);
                JSONObject rawSchema = new JSONObject(definitionContent);

				SchemaLoader schemaLoader = SchemaLoader.builder().schemaJson(rawSchema).draftV7Support()
						.resolutionScope("http://localhost:8080/_schemas/").build();
				schema = schemaLoader.load().build();
				entitySchemaMap.put(entityType, schema);
			} catch (Exception ioe) {
			    ioe.printStackTrace();
				throw new MiddlewareHaltException("can't validate, "+ entityType + ": schema has a problem!");
			}
			return schema;
		}
	}

	@Override
	public boolean validate(String entityType, String objString) throws MiddlewareHaltException {
		boolean result = false;
		Schema schema = getEntitySchema(entityType);
		JSONObject obj = new JSONObject(objString);
		try {
			schema.validate(obj); // throws a ValidationException if this object is invalid
			result = true;
		} catch (ValidationException e) {
			logger.error(e.getMessage() + " : " + e.getErrorMessage());
			e.getCausingExceptions().stream()
					.map(ValidationException::getMessage)
					.forEach(logger::error);
			throw new MiddlewareHaltException(e.getMessage());
		}
		return result;
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

}
