package io.opensaber.registry.transformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.opensaber.registry.middleware.transform.commons.Data;
import io.opensaber.registry.middleware.transform.commons.ErrorCode;
import io.opensaber.registry.middleware.transform.commons.ITransformer;
import io.opensaber.registry.middleware.transform.commons.TransformationException;
import io.opensaber.registry.middleware.transform.commons.Constants.JsonldConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

@Component
public class JsonToLdTransformer implements ITransformer<Object> {

	private static Logger logger = LoggerFactory.getLogger(JsonToLdTransformer.class);
	private List<String> keysToPurge = new ArrayList<>();

	public Data<Object> transform(Data<Object> data) throws TransformationException {
		try {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode input = (ObjectNode) mapper.readTree(data.getData().toString());
			JsonNode jsonNode = getconstructedJson(input, keysToPurge);
			return new Data<>(jsonNode);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw new TransformationException(ex.getMessage(), ex, ErrorCode.JSONLD_TO_JSON_TRANFORMATION_ERROR);
		}
	}

	private JsonNode getconstructedJson(ObjectNode rootDataNode, List<String> keysToPurge)
			throws IOException, ParseException {

		setPurgeData(keysToPurge);
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
		for(JsonNode graphNode : rootDataNode.path(JsonldConstants.GRAPH)){
			ObjectNode rootNode = addRootTypeNode(graphNode);
			if (keysToPurge.size() != 0)
				purgedKeys(rootNode);
			arrayNode.add(rootNode);
		}
		return arrayNode;
	}

	private ObjectNode addRootTypeNode(JsonNode graphNode) {
		String rootNodeType = graphNode.path(JsonldConstants.TYPE).asText();
		ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
		rootNode.set(rootNodeType, graphNode);
		return rootNode;

	}

	private void purgedKeys(ObjectNode node) {
		List<String> removeKeyNames = new ArrayList<String>();
		node.fields().forEachRemaining(entry -> {
			if (keysToPurge.contains(entry.getKey())) {
				removeKeyNames.add(entry.getKey());
			} else {
				if (entry.getValue().isArray()) {
					for (int i = 0; i < entry.getValue().size(); i++) {
						if (entry.getValue().get(i).isObject()) 
							purgedKeys((ObjectNode) entry.getValue().get(i));
					}
				} else if (entry.getValue().isObject()) {
					purgedKeys((ObjectNode) entry.getValue());
				}
			}
		});
		node.remove(removeKeyNames);
	}

	@Override
	public void setPurgeData(List<String> keyToPruge) {
		this.keysToPurge = keyToPruge;

	}

}
