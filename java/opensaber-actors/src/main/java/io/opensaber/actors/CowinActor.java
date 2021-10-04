package io.opensaber.actors;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.actors.factory.MessageFactory;
import io.opensaber.pojos.PluginRequestMessage;
import io.opensaber.pojos.PluginResponseMessage;
import io.opensaber.verifiablecredentials.CredentialService;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.util.Collections;
import java.util.Date;

public class CowinActor extends BaseActor {
    private static final String PRIVATE_KEY = "984b589e121040156838303f107e13150be4a80fc5088ccba0b0bdc9b1d89090de8777a28f8da1a74e7a13090ed974d879bf692d001cddee16e4cc9f84b60580";
    private static final String PUBLIC_KEY = "de8777a28f8da1a74e7a13090ed974d879bf692d001cddee16e4cc9f84b60580";
    private static final String DOMAIN = "opensaber.io";
    private static final String CREATOR = "opensaber";
    private static final String NONCE = "";
    private ObjectMapper objectMapper;
    private CredentialService credentialService;

    public CowinActor() {
        this.objectMapper = new ObjectMapper();
        this.credentialService = new CredentialService(PRIVATE_KEY, PUBLIC_KEY, DOMAIN, CREATOR, NONCE);
    }

    @Override
    public void onReceive(MessageProtos.Message request) throws Throwable {
        logger.debug("Received a message to Notification Actor {}", request.getPerformOperation());
        objectMapper = new ObjectMapper();
        PluginRequestMessage pluginRequestMessage = objectMapper.readValue(request.getPayload().getStringValue(), PluginRequestMessage.class);
        String cowinResponse = "{\n" +
                "  \"status\": \"verified\",\n" +
                "  \"data\": \"b-123\"\n" +
                "}";
        PluginResponseMessage pluginResponseMessage = PluginResponseMessage.builder().policyName(pluginRequestMessage.getPolicyName())
                .sourceEntity(pluginRequestMessage.getSourceEntity()).sourceOSID(pluginRequestMessage.getSourceOSID())
                .attestationOSID(pluginRequestMessage.getAttestationOSID())
                .attestorPlugin(pluginRequestMessage.getAttestorPlugin())
                .signedData(objectMapper.writeValueAsString(credentialService.sign(cowinResponse)))
                .additionalData(Collections.emptyMap())
                .status("")
                .date(new Date())
                .validUntil(new Date())
                .version("").build();
        logger.info("{}", pluginRequestMessage);
        MessageProtos.Message esProtoMessage = MessageFactory.instance().createPluginResponseMessage(pluginResponseMessage);
        ActorCache.instance().get(Router.ROUTER_NAME).tell(esProtoMessage, null);
    }

    @Override
    public void onFailure(MessageProtos.Message message) {
        logger.info("Send hello failed {}", message.toString());
    }

    @Override
    public void onSuccess(MessageProtos.Message message) {
        logger.info("Send hello answered successfully {}", message.toString());
    }

}
