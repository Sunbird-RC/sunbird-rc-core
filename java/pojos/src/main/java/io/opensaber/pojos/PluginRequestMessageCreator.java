package io.opensaber.pojos;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.pojos.attestation.Action;
import io.opensaber.pojos.attestation.AttestationPolicy;

import java.util.Collections;

public class PluginRequestMessageCreator {
    public static PluginRequestMessage createClaimPluginMessage(String propertyData, String condition, AttestationPolicy attestationPolicy, String attestationOSID, String entityName, String entityId) {
        PluginRequestMessage pluginRequestMessage = new PluginRequestMessage();
        pluginRequestMessage.setPolicyName(attestationPolicy.getName());
        pluginRequestMessage.setAdditionalInputs(JsonNodeFactory.instance.nullNode());
        pluginRequestMessage.setPropertyData(propertyData);
        pluginRequestMessage.setSourceEntity(entityName);
        pluginRequestMessage.setSourceOSID(entityId);
        pluginRequestMessage.setAttestationOSID(attestationOSID);
        pluginRequestMessage.setAttestationType(attestationPolicy.getType().name());
        pluginRequestMessage.setAttestorPlugin(attestationPolicy.getAttestorPlugin());
        pluginRequestMessage.setAttestorEntity(attestationPolicy.getAttestorEntity());
        pluginRequestMessage.setAttestorSignin(attestationPolicy.getAttestorSignin());
        pluginRequestMessage.setConditions(condition);
        pluginRequestMessage.setStatus(Action.RAISE_CLAIM.name());
        return pluginRequestMessage;
    }
}
