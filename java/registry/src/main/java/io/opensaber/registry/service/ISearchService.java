package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface ISearchService {

    JsonNode search(JsonNode inputQueryNode);

}
