package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@Component
public class RefResolver {
    private static Logger logger = LoggerFactory.getLogger(RefResolver.class);
    private final static String REF = "$ref";

    private final IDefinitionsManager definitionsManager;

    public RefResolver(IDefinitionsManager definitionsManager) {
        this.definitionsManager = definitionsManager;
    }

    public JsonNode getResolvedSchema(String rootDefinitionName, String rootContext) {
        Definition definition = definitionsManager.getDefinition(rootDefinitionName);
        String content = definition.getContent();
        try {
            JsonNode node = JSONUtil.convertStringJsonNode(content);
            ObjectNode jsonNode = ((ObjectNode) node);
            JsonNode resolvedDefinitions = resolveDefinitions(rootDefinitionName, jsonNode.get(rootContext));
            logger.info(JSONUtil.convertObjectJsonString(resolvedDefinitions));
            return resolvedDefinitions;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JsonNode resolveDefinitions(String currentDefinitionName, JsonNode currentNode) {
        if (checkIfRefIsPresent(currentNode)) {
            String refPath = currentNode.get(REF).asText();
            JsonNode refJsonNode = fetchDefinition(refPath, currentDefinitionName);
            if (refJsonNode != null && !refJsonNode.isNull()) {
                if (isExternalReference(refPath)) {
                    currentDefinitionName = getDefinitionName(refPath);
                }
                currentNode = refJsonNode;
            }
        }
        if (currentNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) currentNode;
            ArrayNode updateArrayNodes = JsonNodeFactory.instance.arrayNode();
            Iterator<JsonNode> node = arrayNode.elements();
            while (node.hasNext()) {
                JsonNode resultantNode = resolveDefinitions(currentDefinitionName, node.next());
                updateArrayNodes.add(resultantNode);
            }
            return updateArrayNodes;
        } else if (currentNode.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> it = currentNode.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                JsonNode resultantNode = resolveDefinitions(currentDefinitionName, entry.getValue());
                ((ObjectNode) currentNode).set(entry.getKey(), resultantNode);
            }
        }
        return currentNode;
    }

    private JsonNode fetchDefinition(String refPath, String currentDefinitionName) {
        try {
            if (isExternalReference(refPath)) {
                return extractDefinitionFromExternalFile(refPath);
            } else {
                return extractDefinition(refPath, currentDefinitionName);
            }

        } catch (Exception e) {
            logger.error("Fetching definition of $ref {} failed", refPath, e);
            return null;
        }
    }

    private boolean isExternalReference(String refPath) {
        return refPath.contains(".json");
    }

    private JsonNode extractDefinition(String refPath, String currentDefinitionName) throws IOException {
        Definition definition = definitionsManager.getDefinition(currentDefinitionName);
        JsonNode referenceJson = JSONUtil.convertStringJsonNode(definition.getContent());
        return referenceJson.at(extractJsonPointer(refPath));
    }

    private JsonNode extractDefinitionFromExternalFile(String refPath) throws IOException {
        String fileNameWithoutExtn = getDefinitionName(refPath);
        return extractDefinition(refPath, fileNameWithoutExtn);
    }

    private String getDefinitionName(String refPath) {
        String fileName = refPath.substring(0, refPath.indexOf("/"));
        return fileName.substring(0, fileName.indexOf('.'));
    }

    private String extractJsonPointer(String refPath) {
        return refPath.substring(refPath.indexOf("/")).replace("/#", "");
    }

    private boolean checkIfRefIsPresent(JsonNode node) {
        return node.has(REF);
    }
}
