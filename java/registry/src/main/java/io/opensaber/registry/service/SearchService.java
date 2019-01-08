package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface SearchService {

    JsonNode search(JsonNode inputQueryNode);

}
