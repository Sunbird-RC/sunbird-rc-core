package io.opensaber.actors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import io.opensaber.pojos.attestation.auto.AutoAttestationMessage;
import io.opensaber.pojos.attestation.auto.AutoAttestationPolicy;
import io.opensaber.pojos.attestation.auto.PluginType;
import io.opensaber.pojos.attestation.auto.adapter.PluginAdapter;
import io.opensaber.pojos.attestation.auto.adapter.PluginFactory;
import io.opensaber.registry.middleware.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Optional;

public class AutoAttestorActor extends BaseActor {
    private static final Logger logger = LoggerFactory.getLogger(AutoAttestorActor.class);

    @Override
    protected void onReceive(MessageProtos.Message request) throws Throwable {
        String payLoad = request.getPayload().getStringValue();
        AutoAttestationMessage autoAttestationMessage = new ObjectMapper().readValue(payLoad,
                AutoAttestationMessage.class);
        AutoAttestationPolicy autoAttestationPolicy = autoAttestationMessage.getAutoAttestationPolicy();

        JsonNode input = autoAttestationMessage.getInput();
        ResponseEntity<JsonNode> responseEntity = getPluginResponse(autoAttestationPolicy, input);
        Optional<ObjectNode> nodeRefOptional = getParentNode(input, autoAttestationPolicy.getNodeRef());

        if(nodeRefOptional.isPresent()) {
            ObjectNode nodeRef = nodeRefOptional.get();
            nodeRef.put("valid", responseEntity.getStatusCode().is2xxSuccessful());
            updateObject(autoAttestationMessage, nodeRef);
        } else {
            throw new Exception("Property not found");
        }
    }

    private ResponseEntity<JsonNode> getPluginResponse(AutoAttestationPolicy autoAttestationPolicy, JsonNode input) {
        String typePath = autoAttestationPolicy.getTypePath();
        String valuePath = autoAttestationPolicy.getValuePath();
        PluginType type = PluginType.valueOf(JSONUtil.readValFromJsonTree(typePath, input));
        PluginAdapter<JsonNode> adapter = PluginFactory.getAdapter(type);
        String value = JSONUtil.readValFromJsonTree(valuePath, input);
        JsonNode  requestBody = buildRequestBody(value);
        return adapter.execute(requestBody);
    }

    private void updateObject(AutoAttestationMessage autoAttestationMessage, ObjectNode nodeRef) {
        AutoAttestationPolicy autoAttestationPolicy = autoAttestationMessage.getAutoAttestationPolicy();
        String property = autoAttestationPolicy.getProperty();
        String propertyId = nodeRef.get("osid").asText();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + autoAttestationMessage.getAccessToken());
        HttpEntity<JsonNode> entity = new HttpEntity<>(nodeRef, headers);

        String uri = String.format("/api/v1/system/%s/%s", property, propertyId);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Object> responseEntity = restTemplate.postForEntity(autoAttestationMessage.getUrl() + uri, entity, Object.class);

        logger.info("Updated the status {}", responseEntity.getStatusCode());
    }

    private Optional<ObjectNode> getParentNode(JsonNode input, String nodeRef) throws IOException {
        Object result = JsonPath.parse(input.toString()).read(nodeRef);
        if(result.getClass().equals(LinkedHashMap.class)) {
            return Optional.of((ObjectNode) JSONUtil.convertObjectJsonNode(result));
        }
        return Optional.empty();
    }

    private JsonNode buildRequestBody(String value) {
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("value", value);
        return objectNode;
    }

}
