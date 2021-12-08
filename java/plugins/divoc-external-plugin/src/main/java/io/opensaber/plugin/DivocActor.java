package io.opensaber.plugin;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.actors.factory.MessageFactory;
import io.opensaber.plugin.dto.VerifyRequest;
import io.opensaber.pojos.PluginRequestMessage;
import io.opensaber.pojos.PluginResponseMessage;
import io.opensaber.pojos.attestation.Action;
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
    private static final String VERIFY_URL = "http://localhost:4324/verify";
    private static final String PUBLIC_KEY = "7mQq3uQuK3AL5mPhhCKEHXpdMSHCRMtZntxMMn7YQrY3";
    private static final String SIGNED_KEY_TYPE = "ED25519";
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
        PluginResponseMessage pluginResponseMessage = PluginResponseMessage.builder().policyName(pluginRequestMessage.getPolicyName())
                .sourceEntity(pluginRequestMessage.getSourceEntity()).sourceOSID(pluginRequestMessage.getSourceOSID())
                .attestationOSID(pluginRequestMessage.getAttestationOSID())
                .attestorPlugin(pluginRequestMessage.getAttestorPlugin())
                .additionalData(JsonNodeFactory.instance.nullNode())
                .date(new Date())
                .validUntil(new Date())
                .version("").build();
        if ((Boolean) verificationResponse.get("verified")) {
            pluginResponseMessage.setStatus(Action.GRANT_CLAIM.name());
            pluginResponseMessage.setResponse(objectMapper.writeValueAsString(((List)verificationResponse.get("results")).get(0)));
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