package io.opensaber.registry.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates Definition for a given JsonNode This accepts a schema
 *
 */
public class Definition {
    private static Logger logger = LoggerFactory.getLogger(Definition.class);
    private final static String TITLE = "title";
    private final static String OSCONFIG = "_osConfig";

    private String content;
    private String title;

    private OSSchemaConfiguration osSchemaConfiguration;

    /**
     * To parse a jsonNode of given schema type
     * 
     * @param schema
     */
    public Definition(JsonNode schema) {
        content = schema.toString();
        if(!schema.has(TITLE))
            throw new RuntimeException(TITLE + " not found for schema, " + schema);
        title = schema.get(TITLE).asText();
        ObjectMapper mapper = new ObjectMapper();

        JsonNode configJson = schema.get(OSCONFIG);
        if (null != configJson) {
            try {
                osSchemaConfiguration = mapper.treeToValue(configJson, OSSchemaConfiguration.class);
            } catch (JsonProcessingException e) {
                logger.debug(title + " does not have OS configuration.");
            }
        }
        
        //Default when no config provided
        if (osSchemaConfiguration == null) {
            osSchemaConfiguration = new OSSchemaConfiguration();
        }
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

    public OSSchemaConfiguration getOsSchemaConfiguration() {
        return osSchemaConfiguration;
    }
    
}
