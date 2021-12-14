package io.opensaber.plugin;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.actors.factory.MessageFactory;
import io.opensaber.pojos.PluginRequestMessage;
import io.opensaber.pojos.PluginResponseMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static io.opensaber.pojos.attestation.Action.*;
import static java.net.URLDecoder.decode;

public class DocumentUploadActor extends BaseActor {

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
        pluginResponseMessage.setStatus(SELF_ATTEST.name());

        String md5 = "";
        if(additionalInput.has("fileUrl")) {
            RestTemplate restTemplate = new RestTemplate();
            String fileUrl = additionalInput.get("fileUrl").asText();
            String decodedUrl = decode(fileUrl, StandardCharsets.UTF_8.name());
            ResponseEntity<byte[]> fileResponse = restTemplate.getForEntity(decodedUrl, byte[].class);
            byte[] content = fileResponse.getBody();
            md5 = content == null ? "" : DigestUtils.md5DigestAsHex(content);
        }
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
