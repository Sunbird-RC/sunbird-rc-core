package dev.sunbirdrc.claim.dto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class AttestationPropertiesDTO {
    private Map<String, Object>  entity;

    public Map<String, Object> getEntity() {
        return entity;
    }

    public void setEntity(Map<String, Object> entity) {
        this.entity = entity;
    }


    public JsonNode getEntityAsJsonNode() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(entity, JsonNode.class);
    }
}
