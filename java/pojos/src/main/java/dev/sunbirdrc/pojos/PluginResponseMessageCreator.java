package dev.sunbirdrc.pojos;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.attestation.Action;

import java.util.Date;

public class PluginResponseMessageCreator {
	public static PluginResponseMessage createClaimResponseMessage(String claimId, Action status, PluginRequestMessage pluginRequestMessage) {
		ObjectNode additionalData = JsonNodeFactory.instance.objectNode();
		additionalData.put("claimId", claimId);
		return PluginResponseMessage.builder()
				.sourceEntity(pluginRequestMessage.getSourceEntity())
				.sourceOSID(pluginRequestMessage.getSourceOSID())
				.attestationOSID(pluginRequestMessage.getAttestationOSID())
				.attestorPlugin(pluginRequestMessage.getAttestorPlugin())
				.additionalData(additionalData)
				.policyName(pluginRequestMessage.getPolicyName())
				.status(status.name())
				.date(new Date())
				.validUntil(new Date())
				.version("")
				.propertiesOSID(pluginRequestMessage.getPropertiesOSID())
				.userId(pluginRequestMessage.getUserId())
				.emailId(pluginRequestMessage.getEmailId())
				.build();
	}

	public static PluginResponseMessage createPluginResponseMessage(PluginRequestMessage pluginRequestMessage) {
		return PluginResponseMessage.builder()
				.policyName(pluginRequestMessage.getPolicyName())
				.sourceEntity(pluginRequestMessage.getSourceEntity())
				.sourceOSID(pluginRequestMessage.getSourceOSID())
				.attestationOSID(pluginRequestMessage.getAttestationOSID())
				.attestorPlugin(pluginRequestMessage.getAttestorPlugin())
				.additionalData(JsonNodeFactory.instance.nullNode())
				.date(new Date()).validUntil(new Date()).version("")
                .build();
	}
}
