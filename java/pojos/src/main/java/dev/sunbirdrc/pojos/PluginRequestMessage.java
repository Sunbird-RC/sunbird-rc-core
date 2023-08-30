package dev.sunbirdrc.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.*;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class PluginRequestMessage {
    private String policyName;
    private JsonNode additionalInputs;
    private String propertyData;
    private String sourceEntity;
    private String sourceOSID;
    private String attestationOSID;
    private String attestationType;
    private String attestorPlugin;
    private String attestorEntity;
    private String attestorSignin;
    private String conditions;
    private String status;
    @Nullable
    private String userId;
    private Map<String, List<String>> propertiesOSID;
    private String emailId;
    private String credType;

    public Optional<String> getActorName() {
        // sample names did:plugin:aadhar, did:plugin:claim,
        String[] split1 = attestorPlugin.split("\\?");
        String[] split2 = split1[0].split(":");
        return split2.length >= 3 ? Optional.of(split2[2]) : Optional.empty();
    }

}
