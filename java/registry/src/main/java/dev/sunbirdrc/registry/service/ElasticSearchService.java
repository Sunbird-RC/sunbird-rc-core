package dev.sunbirdrc.registry.service;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

import static dev.sunbirdrc.registry.middleware.util.Constants.*;

/**
 * This class provide search option with Elastic search Hits elastic search
 * database to operate
 *
 */
@Component
@ConditionalOnProperty(name = "search.providerName", havingValue = "dev.sunbirdrc.registry.service.ElasticSearchService")
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
    public JsonNode search(JsonNode inputQueryNode, String userId) throws IOException {
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
                logger.error("Exception in Elastic search operation: {}", ExceptionUtils.getStackTrace(e));
            }
        }

        try {
            if(userId == null) userId = apiMessage.getUserID();
            auditService.auditElasticSearch( new AuditRecord().setUserId(userId),
                    searchQuery.getEntityTypes(), inputQueryNode);
        } catch (Exception e) {
            logger.error("Exception while auditing: {}", ExceptionUtils.getStackTrace(e));
        }

        return resultNode;

    }

    private ArrayNode expandReference(JsonNode searchedNode) {
        ArrayNode arrayNode = (ArrayNode) searchedNode;
        ArrayNode nodeWithExpandedReference = JsonNodeFactory.instance.arrayNode();
        HashMap<String, List<String>> indexUuidsMap = new HashMap<>();
        for (JsonNode node : arrayNode) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(objectField -> {
                String pattern = "^"+ DID_TYPE+":[^:]+:[^:]+";
                if(objectField.getValue().asText().matches(pattern)) {
                    String[] referenceStrSplit = objectField.getValue().asText().split(":");
                    String indexName = referenceStrSplit[1].toLowerCase();
                    String uuidPropertyValue = referenceStrSplit[2];
                    List<String> uuidPropertyValues;
                    if(indexUuidsMap.get(indexName) == null) {
                        uuidPropertyValues = new ArrayList();
                    } else {
                        uuidPropertyValues = indexUuidsMap.get(indexName);
                    }
                    uuidPropertyValues.add(uuidPropertyValue);
                    indexUuidsMap.put(indexName, uuidPropertyValues);
                }
            });
        }
        SearchQuery searchQuery = null;
        ArrayNode referenceNodes = JsonNodeFactory.instance.arrayNode();
        for (Map.Entry<String, List<String>> indexUuidPropertyEntry: indexUuidsMap.entrySet()) {
            try {
                searchQuery = getSearchQuery(indexUuidPropertyEntry.getKey(), indexUuidPropertyEntry.getValue());
                referenceNodes.addAll((ArrayNode) elasticService.search(indexUuidPropertyEntry.getKey(), searchQuery));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (JsonNode node : arrayNode) {
            ObjectNode objectNode = (ObjectNode) node;
            ArrayNode finalReferenceNodes = referenceNodes;
            objectNode.fields().forEachRemaining(objectField -> {
                if (objectField.getValue().asText().startsWith("did:")) {
                    String[] referenceStrSplit = objectField.getValue().asText().split(":");
                    String uuidPropertyValue = referenceStrSplit[2];
                    for(JsonNode referenceNode: finalReferenceNodes) {
                        if(referenceNode.get(uuidPropertyName).textValue().contains(uuidPropertyValue)) {
                            objectNode.set(objectField.getKey(), referenceNode);
                        }
                    }
                }
            });
            nodeWithExpandedReference.add(objectNode);
        }
        return nodeWithExpandedReference;
    }

    private SearchQuery getSearchQuery(String entityName, List<String> uuidPropertyValues) throws JsonProcessingException {
        ArrayNode uuidPropertyValuesArrayNode = JsonNodeFactory.instance.arrayNode();
        for (String uuidPropertyValue: uuidPropertyValues) {
            uuidPropertyValuesArrayNode.add("1-"+uuidPropertyValue);
        }
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        ObjectNode conditionNode = JsonNodeFactory.instance.objectNode();
        conditionNode.set("or", uuidPropertyValuesArrayNode);
        ObjectNode uuidPropertyValueFilterNode = JsonNodeFactory.instance.objectNode();
        uuidPropertyValueFilterNode.set(uuidPropertyName, conditionNode);
        objectNode.set(FILTERS, uuidPropertyValueFilterNode);
        ArrayNode entity = JsonNodeFactory.instance.arrayNode();
        entity.add(entityName);
        objectNode.set(ENTITY_TYPE, entity);
        return getSearchQuery(objectNode, offset, limit);
    }

    private void updateStatusFilter(SearchQuery searchQuery) {
        List<Filter> filterList = searchQuery.getFilters();
        Filter filter = new Filter(Constants.STATUS_KEYWORD, FilterOperators.neq, Constants.STATUS_INACTIVE);
        filterList.add(filter);
	}

}
