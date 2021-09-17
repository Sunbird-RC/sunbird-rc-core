package io.opensaber.pojos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@JsonSerialize
@Data
@RequiredArgsConstructor
public class PluginRequestMessage {
    String policyName;
    List<String> properties;
    Map additionalInputs;
    Map propertyData;
    String sourceEntity;
    String sourceOSID;
    String attestationOSID;
    String attestationType;
    String attestorPlugin;
    String conditions;
}
