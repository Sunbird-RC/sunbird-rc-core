package io.opensaber.actors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
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
import java.util.Objects;
import java.util.Optional;

public class AutoAttestorActor extends BaseActor {
    private static final Logger logger = LoggerFactory.getLogger(AutoAttestorActor.class);
    private static final String SYSTEM_PROPERTY_URL = "/api/v1/system/%s/%s";
    private static final String VALUE = "value";
    private static final String UUID_PROPERTY_NAME = "osid";
    private static final String VALID_PROPERTY = "valid";

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
            nodeRef.put(VALID_PROPERTY, responseEntity.getStatusCode().is2xxSuccessful());
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
        String propertyId = nodeRef.get(UUID_PROPERTY_NAME).asText();
        logger.info("Updating the nodeRef {}", nodeRef);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", autoAttestationMessage.getAccessToken());
        HttpEntity<JsonNode> entity = new HttpEntity<>(nodeRef, headers);
        String uri = String.format(SYSTEM_PROPERTY_URL, property, propertyId);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ResponseParams> responseEntity = restTemplate.postForEntity(autoAttestationMessage.getUrl() + uri, entity, ResponseParams.class);
        ResponseParams responseParams = Objects.requireNonNull(responseEntity.getBody());

        logger.info("Updated the status {}", responseParams.getStatus());
        if(responseParams.getStatus().equals(Response.Status.UNSUCCESSFUL)) {
            logger.error("Updating the node {} is failed with error {}", nodeRef, responseParams.getErrmsg());
        }
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
        objectNode.put(VALUE, value);
        return objectNode;
    }

}
