package dev.sunbirdrc.registry.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.sunbirdrc.elastic.IElasticService;
import dev.sunbirdrc.pojos.APIMessage;
import dev.sunbirdrc.pojos.AuditRecord;
import dev.sunbirdrc.pojos.Filter;
import dev.sunbirdrc.pojos.FilterOperators;
import dev.sunbirdrc.pojos.SearchQuery;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.util.RecordIdentifier;

import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_TYPE;

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

    @Autowired
    private APIMessage apiMessage;

    @Autowired
    private IAuditService auditService;

    @Value("${search.offset}")
    private int offset;

    @Value("${search.limit}")
    private int limit;

    @Value("${database.uuidPropertyName}")
    private String uuidPropertyName;

    @Value("${audit.enabled}")
    private boolean auditEnabled;
    
    @Value("${audit.frame.suffix}")
    private String auditSuffix;

    @Value("${registry.expandReference}")
    private boolean expandReferenceObj;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public JsonNode search(JsonNode inputQueryNode) throws IOException {
        logger.debug("search request body = " + inputQueryNode);

        SearchQuery searchQuery = getSearchQuery(inputQueryNode, offset, limit);

        Filter uuidFilter = getUUIDFilter(searchQuery, uuidPropertyName);
        
        // Fetch only Active records
        updateStatusFilter(searchQuery);
        
        boolean isSpecificSearch = (uuidFilter != null);
        if (isSpecificSearch) {
            RecordIdentifier recordIdentifier = RecordIdentifier.parse(uuidFilter.getValue().toString());

            if (!uuidFilter.getValue().equals(recordIdentifier.getUuid())) {
                // value is not just uuid and so trim out
                uuidFilter.setValue(recordIdentifier.getUuid());
            }
        }

        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        for(String indexName : searchQuery.getEntityTypes()){
            try{
                JsonNode searchedNode =  elasticService.search(indexName.toLowerCase(), searchQuery);
                if(expandReferenceObj) {
                    searchedNode = expandReference(searchedNode);
                }
                resultNode.set(indexName, searchedNode);
            }
            catch (Exception e) {
                logger.error("Elastic search operation - {}", e);
            }
        }

        try {
            auditService.auditElasticSearch( new AuditRecord().setUserId(apiMessage.getUserID()),
                    searchQuery.getEntityTypes(), inputQueryNode);
        } catch (Exception e) {
            logger.error("Exception while auditing " + e);
        }

        return resultNode;

    }

    private ArrayNode expandReference(JsonNode searchedNode) {
        ArrayNode arrayNode = (ArrayNode) searchedNode;
        ArrayNode nodeWithExpandedReference = JsonNodeFactory.instance.arrayNode();
        for (JsonNode node : arrayNode) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> removableReferenceKeys = new ArrayList<>();
            objectNode.fields().forEachRemaining(objectField -> {
                if(objectField.getValue().asText().startsWith("did:")) {
                    String[] splits = objectField.getValue().asText().split(":");
                    String indexName = splits[1].toLowerCase();
                    String osid = splits[2];
                    SearchQuery searchQuery = null;
                    ArrayNode referenceNode = null;
                    try {
                        searchQuery = getSearchQuery(splits[1], osid);
                        referenceNode = (ArrayNode) elasticService.search(indexName, searchQuery);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if(referenceNode.size() > 0) {
                        objectField.setValue(referenceNode.get(0));
                    } else {
                        removableReferenceKeys.add(objectField.getKey());
                    }
                }
            });
            for (String referenceKeys: removableReferenceKeys) {
                objectNode.remove(referenceKeys);
            }
            nodeWithExpandedReference.add(objectNode);
        }
        return nodeWithExpandedReference;
    }

    private SearchQuery getSearchQuery(String entityName, String osid) throws JsonProcessingException {
        String filter = "{\"filters\": {\"osid\":{ \"eq\":\"" + osid + "\"}}}";
        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(filter);
        ArrayNode entity = JsonNodeFactory.instance.arrayNode();
        entity.add(entityName);
        jsonNode.set(ENTITY_TYPE, entity);
        SearchQuery searchQuery1 = getSearchQuery(jsonNode, offset, limit);
        return searchQuery1;
    }

    private void updateStatusFilter(SearchQuery searchQuery) {
        List<Filter> filterList = searchQuery.getFilters();
        Filter filter = new Filter(Constants.STATUS_KEYWORD, FilterOperators.neq, Constants.STATUS_INACTIVE);
        filterList.add(filter);
	}

}
