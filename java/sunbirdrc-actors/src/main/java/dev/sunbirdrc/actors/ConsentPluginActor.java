package dev.sunbirdrc.actors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.dto.ConsentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

import java.io.IOException;

public class ConsentPluginActor extends BaseActor {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RestTemplate restTemplate = new RestTemplate();
    private final String consentUrl = System.getenv().getOrDefault("consent_url", "http://localhost:8083");
    @Override
    protected void onReceive(MessageProtos.Message request) throws Throwable {
        PluginRequestMessage pluginRequestMessage = new ObjectMapper().readValue(request.getPayload().getStringValue(), PluginRequestMessage.class);
        if(!(pluginRequestMessage.getAdditionalInputs() instanceof NullNode)) {
            grantOrRejectConsent(pluginRequestMessage);
            return;
        }
        createConsent(pluginRequestMessage);
    }

    private void grantOrRejectConsent(PluginRequestMessage pluginRequestMessage) throws IOException {
        String requestBody = "{\"status\": " + "\"" + pluginRequestMessage.getStatus() + "\"" + "}";
        String consentId = pluginRequestMessage.getAdditionalInputs().get("consentId").asText();
        String consenterId = pluginRequestMessage.getUserId();
        JsonNode jsonNode = new ObjectMapper().readValue(requestBody, JsonNode.class);
        restTemplate.exchange(consentUrl + "/api/v1/consent/" + consentId + "/" + consenterId, HttpMethod.PUT,new HttpEntity<>(jsonNode), Object.class);
    }

    private void createConsent(PluginRequestMessage pluginRequestMessage) {
        ConsentDTO consentDTO = new ConsentDTO();
        consentDTO.setEntityId(pluginRequestMessage.getConsentEntityId());
        consentDTO.setEntityName(pluginRequestMessage.getConsentEntityName());
        consentDTO.setOsOwner(pluginRequestMessage.getConsentEntityOsOwner());
        consentDTO.setConsentFieldsPath(pluginRequestMessage.getConsentFieldPath());
        consentDTO.setConsentExpiryTime(pluginRequestMessage.getExpirationTime());
        consentDTO.setRequestorName(pluginRequestMessage.getSourceEntity());
        consentDTO.setRequestorId(pluginRequestMessage.getUserId());
        restTemplate.exchange(consentUrl + "/api/v1/consent", HttpMethod.POST, new HttpEntity<>(consentDTO), Object.class);
    }
}
