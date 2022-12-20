package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.actors.factory.PluginRouter;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.dto.ConsentDTO;
import dev.sunbirdrc.registry.exception.ConsentForbiddenException;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.middleware.util.OSSystemFields;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_TYPE;
import static dev.sunbirdrc.registry.middleware.util.Constants.FILTERS;

@Component
public class ConsentRequestClient {

    private final String consentUrl;
    private final RestTemplate restTemplate;

    @Autowired
    IDefinitionsManager definitionsManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegistryHelper registryHelper;

    public ConsentRequestClient(@Value("${consent.url}") String consentUrl, RestTemplate restTemplate) {
        this.consentUrl = consentUrl;
        this.restTemplate = restTemplate;
    }

    public JsonNode searchUser(String entityName, String userId) throws Exception {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.set(ENTITY_TYPE, JsonNodeFactory.instance.arrayNode().add(entityName));
        ObjectNode filters = JsonNodeFactory.instance.objectNode();
        filters.set(OSSystemFields.osOwner.toString(), JsonNodeFactory.instance.objectNode().put("contains", userId));
        payload.set(FILTERS, filters);
        return registryHelper.searchEntity(payload);
    }

    public void addConsent(ConsentDTO consentDTO, HttpServletRequest request) throws Exception {
        if(!(definitionsManager.getDefinition(consentDTO.getEntityName()) != null &&
                isAllConsentFieldsInPrivate(consentDTO.getConsentFieldsPath(), consentDTO.getEntityName()))) {
            throw new ConsentForbiddenException("Consent cannot be requested on these fields");
        }
        final String attestorPlugin = "did:internal:ConsentPluginActor";
        PluginRequestMessage pluginRequestMessage = PluginRequestMessage.builder().build();
        pluginRequestMessage.setAttestorPlugin(attestorPlugin);
        pluginRequestMessage.setConsentEntityName(consentDTO.getEntityName());
        pluginRequestMessage.setConsentEntityId(consentDTO.getEntityId());
        pluginRequestMessage.setExpirationTime(consentDTO.getConsentExpiryTime());
        pluginRequestMessage.setSourceEntity(consentDTO.getRequestorName());
        pluginRequestMessage.setUserId(registryHelper.getKeycloakUserId(request));
        pluginRequestMessage.setConsentFieldPath(consentDTO.getConsentFieldsPath());
        pluginRequestMessage.setConsentEntityOsOwner(consentDTO.getOsOwner());
        PluginRouter.route(pluginRequestMessage);
    }

    private boolean isAllConsentFieldsInPrivate(Map<String, String> consentFieldsPath, String entityName) {
        return consentFieldsPath.keySet().stream().allMatch(field ->
                definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getPrivateFields().contains(field)
        );
    }

    public ResponseEntity<Object> grantOrRejectClaim(String consentId, String userId, JsonNode jsonNode) throws Exception {
        final String attestorPlugin = "did:internal:ConsentPluginActor";
        PluginRequestMessage pluginRequestMessage = PluginRequestMessage.builder().build();
        pluginRequestMessage.setAttestorPlugin(attestorPlugin);
        pluginRequestMessage.setUserId(userId);
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

    public JsonNode getConsentByConsentIdAndCreator(String consentId, String keycloakUserId) {
        return restTemplate.getForObject(
                consentUrl + "/api/v1/consent/" + consentId + "/" + keycloakUserId,
                JsonNode.class
        );
    }
}
