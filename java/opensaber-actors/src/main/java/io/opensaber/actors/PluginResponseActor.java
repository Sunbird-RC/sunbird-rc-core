package io.opensaber.actors;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.actors.services.NotificationService;
import io.opensaber.pojos.PluginRequestMessage;
import io.opensaber.pojos.PluginResponseMessage;
import io.opensaber.pojos.ResponseParams;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

import java.util.Objects;


public class PluginResponseActor extends BaseActor {
    private static final String SYSTEM_PROPERTY_URL = "/api/v1/%s/%s/attestation/%s/%s";
    private ObjectMapper objectMapper;

    public PluginResponseActor() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void onReceive(MessageProtos.Message request) throws Throwable {
        logger.debug("Received a message to Notification Actor {}", request.getPerformOperation());
        PluginResponseMessage pluginResponseMessage = objectMapper.readValue(request.getPayload().getStringValue(), PluginResponseMessage.class);
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
        ResponseEntity<ResponseParams> responseEntity = restTemplate.postForEntity("http://localhost:8081" + uri, entity, ResponseParams.class);
        ResponseParams responseParams = Objects.requireNonNull(responseEntity.getBody());
        logger.info("Update api response: {}", responseParams);
    }

}
