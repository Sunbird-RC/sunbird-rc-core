package io.opensaber.actors;


import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.jsonld.JsonLDException;
import io.opensaber.pojos.PluginResponseMessage;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.verifiablecredentials.CredentialService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Objects;


public class PluginResponseActor extends BaseActor {
    private static final String SYSTEM_PROPERTY_URL = "/api/v1/%s/%s/attestation/%s/%s";
    private static final String REGISTRY_URL = "http://localhost:8081";
    private static final String PRIVATE_KEY = "984b589e121040156838303f107e13150be4a80fc5088ccba0b0bdc9b1d89090de8777a28f8da1a74e7a13090ed974d879bf692d001cddee16e4cc9f84b60580";
    private static final String PUBLIC_KEY = "de8777a28f8da1a74e7a13090ed974d879bf692d001cddee16e4cc9f84b60580";
    private static final String DOMAIN = "opensaber.io";
    private static final String CREATOR = "opensaber";
    private static final String NONCE = "";

    private ObjectMapper objectMapper;
    private CredentialService credentialService;

    public PluginResponseActor() {
        this.objectMapper = new ObjectMapper();
        this.credentialService = new CredentialService(PRIVATE_KEY, PUBLIC_KEY, DOMAIN, CREATOR, NONCE);
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

    private void callUpdateAttestationAPI(PluginResponseMessage pluginResponseMessage) throws GeneralSecurityException, JsonLDException, ParseException, IOException {
        pluginResponseMessage.setSignedData(objectMapper.writeValueAsString(credentialService.sign(pluginResponseMessage.getResponse())));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PluginResponseMessage> entity = new HttpEntity<>(pluginResponseMessage, headers);
        String uri = String.format(SYSTEM_PROPERTY_URL, pluginResponseMessage.getSourceEntity(), pluginResponseMessage.getSourceOSID(), pluginResponseMessage.getPolicyName(), pluginResponseMessage.getAttestationOSID());
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ResponseParams> responseEntity = restTemplate.postForEntity(REGISTRY_URL + uri, entity, ResponseParams.class);
        ResponseParams responseParams = Objects.requireNonNull(responseEntity.getBody());
        logger.info("Update api response: {}", responseParams);
    }

}
