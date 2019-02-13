package io.opensaber.registry.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.registry.middleware.util.Constants.JsonldConstants;
import io.opensaber.registry.middleware.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Json2LdTransformer implements ITransformer<Object> {

	private static final String SEPERATOR = ":";
	private static Logger logger = LoggerFactory.getLogger(Json2LdTransformer.class);
	private String context;
	private List<String> nodeTypes = new ArrayList<>();
	private String prefix = "";
	private final ObjectMapper mapper = new ObjectMapper();

	public Json2LdTransformer(String context, String domain) {
		this.context = context;
		prefix = domain + SEPERATOR;

	}

	@Override
	public Data<Object> transform(Data<Object> data) throws TransformationException {
		try {
			ObjectNode resultNode = (ObjectNode) mapper.readTree(data.getData().toString());
			String rootType = getTypeFromNode(resultNode);
			resultNode = (ObjectNode) resultNode.path(rootType);

			// Set the generic context to this entity type
			String modifiedContext = context.replace("<@type>", rootType);
			ObjectNode contextNode = (ObjectNode) mapper.readTree(modifiedContext);
			setNodeTypeToAppend(contextNode);

			// Add prefix to all content
			JSONUtil.addPrefix(resultNode, prefix, nodeTypes);
			logger.debug("Appended prefix to requestNode.");

			// Insert context to the result
			resultNode.setAll(contextNode);

			return new Data<>(resultNode);
		} catch (Exception ex) {
			logger.error("Error trnsx : " + ex.getMessage(), ex);
			throw new TransformationException(ex.getMessage(), ex, ErrorCode.JSON_TO_JSONLD_TRANFORMATION_ERROR);
		}
	}

	/*
	 * Given a input like the following, {entity:{"a":1, "b":1}} returns
	 * "entity" being the type of the json object.
	 */
	private String getTypeFromNode(ObjectNode requestNode) {
		String rootValue = "";
		if (requestNode.isObject()) {
			logger.info("root node to set as type " + requestNode.fields().next().getKey());
			rootValue = requestNode.fields().next().getKey();
		}
		return rootValue;
	}

	/**
	 * Extracting the sub entities from context.
	 * 
	 * @param contextNode
	 */
	private void setNodeTypeToAppend(ObjectNode contextNode) {
		ObjectNode context = (ObjectNode) contextNode.path(JsonldConstants.CONTEXT);
		nodeTypes.add(JsonldConstants.ID);
		context.fields().forEachRemaining(entry -> {
			if (entry.getValue().has(JsonldConstants.TYPE)
					&& entry.getValue().get(JsonldConstants.TYPE).asText().equalsIgnoreCase(JsonldConstants.ID)) {
				nodeTypes.add(entry.getKey());
			}
		});
	}

	@Override
	public void setPurgeData(List<String> keyToPruge) {
		// Nothing to purge
	}

}
