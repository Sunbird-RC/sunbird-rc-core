package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import dev.sunbirdrc.registry.model.EventConfig;
import dev.sunbirdrc.registry.service.mask.EmitStrategyFactory;
import dev.sunbirdrc.registry.service.mask.IEmitStrategy;
import dev.sunbirdrc.registry.util.OSSchemaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

import static dev.sunbirdrc.registry.middleware.util.JSONUtil.convertObjectJsonString;

@Service
public class EntityTransformer {
    private static Logger logger = LoggerFactory.getLogger(EntityTransformer.class);

    private JsonNode updateFields(JsonNode jsonNode, List<String> fields, EventConfig eventConfig) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        DocumentContext documentContext = JsonPath.parse(convertObjectJsonString(jsonNode));
        for (String str : fields) {
            try {
                String value = updateValue(documentContext.read(str), eventConfig);
                if (value == null) {
                    documentContext.delete(str);
                    continue;
                }
                documentContext.set(str, value);
            } catch (PathNotFoundException e) {
                logger.error(e.toString());
            }
        }
        return objectMapper.readTree(documentContext.jsonString());
    }

    private String updateValue(String value, EventConfig config) {
        IEmitStrategy maskConfig = EmitStrategyFactory.getMaskConfig(config);
        return maskConfig.updateValue(value);
    }

    public JsonNode updatePrivateAndInternalFields(JsonNode jsonNode, OSSchemaConfiguration osSchemaConfiguration) throws JsonProcessingException {
        JsonNode maskedPrivateFields = updateFields(jsonNode, osSchemaConfiguration.getPrivateFields(), osSchemaConfiguration.getPrivateFieldConfig());
        return updateFields(maskedPrivateFields, osSchemaConfiguration.getInternalFields(), osSchemaConfiguration.getInternalFieldConfig());
    }
}
