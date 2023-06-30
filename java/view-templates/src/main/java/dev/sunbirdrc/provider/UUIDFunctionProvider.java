package dev.sunbirdrc.provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.views.IViewFunctionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * This class is a sample implementation class of IViewFunctionProvider<T>
 *
 */
public class UUIDFunctionProvider implements IViewFunctionProvider<String> {
    private static Logger logger = LoggerFactory.getLogger(UUIDFunctionProvider.class);
    @Override
    public String doAction(List<Object> values) {
        // doing a simple concat for the values
        return generateUUID(values);
    }

    @Override
    public String doAction(List<Object> values, String[] paths) {
        return doAction(values);
    }

    /**
     * simple concat for the values as string and comma(',') as seperator
     *
     * @param args
     * @return
     */
    public String generateUUID(List<Object> args) {
        ObjectNode inputNode = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL).valueToTree(args.get(0));
        inputNode.set("output", JsonNodeFactory.instance.textNode(UUID.randomUUID().toString()));
        try {
            return new ObjectMapper().writeValueAsString(inputNode);
        } catch (JsonProcessingException e) {
            logger.error("Error while performing transformation", e);
            return "";
        }
    }

}
