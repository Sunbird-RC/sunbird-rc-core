package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.actors.factory.PluginRouter;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.middleware.util.OSSystemFields;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_TYPE;
import static dev.sunbirdrc.registry.middleware.util.Constants.FILTERS;

@Component
public class ConsentRequestClient {

    private final String consentUrl;
    private final RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegistryHelper registryHelper;

    public ConsentRequestClient(@Value("${consent.url}") String consentUrl, RestTemplate restTemplate) {
        this.consentUrl = consentUrl;
        this.restTemplate = restTemplate;
    }

    public JsonNode getConsent(String consentId) throws Exception{
        return restTemplate.getForObject(
                consentUrl + "/api/v1/consent/" + consentId,
                JsonNode.class
        );
    }

    public JsonNode searchUser(String entityName, String userId) throws Exception {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.set(ENTITY_TYPE, JsonNodeFactory.instance.arrayNode().add(entityName));
        ObjectNode filters = JsonNodeFactory.instance.objectNode();
        filters.set(OSSystemFields.osOwner.toString(), JsonNodeFactory.instance.objectNode().put("contains", userId));
        payload.set(FILTERS, filters);
        return registryHelper.searchEntity(payload);
    }

    public void addConsent(ObjectNode objectNode, HttpServletRequest request) throws Exception {
        final String attestorPlugin = "did:internal:ConsentPluginActor";
        PluginRequestMessage pluginRequestMessage = PluginRequestMessage.builder().build();
        pluginRequestMessage.setAttestorPlugin(attestorPlugin);
        pluginRequestMessage.setConsentEntityName(objectNode.get("entityName").asText());
        pluginRequestMessage.setConsentEntityId(objectNode.get("entityId").asText());
        pluginRequestMessage.setExpirationTime(objectNode.get("consentExpiryTime").asText());
        pluginRequestMessage.setSourceEntity(objectNode.get("requestorName").asText());
        pluginRequestMessage.setUserId(registryHelper.getKeycloakUserId(request));
        pluginRequestMessage.setStatus(objectNode.get("status").asText());
        pluginRequestMessage.setConsentFieldPath(objectMapper.convertValue(objectNode.get("consentFieldsPath"), Map.class));
        pluginRequestMessage.setConsentEntityOsOwner(objectMapper.convertValue(objectNode.get("osOwner"), List.class));
        PluginRouter.route(pluginRequestMessage);
    }

    public ResponseEntity<Object> grantOrRejectClaim(String consentId, JsonNode jsonNode) throws Exception {
        final String attestorPlugin = "did:internal:ConsentPluginActor";
        PluginRequestMessage pluginRequestMessage = PluginRequestMessage.builder().build();
        pluginRequestMessage.setAttestorPlugin(attestorPlugin);
        pluginRequestMessage.setStatus(jsonNode.get("status").asText());
        String consent = "{\"consentId\" : " + "\"" + consentId + "\"" + "}";
        JsonNode additionalInput = objectMapper.readValue(consent, JsonNode.class);
        pluginRequestMessage.setAdditionalInputs(additionalInput);
        PluginRouter.route(pluginRequestMessage);
        return null;
    }

    public JsonNode getConsentByOwner(String ownerId) {
        return restTemplate.getForObject(consentUrl + "/api/v1/consent/owner/" + ownerId, JsonNode.class);
    }
}
