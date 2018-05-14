package io.opensaber.registry.transform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.config.Configuration;
import io.opensaber.registry.constants.Constants.*;
import io.opensaber.registry.constants.ErrorCode;
import io.opensaber.registry.exception.TransformationException;
import io.opensaber.registry.transform.utils.JsonUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public class JsonldToJsonTransformer implements ITransformer<String> {

    private static JsonldToJsonTransformer instance;
    private static Logger logger = LoggerFactory.getLogger(JsonldToJsonTransformer.class);
    private JsonNode fieldMapping;
    private static ObjectMapper mapper = new ObjectMapper();
    private static TypeReference<Map<String, String>> mapTypeRef = new TypeReference<Map<String, String>>() {};

    static {
        try {
            instance = new JsonldToJsonTransformer();
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }


    public static JsonldToJsonTransformer getInstance() {
        return instance;
    }

    private JsonldToJsonTransformer() throws Exception {
        this.fieldMapping = loadDefaultMapping();
    }

    private JsonNode loadDefaultMapping() throws IOException {
        String mappingJson = CharStreams.toString(new InputStreamReader
                (JsonldToJsonTransformer.class.getClassLoader().getResourceAsStream(Configuration.MAPPING_FILE), "UTF-8"));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(mappingJson);
    }

    @Override
    public ResponseData<String> transform(RequestData<String> data) throws TransformationException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode input = mapper.readTree(data.getRequestData());
            ObjectNode result = constructJson(input, fieldMapping);
            String jsonldResult = mapper.writeValueAsString(result);
            return new ResponseData<>(jsonldResult);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new TransformationException(ex.getMessage(), ex, ErrorCode.JSONLD_TO_JSON_TRANFORMATION_ERROR);
        }
    }

    public ObjectNode constructJson(JsonNode rootDataNode, JsonNode nodeMapping) throws IOException, ParseException {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        JsonNode framedJsonldNode = rootDataNode.path(JsonldConstants.GRAPH);
        Map<String, String> context = mapper.readValue(nodeMapping.path(MappingConstants.CONTEXT).toString(), mapTypeRef);

        for (JsonNode dataNode : framedJsonldNode) {
            String dataNodeType = dataNode.path(JsonldConstants.TYPE).asText();
            for (JsonNode mapping : nodeMapping) {
                if (String.format("%s:%s", mapping.path(MappingConstants.PREFIX).asText(),
                        mapping.path(MappingConstants.TYPE).asText()).equalsIgnoreCase(dataNodeType)) {
                    String nodeLabel = mapping.path(MappingConstants.TYPE).asText().toLowerCase();
                    result.setAll(processComplexNode(
                            new AbstractMap.SimpleEntry<>(nodeLabel, dataNode),
                            mapping.path(MappingConstants.DEFINITION),
                            context));
                    break;
                }
            }
        }
        return result;
    }

    public ObjectNode processComplexNode(Map.Entry<String, JsonNode> dataNode,
                                         JsonNode mapping, Map<String, String> context) throws ParseException {
        ObjectNode result = JsonUtils.createObjectNode();
        String field = dataNode.getKey();
        if (dataNode.getValue().isArray()) {
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
            for (JsonNode node : dataNode.getValue()) {
                arrayNode.add(processUnitComplexNode(node, mapping, context));
            }
            result.putArray(field).addAll(arrayNode);
        } else {
            result.set(field, processUnitComplexNode(dataNode.getValue(), mapping, context));
        }
        return result;
    }

    private ObjectNode processUnitComplexNode(JsonNode dataNode, JsonNode mapping, Map<String, String> context) throws ParseException {
        ObjectNode resultNode = JsonUtils.createObjectNode();
        List<Map.Entry<String, JsonNode>> childNodes = Lists.newArrayList(dataNode.fields());
        for (Map.Entry<String, JsonNode> node : childNodes) {
            if (!(node.getKey().equalsIgnoreCase(JsonldConstants.ID)
                    || node.getKey().equalsIgnoreCase(JsonldConstants.TYPE))) {

                String nodeKey = node.getKey().split(":")[1];
                if(mapping.path(nodeKey).path(MappingConstants.DEFINITION).isMissingNode()) {
                    resultNode.setAll(processLiteralsOrConstantsNode(node));
                } else {
                    JsonNode nodeMapping = mapping.path(nodeKey).path(MappingConstants.DEFINITION);
                    if(mapping.path(nodeKey).path(MappingConstants.COLLECTION).asBoolean()) {
                        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                        for (JsonNode n : node.getValue()) {
                            arrayNode.add(processUnitComplexNode(n, nodeMapping, context));
                        }
                        resultNode.putArray(nodeKey).addAll(arrayNode);
                    } else {
                        resultNode.set(nodeKey, processUnitComplexNode(node.getValue(), nodeMapping, context));
                    }
                }
            }
        }

        String [] nodeIdArray = dataNode.path(JsonldConstants.ID).asText().split(":");
        String contextUri = context.getOrDefault(nodeIdArray[0], nodeIdArray[1]);
        resultNode.put(MappingConstants.ID, contextUri + nodeIdArray[1]);
        return resultNode;
    }

    public ObjectNode processLiteralsOrConstantsNode(Map.Entry<String, JsonNode> node) throws ParseException {
        ObjectNode result = JsonUtils.createObjectNode();
        JsonNode nodeData = node.getValue();
        String field = node.getKey().split(":")[1];
        if (nodeData.isArray()) {
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
            for (JsonNode dataNode : nodeData) {
                if (dataNode.path(JsonldConstants.ID).isMissingNode()) {
                    arrayNode.add(processLiteralNode(dataNode));
                } else {
                    arrayNode.add(processEnumeratedConstant(dataNode));
                }
            }
            result.putArray(field).addAll(arrayNode);
        } else {
            if (nodeData.path(JsonldConstants.ID).isMissingNode()) {
                result.set(field, processLiteralNode(nodeData));
            } else {
                result.set(field, processEnumeratedConstant(nodeData));
            }
        }
        return result;
    }

    private JsonNode processEnumeratedConstant(JsonNode dataNode) {
        return mapper.convertValue(dataNode.path(JsonldConstants.ID).asText().split(":")[1], JsonNode.class);
    }

    private JsonNode processLiteralNode(JsonNode dataNode) throws ParseException {
        String data = dataNode.path(JsonldConstants.VALUE).isMissingNode() ? dataNode.asText()
                : dataNode.path(JsonldConstants.VALUE).asText();
        JsonNode result;
        if (NumberUtils.isNumber(data)) {
            result = mapper.convertValue(NumberFormat.getInstance().parse(data), JsonNode.class);
        } else {
            result = mapper.convertValue(data, JsonNode.class);
        }
        return result;
    }

}
