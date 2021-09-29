package io.opensaber.pojos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.List;

@JsonSerialize
@Data
@RequiredArgsConstructor
@Builder
public class PluginRequestMessage {
    String policyName;
    List<String> properties;
    JsonNode additionalInputs;
    JsonNode propertyData;
    String sourceEntity;
    String sourceOSID;
    String attestationOSID;
    String attestationType;
    String attestorPlugin;
    String conditions;
}
