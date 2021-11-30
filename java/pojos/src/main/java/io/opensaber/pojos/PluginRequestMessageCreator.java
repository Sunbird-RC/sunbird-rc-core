package io.opensaber.pojos;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.pojos.attestation.AttestationPolicy;

public class PluginRequestMessageCreator {
    public static PluginRequestMessage create(String propertyData,
                                              String condition,
                                              AttestationPolicy attestationPolicy,
                                              String attestationOSID,
                                              String entityName,
                                              String entityId, JsonNode additionalInput, String status) {

        PluginRequestMessage pluginRequestMessage = new PluginRequestMessage();
        pluginRequestMessage.setPolicyName(attestationPolicy.getName());
        pluginRequestMessage.setAdditionalInputs(additionalInput);
        pluginRequestMessage.setPropertyData(propertyData);
        pluginRequestMessage.setSourceEntity(entityName);
        pluginRequestMessage.setSourceOSID(entityId);
        pluginRequestMessage.setAttestationOSID(attestationOSID);
        pluginRequestMessage.setAttestorPlugin(attestationPolicy.getAttestorPlugin());
        pluginRequestMessage.setAttestorEntity(attestationPolicy.getAttestorEntity());
        pluginRequestMessage.setAttestorSignin(attestationPolicy.getAttestorSignin());
        pluginRequestMessage.setConditions(condition);
        pluginRequestMessage.setStatus(status);
        return pluginRequestMessage;
    }
}
