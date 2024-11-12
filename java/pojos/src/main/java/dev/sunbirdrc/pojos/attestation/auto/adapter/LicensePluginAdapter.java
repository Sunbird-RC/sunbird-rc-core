package dev.sunbirdrc.pojos.attestation.auto.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class LicensePluginAdapter implements PluginAdapter<JsonNode> {
    @Override
    public ResponseEntity<JsonNode> execute(JsonNode requestBody) {
        // TODO: have to setup the mock server
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ObjectMapper().createObjectNode());
    }
}
