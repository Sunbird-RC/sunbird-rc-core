package io.opensaber.pojos;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Map;

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
