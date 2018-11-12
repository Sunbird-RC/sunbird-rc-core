package io.opensaber.validators.json.jsonschema;

import java.io.InputStream;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.validators.IValidate;

public class JsonValidationServiceImpl implements IValidate {

	private void entity() {
		// TODO: This can move to be a member when we want to validate.
		// Stubbing for now.
		Schema schema;
		InputStream schemaStream = JsonValidationServiceImpl.class.getResourceAsStream("public/_schemas/Teacher.json");
		JSONObject rawSchema = new JSONObject(new JSONTokener(schemaStream));
		SchemaLoader schemaLoader = SchemaLoader.builder().schemaJson(rawSchema).draftV7Support()
				.resolutionScope("http://localhost:8080/_schemas/").build();
		schema = schemaLoader.load().build();
	}

	@Override
	public ValidationResponse validate(Object input, String methodOrigin) throws MiddlewareHaltException {
	    entity();
		String inputStr = input.toString();
		return new ValidationResponse(inputStr);
	}
}
