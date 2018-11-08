package io.opensaber.pojos;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.*;

public class ValidationResponseSerializer implements JsonSerializer<ValidationResponse> {

	public JsonElement serialize(ValidationResponse validationResponse, Type type,
			JsonSerializationContext jsonSerializationContext) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("type", validationResponse.getType());
		final JsonElement fieldErrors = new Gson().toJsonTree(validationResponse.getFields(), Map.class);
		jsonObject.add("data", fieldErrors);
		return jsonObject;
	}
}
