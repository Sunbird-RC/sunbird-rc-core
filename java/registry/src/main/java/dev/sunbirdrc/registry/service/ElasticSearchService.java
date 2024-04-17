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

import static dev.sunbirdrc.registry.middleware.util.Constants.DID_TYPE;
import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_TYPE;

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
        HashMap<String, List<String>> indexOsidsMap = new HashMap<>();
        for (JsonNode node : arrayNode) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(objectField -> {
                String pattern = "^"+ DID_TYPE+":[^:]+:[^:]+";
                if(objectField.getValue().asText().matches(pattern)) {
                    String[] referenceStrSplit = objectField.getValue().asText().split(":");
                    String indexName = referenceStrSplit[1].toLowerCase();
                    String osid = referenceStrSplit[2];
                    List<String> osids;
                    if(indexOsidsMap.get(indexName) == null) {
                        osids = new ArrayList();
                    } else {
                        osids = indexOsidsMap.get(indexName);
                    }
                    osids.add(osid);
                    indexOsidsMap.put(indexName, osids);
                }
            });
        }
        SearchQuery searchQuery = null;
        ArrayNode referenceNodes = JsonNodeFactory.instance.arrayNode();
        for (Map.Entry<String, List<String>> indexOsidEntry: indexOsidsMap.entrySet()) {
            try {
                searchQuery = getSearchQuery(indexOsidEntry.getKey(), indexOsidEntry.getValue());
                referenceNodes.addAll((ArrayNode) elasticService.search(indexOsidEntry.getKey(), searchQuery));
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
                    String osid = referenceStrSplit[2];
                    for(JsonNode referenceNode: finalReferenceNodes) {
                        if(referenceNode.get("osid").textValue().contains(osid)) {
                            objectNode.set(objectField.getKey(), referenceNode);
                        }
                    }
                }
            });
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

    private SearchQuery getSearchQuery(String entityName, List<String> osids) throws JsonProcessingException {
        ArrayNode osidsArrayNode = JsonNodeFactory.instance.arrayNode();
        for (String osid: osids) {
            osidsArrayNode.add("1-"+osid);
        }
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        ObjectNode conditionNode = JsonNodeFactory.instance.objectNode();
        conditionNode.set("or", osidsArrayNode);
        ObjectNode osidFilterNode = JsonNodeFactory.instance.objectNode();
        osidFilterNode.set("osid", conditionNode);
        objectNode.set("filters", osidFilterNode);
        ArrayNode entity = JsonNodeFactory.instance.arrayNode();
        entity.add(entityName);
        objectNode.set(ENTITY_TYPE, entity);
        SearchQuery searchQuery1 = getSearchQuery(objectNode, offset, limit);
        return searchQuery1;
    }

    private void updateStatusFilter(SearchQuery searchQuery) {
        List<Filter> filterList = searchQuery.getFilters();
        Filter filter = new Filter(Constants.STATUS_KEYWORD, FilterOperators.neq, Constants.STATUS_INACTIVE);
        filterList.add(filter);
	}

}
