package io.opensaber.registry.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates Definition for a given JsonNode This accepts a schema
 *
 */
public class Definition {
    private static Logger logger = LoggerFactory.getLogger(Definition.class);
    private final static String TITLE = "title";
    private final static String OSCONFIG = "_osConfig";
    private final static String DEFINITIONS = "definitions";
    private final static String PROPERTIES = "properties";
    private final static String REF = "$ref";
    private final static String TYPE = "type";
    private final static String OBJECT = "object";

    private String content;
    private String title;

    private Map<String, String> subSchemaNames = new HashMap<>();

    private OSSchemaConfiguration osSchemaConfiguration = new OSSchemaConfiguration();

    /**
     * To parse a jsonNode of given schema type
     * 
     * @param schemaNode
     */
    public Definition(JsonNode schemaNode) {
        content = schemaNode.toString();
        if(!schemaNode.has(TITLE))
            throw new RuntimeException(TITLE + " not found for schema, " + schemaNode);
        title = schemaNode.get(TITLE).asText();
        ObjectMapper mapper = new ObjectMapper();

        JsonNode configJson = schemaNode.get(OSCONFIG);
        if (null != configJson) {
            try {
                osSchemaConfiguration = mapper.treeToValue(configJson, OSSchemaConfiguration.class);
            } catch (JsonProcessingException e) {
                logger.error("Error processing {} JSON: ", OSCONFIG, e);
                logger.debug(title + " does not have OS configuration.");
            }
        }

        // Iterate over all properties in the current definition
        JsonNode defnTitle = schemaNode.get(DEFINITIONS).get(title);
        JsonNode properties = null;
        if (null != defnTitle) {
            properties = defnTitle.get(PROPERTIES);
            properties.fields().forEachRemaining(field -> {
                JsonNode typeTextNode = field.getValue().get(TYPE);
                boolean isArrayType = typeTextNode != null && typeTextNode.asText().equals("array");

                JsonNode refTextNode = field.getValue().get(REF);
                if (isArrayType) {
                    refTextNode = field.getValue().get("items").get(REF);
                }

                boolean isRefValid = isRefNode(refTextNode);
                if (isRefValid) {
                    String refVal = refTextNode.asText();
                    logger.debug("{}.{} is a ref field with value {}", title, field.getKey(), refVal);
                    addFieldSchema(field.getKey(),
                            refVal.substring(refVal.lastIndexOf("/") + 1));
                }
            });
        }
    }


    private boolean isRefNode(JsonNode refTextNode) {
        return (refTextNode != null && !refTextNode.isMissingNode() && !refTextNode.isNull());
    }

    /**
     * Holds the title for a given schema
     * 
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * Holds the String representation of schema
     * 
     * @return
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the OSSchemaConfiguration
     * @return
     */
    public OSSchemaConfiguration getOsSchemaConfiguration() {
        return osSchemaConfiguration;
    }

    public void addFieldSchema(String fieldName, String definitionName) {
        subSchemaNames.put(fieldName, definitionName);
    }

    public String getDefinitionNameForField(String name) {
        return subSchemaNames.getOrDefault(name, null);
    }

    public Map<String, String> getSubSchemaNames() {
        return subSchemaNames;
    }
}
