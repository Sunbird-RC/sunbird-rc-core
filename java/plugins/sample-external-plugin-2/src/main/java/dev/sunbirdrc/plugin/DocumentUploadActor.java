package dev.sunbirdrc.plugin;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.actors.factory.MessageFactory;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import dev.sunbirdrc.pojos.PluginResponseMessageCreator;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static dev.sunbirdrc.pojos.attestation.Action.*;
import static java.net.URLDecoder.decode;

public class DocumentUploadActor extends BaseActor {

    @Override
    public void onReceive(MessageProtos.Message request) throws Throwable {
        String payLoad = request.getPayload().getStringValue();
        ObjectMapper objectMapper = new ObjectMapper();
        PluginRequestMessage pluginRequestMessage = objectMapper.readValue(payLoad, PluginRequestMessage.class);
        logger.info("Received request message {} ", pluginRequestMessage);
        JsonNode additionalInput = pluginRequestMessage.getAdditionalInputs();

        PluginResponseMessage pluginResponseMessage = PluginResponseMessageCreator.createPluginResponseMessage(pluginRequestMessage);
        pluginResponseMessage.setStatus(SELF_ATTEST.name());

        ArrayNode signedMd5s = JsonNodeFactory.instance.arrayNode();
        if(additionalInput.has("fileUrl")) {
            ArrayNode fileUrls = (ArrayNode)(additionalInput.get("fileUrl"));
            for (JsonNode fileUrl : fileUrls) {
                String md5;
                RestTemplate restTemplate = new RestTemplate();
                String decodedUrl = decode(fileUrl.asText(), StandardCharsets.UTF_8.name());
                ResponseEntity<byte[]> fileResponse = restTemplate.getForEntity(decodedUrl, byte[].class);
                byte[] content = fileResponse.getBody();
                md5 = content == null ? "" : DigestUtils.md5DigestAsHex(content);
                signedMd5s.add(md5);
            }
        }
        ObjectNode md5Response = JsonNodeFactory.instance.objectNode();
        md5Response.set("md5", signedMd5s);
        pluginResponseMessage.setResponse(objectMapper.writeValueAsString(md5Response));
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
