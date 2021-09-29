package io.opensaber.pojos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;

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

    public Optional<String> getActorName() {
        // sample names did:plugin:aadhar, did:plugin:claim,
        String[] split = attestorPlugin.split(":");
        return split.length >= 3 ? Optional.of(split[2]) : Optional.empty();
    }
}
