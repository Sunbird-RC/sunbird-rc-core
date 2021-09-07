package io.opensaber.pojos.attestation.auto.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;

public interface PluginAdapter<T> {
    ResponseEntity<T> execute(JsonNode requestBody);
}
