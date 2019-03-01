package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.elastic.IElasticService;
import io.opensaber.pojos.SearchQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This class provide search option with Elastic search Hits elastic search
 * database to operate
 *
 */
@Component
public class ElasticSearchService implements ISearchService {

    @Autowired
    private IElasticService elasticService;

    @Value("${search.offset}")
    private int offset;

    @Value("${search.limit}")
    private int limit;


    @Override
    public JsonNode search(JsonNode inputQueryNode) {
        // calls the Elastic search
        SearchQuery searchQuery = getSearchQuery(inputQueryNode, offset, limit);
        String indexName = inputQueryNode.fieldNames().next().toLowerCase();
        return elasticService.search(indexName, searchQuery);

    }

}
