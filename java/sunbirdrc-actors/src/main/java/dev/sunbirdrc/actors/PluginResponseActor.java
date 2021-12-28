package dev.sunbirdrc.actors;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import dev.sunbirdrc.pojos.ResponseParams;
import dev.sunbirdrc.pojos.attestation.Action;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

// TODO: autowire signature service
public class PluginResponseActor extends BaseActor {
    private static final String SYSTEM_PROPERTY_URL = "/api/v1/%s/%s/attestation/%s/%s";
    private static final String REGISTRY_HOST_URL = "http://localhost:8081";
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onReceive(MessageProtos.Message request) throws Throwable {
        logger.debug("Received a message to PluginResponse Actor {}", request.getPerformOperation());
        PluginResponseMessage pluginResponseMessage = objectMapper.readValue(request.getPayload().getStringValue(), PluginResponseMessage.class);

        if(Action.GRANT_CLAIM.equals(Action.valueOf(pluginResponseMessage.getStatus()))) {
            JsonNode response = objectMapper.readTree(pluginResponseMessage.getResponse());
            pluginResponseMessage.setResponse(response.toString());
        }
        else if(Action.SELF_ATTEST.equals(Action.valueOf(pluginResponseMessage.getStatus()))) {
            String response = pluginResponseMessage.getResponse();
            pluginResponseMessage.setResponse(response);
        }
        logger.info("{}", pluginResponseMessage);
        callUpdateAttestationAPI(pluginResponseMessage);
    }

    @Override
    public void onFailure(MessageProtos.Message message) {
        logger.info("Send hello failed {}", message.toString());
    }

    @Override
    public void onSuccess(MessageProtos.Message message) {
        logger.info("Send hello answered successfully {}", message.toString());
    }

    private void callUpdateAttestationAPI(PluginResponseMessage pluginResponseMessage){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PluginResponseMessage> entity = new HttpEntity<>(pluginResponseMessage, headers);
        String uri = String.format(SYSTEM_PROPERTY_URL, pluginResponseMessage.getSourceEntity(), pluginResponseMessage.getSourceOSID(), pluginResponseMessage.getPolicyName(), pluginResponseMessage.getAttestationOSID());
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ResponseParams> responseEntity = restTemplate.exchange(REGISTRY_HOST_URL + uri, HttpMethod.PUT, entity, ResponseParams.class);
        logger.info("Update status api call's status {}", responseEntity.getStatusCode());
    }

}
