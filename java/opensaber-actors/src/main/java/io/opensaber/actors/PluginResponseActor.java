package io.opensaber.actors;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.pojos.PluginResponseMessage;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.pojos.attestation.Action;
import io.opensaber.verifiablecredentials.CredentialService;
import org.springframework.http.*;
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
        logger.debug("Received a message to PluginResponse Actor {}", request.getPerformOperation());
        PluginResponseMessage pluginResponseMessage = objectMapper.readValue(request.getPayload().getStringValue(), PluginResponseMessage.class);
        CredentialService credentialService = new CredentialService(CredentialConstants.PRIVATE_KEY, CredentialConstants.PUBLIC_KEY, CredentialConstants.DOMAIN, CredentialConstants.CREATOR, CredentialConstants.NONCE);
        if(Action.GRANT_CLAIM.equals(Action.valueOf(pluginResponseMessage.getStatus()))) {
            pluginResponseMessage.setSignedData(objectMapper.writeValueAsString(credentialService.sign(pluginResponseMessage.getSignedData())));
        }
        // TODO: check what is the status for verified response of cowin actor`

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
        ResponseEntity<ResponseParams> responseEntity = restTemplate.exchange("http://localhost:8081" + uri, HttpMethod.PUT, entity, ResponseParams.class);
        logger.info("Update status api call's status {}", responseEntity.getStatusCode());
    }

}
