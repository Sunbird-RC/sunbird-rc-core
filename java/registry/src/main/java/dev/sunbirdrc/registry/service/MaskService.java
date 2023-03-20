package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import dev.sunbirdrc.registry.model.EventConfig;
import dev.sunbirdrc.registry.service.mask.*;
import dev.sunbirdrc.registry.util.OSSchemaConfiguration;
import org.springframework.stereotype.Service;

import java.util.List;

import static dev.sunbirdrc.registry.middleware.util.JSONUtil.convertObjectJsonString;

@Service
public class MaskService {
    private static JsonNode updateFields(JsonNode jsonNode, List<String> fields, EventConfig eventConfig) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        DocumentContext documentContext = JsonPath.parse(convertObjectJsonString(jsonNode));
        for(String str : fields) {
            String value = updateValue(documentContext.read(str), eventConfig);
            if(value == null) {
                documentContext.delete(str);
                continue;
            }
            documentContext.set(str, value);
        }
        return objectMapper.readTree(documentContext.jsonString());
    }

    private static String updateValue(String value, EventConfig config) {
        IEmitStrategy maskConfig = EmitMap.getMaskConfig(config);
        return maskConfig.updateValue(value);
    }

    public JsonNode updatePrivateAndInternalFields(JsonNode jsonNode, OSSchemaConfiguration osSchemaConfiguration) throws JsonProcessingException {
        JsonNode maskedPrivateFields = updateFields(jsonNode, osSchemaConfiguration.getPrivateFields(), osSchemaConfiguration.getPrivateFieldConfig());
        return updateFields(maskedPrivateFields, osSchemaConfiguration.getInternalFields(), osSchemaConfiguration.getInternalFieldConfig());
    }
}
