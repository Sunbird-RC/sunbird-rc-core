package io.opensaber.validators.json.jsonschema;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import io.opensaber.pojos.APIMessage;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;


import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.validators.IValidate;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.context.support.ServletContextResource;

import javax.servlet.ServletContext;

public class JsonValidationServiceImpl implements IValidate {
	private static Logger logger = LoggerFactory.getLogger(JsonValidationServiceImpl.class);

	private ResourceLoader resourceLoader;
	private Map<String, Schema> entitySchemaMap = new HashMap<>();

	@Autowired
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	private Schema getEntitySchema(String entityType) throws MiddlewareHaltException {

		if (entitySchemaMap.containsKey(entityType)) {
			return entitySchemaMap.get(entityType);
		} else {
			Schema schema;
			try {
				Resource resource = resourceLoader.getResource("classpath:/public/_schemas/" + entityType + ".json");
				InputStream schemaStream = resource.getInputStream();

				JSONObject rawSchema = new JSONObject(new JSONTokener(schemaStream));
				SchemaLoader schemaLoader = SchemaLoader.builder().schemaJson(rawSchema).draftV7Support()
						.resolutionScope("http://localhost:8080/_schemas/").build();
				schema = schemaLoader.load().build();
				entitySchemaMap.put(entityType, schema);
			} catch (IOException ioe) {
				throw new MiddlewareHaltException("can't validate");
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
		}
		return result;
	}

}
