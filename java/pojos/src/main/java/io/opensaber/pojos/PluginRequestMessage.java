package io.opensaber.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.*;

import java.util.List;
import java.util.Optional;

@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class PluginRequestMessage {
    String policyName;
    JsonNode additionalInputs;
    String propertyData;
    String sourceEntity;
    String sourceOSID;
    String attestationOSID;
    String attestationType;
    String attestorPlugin;
    String attestorEntity;
    String attestorSignin;
    String conditions;
    String status;

    public Optional<String> getActorName() {
        // sample names did:plugin:aadhar, did:plugin:claim,
        String[] split1 = attestorPlugin.split("\\?");
        String[] split2 = split1[0].split(":");
        return split2.length >= 3 ? Optional.of(split2[2]) : Optional.empty();
    }
}
