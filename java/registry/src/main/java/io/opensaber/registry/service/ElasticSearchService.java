package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.elastic.IElasticService;
import io.opensaber.pojos.SearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static Logger logger = LoggerFactory.getLogger(ElasticSearchService.class);

    @Autowired
    private IElasticService elasticService;

    @Value("${search.offset}")
    private int offset;

    @Value("${search.limit}")
    private int limit;


    @Override
    public JsonNode search(JsonNode inputQueryNode) {
        logger.debug("search request body = " + inputQueryNode);
        SearchQuery searchQuery = getSearchQuery(inputQueryNode, offset, limit);
        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        for(String indexName : searchQuery.getEntityTypes()){
            JsonNode node = elasticService.search(indexName.toLowerCase(), searchQuery);
            resultNode.set(indexName, node);
        }
        return resultNode;

    }

}
