package dev.sunbirdrc.pojos;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public class PluginRequestMessageCreator {
    public static PluginRequestMessage create(String propertyData,
                                              String condition,
                                              String attestationUUID,
                                              String entityName,
                                              String userId,
                                              String entityId, JsonNode additionalInput, String status,
                                              String name, String attestorPlugin, String attestorEntity,
                                              String attestorSignin, Map<String, List<String>> propertiesUUIDs, String emailId) {

        PluginRequestMessage pluginRequestMessage = new PluginRequestMessage();
        pluginRequestMessage.setPolicyName(name);
        pluginRequestMessage.setAdditionalInputs(additionalInput);
        pluginRequestMessage.setPropertyData(propertyData);
        pluginRequestMessage.setSourceEntity(entityName);
        pluginRequestMessage.setSourceUUID(entityId);
        pluginRequestMessage.setAttestationUUID(attestationUUID);
        pluginRequestMessage.setAttestorPlugin(attestorPlugin);
        pluginRequestMessage.setAttestorEntity(attestorEntity);
        pluginRequestMessage.setAttestorSignin(attestorSignin);
        pluginRequestMessage.setConditions(condition);
        pluginRequestMessage.setStatus(status);
        pluginRequestMessage.setUserId(userId);
        pluginRequestMessage.setPropertiesUUID(propertiesUUIDs);
        pluginRequestMessage.setEmailId(emailId);
        return pluginRequestMessage;
    }

}
