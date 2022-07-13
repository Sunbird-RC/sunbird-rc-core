package dev.sunbirdrc.views;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TestSampleProvider implements IViewFunctionProvider<String> {
    private static Logger logger = LoggerFactory.getLogger(TestSampleProvider.class);
	@Override
	public String doAction(List<Object> values) {
		// doing a simple concat for the values
		return concat(values);
	}

	/**
	 * simple concat for the values as string and comma(',') as seperator
	 *
	 * @param args
	 * @return
	 */
	public String concat(List<Object> args) {
        ObjectNode inputNode = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL).valueToTree(args.get(0));
		inputNode.set("output", JsonNodeFactory.instance.textNode(inputNode.get("id").asText() + "-" + inputNode.get("name").asText()));
        try {
            return new ObjectMapper().writeValueAsString(inputNode);
        } catch (JsonProcessingException e) {
            logger.error("Error while performing transformation", e);
            return "";
        }
    }

}