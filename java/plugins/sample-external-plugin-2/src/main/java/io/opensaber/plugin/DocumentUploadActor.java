package io.opensaber.plugin;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.actors.factory.MessageFactory;
import io.opensaber.pojos.PluginRequestMessage;
import io.opensaber.pojos.PluginResponseMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;

import static io.opensaber.pojos.attestation.Action.GRANT_CLAIM;
import static io.opensaber.pojos.attestation.Action.REJECT_CLAIM;

public class DocumentUploadActor extends BaseActor {
    private ObjectMapper objectMapper;


    public DocumentUploadActor() {
        this.objectMapper = new ObjectMapper();

    }

    @Override
    public void onReceive(MessageProtos.Message request) throws Throwable {
        String payLoad = request.getPayload().getStringValue();
        PluginRequestMessage pluginRequestMessage = new ObjectMapper().readValue(payLoad, PluginRequestMessage.class);
        logger.info("Received request message {} ", pluginRequestMessage);
        JsonNode additionalInput = pluginRequestMessage.getAdditionalInputs();

        PluginResponseMessage pluginResponseMessage = PluginResponseMessage.builder().policyName(pluginRequestMessage.getPolicyName())
                .sourceEntity(pluginRequestMessage.getSourceEntity()).sourceOSID(pluginRequestMessage.getSourceOSID())
                .attestationOSID(pluginRequestMessage.getAttestationOSID())
                .attestorPlugin(pluginRequestMessage.getAttestorPlugin())
                .additionalData(JsonNodeFactory.instance.nullNode())
                .date(new Date())
                .validUntil(new Date())
                .version("").build();
        pluginResponseMessage.setStatus(GRANT_CLAIM.name());

        if(additionalInput.has("fileUrl")) {
            // Read from s3 and sign
        }
        // TODO: Add logic to generate hash
        String md5 = "dummy hash";
        pluginResponseMessage.setResponse(md5);
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
