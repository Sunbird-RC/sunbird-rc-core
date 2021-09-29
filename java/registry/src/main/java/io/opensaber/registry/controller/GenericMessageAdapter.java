package io.opensaber.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.pojos.PluginRequestMessage;
import io.opensaber.pojos.attestation.AttestationPolicy;

public class GenericMessageAdapter {
    public static PluginRequestMessage createClaimPluginMessage(JsonNode requestBody, AttestationPolicy attestationPolicy, String attestationOSID, String entityName, String entityId) {
        JsonNode propertyData = requestBody.get("propertyData");
        JsonNode properties = requestBody.get("properties");
        return new PluginRequestMessage.PluginRequestMessageBuilder()
                .policyName(attestationPolicy.getName())
                .additionalInputs()
                .sourceEntity(entityName)
                .sourceOSID(entityId)
                .attestationOSID(attestationOSID)
                .attestorPlugin(attestationPolicy.getAttestorEntity())
                .properties(properties)
                .propertyData(propertyData)
                .conditions(attestationPolicy.getConditions())
                .build();
    }
}
