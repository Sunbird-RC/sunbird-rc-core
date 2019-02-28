package io.opensaber.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.service.ISearchService;
import org.springframework.stereotype.Component;
/**
 * This class provide search option with Elastic search 
 * Hits elastic search database to operate
 *
 */
@Component
public class ElasticSearchService implements ISearchService {

    @Override
    public JsonNode search(JsonNode inputQueryNode) {
        //TODO: call the ElasticService search
        return null;
    }

}
