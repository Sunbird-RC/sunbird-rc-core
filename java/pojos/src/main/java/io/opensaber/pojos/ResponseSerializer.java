package io.opensaber.pojos;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Map;

public class ResponseSerializer implements JsonSerializer<Response> {
	private static Gson gson = new Gson();

	public JsonElement serialize(Response response, Type type, JsonSerializationContext jsonSerializationContext) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("id", response.getId());
		jsonObject.addProperty("ver", response.getVer());
		jsonObject.addProperty("ets", response.getEts());
		final JsonElement responseParams = gson.toJsonTree(response.getParams(), Map.class);
		jsonObject.add("params", responseParams);
		jsonObject.addProperty("responseCode", response.getResponseCode());
		jsonObject.add("result", gson.toJsonTree(response.getResult()));
		return jsonObject;
	}
}
