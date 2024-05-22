package dev.sunbirdrc.registry.entities;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AttestationStep {
    private Map<String, String> additionalProperties = new HashMap<>();
    private String apiURL;
    private String apiMethod;
    private String apiRequestSchema;

    @JsonAnySetter
    public void setAdditionalProperty(String name, String value) {
        additionalProperties.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }
}
