package io.opensaber.pojos.attestation.auto.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class AadharPluginAdapter implements PluginAdapter<JsonNode>{
    @Override
    public ResponseEntity<JsonNode> execute(JsonNode requestBody) {
        // TODO: have to setup the mock server
        String url = "abc.com";
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForEntity(url, requestBody, JsonNode.class);
    }
}
