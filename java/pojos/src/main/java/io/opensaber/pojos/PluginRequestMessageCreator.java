package io.opensaber.pojos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.pojos.attestation.AttestationPolicy;

import java.util.Collections;

public class PluginRequestMessageCreator {
    public static PluginRequestMessage createClaimPluginMessage(JsonNode requestBody, AttestationPolicy attestationPolicy, String attestationOSID, String entityName, String entityId) {
        JsonNode propertyData = requestBody.get("propertyData");
        String properties = requestBody.get("properties").asText();
        return new PluginRequestMessage.PluginRequestMessageBuilder()
                .policyName(attestationPolicy.getName())
                .properties(Collections.singletonList(properties))
                .additionalInputs(JsonNodeFactory.instance.nullNode())
                .propertyData(propertyData)
                .sourceEntity(entityName)
                .sourceOSID(entityId)
                .attestationOSID(attestationOSID)
                .attestationOSID(attestationPolicy.getType().name())
                .attestorPlugin(attestationPolicy.getAttestorEntity())
                .attestorSignin(attestationPolicy.getAttestorSignin())
                .conditions(attestationPolicy.getConditions())
                .build();
    }
}
