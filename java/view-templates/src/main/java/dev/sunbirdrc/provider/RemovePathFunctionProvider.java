package dev.sunbirdrc.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import dev.sunbirdrc.views.IViewFunctionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RemovePathFunctionProvider implements IViewFunctionProvider<JsonNode> {
	private static final Logger logger = LoggerFactory.getLogger(RemovePathFunctionProvider.class);

	@Override
	public JsonNode doAction(List values) {
		return null;
	}

	@Override
	public JsonNode doAction(List<Object> values, String[] paths) {
		try {
			if (values.size() == 1 && paths.length > 1) {
				JsonNode jsonObject = (JsonNode) values.get(0);
				DocumentContext documentContext = JsonPath.parse(new ObjectMapper().writeValueAsString(jsonObject));
				for (int i = 1, pathsLength = paths.length; i < pathsLength; i++) {
					String path = paths[i];
					try {
						documentContext.delete(JsonPath.compile(path));
					} catch (Exception e) {
						logger.error("Error while deleting path: ", e);
					}
				}
				return new ObjectMapper().readTree(documentContext.jsonString());
			}
		} catch (Exception e) {
			logger.error("Error while removing paths: ", e);
		}

		return JsonNodeFactory.instance.textNode("");

	}
}
