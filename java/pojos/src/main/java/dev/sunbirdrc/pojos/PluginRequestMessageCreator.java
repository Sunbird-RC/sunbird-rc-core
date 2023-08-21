package dev.sunbirdrc.pojos;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public class PluginRequestMessageCreator {
    public static PluginRequestMessage create(String propertyData,
                                              String condition,
                                              String attestationOSID,
                                              String entityName,
                                              String userId,
                                              String entityId, JsonNode additionalInput, String status,
                                              String name, String attestorPlugin, String attestorEntity,
                                              String attestorSignin, Map<String, List<String>> propertiesOSID, String emailId, String credType) {

        PluginRequestMessage pluginRequestMessage = new PluginRequestMessage();
        pluginRequestMessage.setPolicyName(name);
        pluginRequestMessage.setAdditionalInputs(additionalInput);
        pluginRequestMessage.setPropertyData(propertyData);
        pluginRequestMessage.setSourceEntity(entityName);
        pluginRequestMessage.setSourceOSID(entityId);
        pluginRequestMessage.setAttestationOSID(attestationOSID);
        pluginRequestMessage.setAttestorPlugin(attestorPlugin);
        pluginRequestMessage.setAttestorEntity(attestorEntity);
        pluginRequestMessage.setAttestorSignin(attestorSignin);
        pluginRequestMessage.setConditions(condition);
        pluginRequestMessage.setStatus(status);
        pluginRequestMessage.setUserId(userId);
        pluginRequestMessage.setPropertiesOSID(propertiesOSID);
        pluginRequestMessage.setEmailId(emailId);
        pluginRequestMessage.setCredType(credType);
        return pluginRequestMessage;
    }

}
