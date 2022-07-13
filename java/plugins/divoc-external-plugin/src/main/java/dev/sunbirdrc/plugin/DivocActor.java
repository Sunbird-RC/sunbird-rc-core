package dev.sunbirdrc.plugin;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.actors.factory.MessageFactory;
import dev.sunbirdrc.plugin.dto.VerifyRequest;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import dev.sunbirdrc.pojos.PluginResponseMessageCreator;
import dev.sunbirdrc.pojos.attestation.Action;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DivocActor extends BaseActor {
    private static final String VERIFY_URL = System.getenv("verify_url");
    private static final String PUBLIC_KEY = System.getenv("divoc_public_key");
    private static final String SIGNED_KEY_TYPE = System.getenv("divoc_key_type");
    private ObjectMapper objectMapper;


    public DivocActor() {
        this.objectMapper = new ObjectMapper();

    }

    @Override
    public void onReceive(MessageProtos.Message request) throws Throwable {
        logger.debug("Received a message to Notification Actor {}", request.getPerformOperation());
        objectMapper = new ObjectMapper();
        PluginRequestMessage pluginRequestMessage = objectMapper.readValue(request.getPayload().getStringValue(), PluginRequestMessage.class);
        Object signedData = pluginRequestMessage.getAdditionalInputs().get("signedCredentials");
        Map verificationResponse = callW3cVerifyAPI(VerifyRequest.builder().publicKey(PUBLIC_KEY).signedCredentials(signedData).signingKeyType(SIGNED_KEY_TYPE).build());
        PluginResponseMessage pluginResponseMessage = PluginResponseMessageCreator.createPluginResponseMessage(pluginRequestMessage);
        if ((Boolean) verificationResponse.get("verified")) {
            pluginResponseMessage.setStatus(Action.GRANT_CLAIM.name());
            pluginResponseMessage.setResponse(objectMapper.writeValueAsString(((List) verificationResponse.get("results")).get(0)));
        } else {
            pluginResponseMessage.setStatus(Action.REJECT_CLAIM.name());
            pluginResponseMessage.setResponse("");
        }
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

    private Map callW3cVerifyAPI(VerifyRequest verifyRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<VerifyRequest> entity = new HttpEntity<>(verifyRequest, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> responseEntity = restTemplate.exchange(VERIFY_URL, HttpMethod.POST, entity, Map.class);
        logger.info("Verification api call's status {}", responseEntity.getStatusCode());
        return responseEntity.getBody();
    }

}