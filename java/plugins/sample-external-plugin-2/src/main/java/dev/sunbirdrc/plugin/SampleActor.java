package dev.sunbirdrc.plugin;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.actors.factory.MessageFactory;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.util.Collections;
import java.util.Date;

public class SampleActor extends BaseActor {
    private ObjectMapper objectMapper;


    public SampleActor() {
        this.objectMapper = new ObjectMapper();

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
                .response(cowinResponse)
                .additionalData(JsonNodeFactory.instance.nullNode())
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
