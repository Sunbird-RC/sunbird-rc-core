package io.opensaber.pojos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.pojos.attestation.AttestationPolicy;

import java.util.Collections;

public class PluginRequestMessageCreator {
    public static PluginRequestMessage createClaimPluginMessage(JsonNode requestBody, AttestationPolicy attestationPolicy, String attestationOSID, String entityName, String entityId) {
        JsonNode propertyData = requestBody.get("propertyData");
        String properties = requestBody.get("properties").asText();
        PluginRequestMessage pluginRequestMessage = new PluginRequestMessage();
        pluginRequestMessage.setPolicyName(attestationPolicy.getName());
        pluginRequestMessage.setProperties(Collections.singletonList(properties));
        pluginRequestMessage.setAdditionalInputs(JsonNodeFactory.instance.nullNode());
        pluginRequestMessage.setPropertyData(propertyData);
        pluginRequestMessage.setSourceEntity(entityName);
        pluginRequestMessage.setSourceOSID(entityId);
        pluginRequestMessage.setAttestationOSID(attestationOSID);
        pluginRequestMessage.setAttestationType(attestationPolicy.getType().name());
        pluginRequestMessage.setAttestorPlugin(attestationPolicy.getAttestorPlugin());
        pluginRequestMessage.setAttestorEntity(attestationPolicy.getAttestorEntity());
        pluginRequestMessage.setAttestorSignin(attestationPolicy.getAttestorSignin());
        pluginRequestMessage.setConditions(attestationPolicy.getConditions());
        return pluginRequestMessage;
    }
}
