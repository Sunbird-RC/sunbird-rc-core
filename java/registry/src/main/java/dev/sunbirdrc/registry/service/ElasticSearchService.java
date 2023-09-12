package dev.sunbirdrc.registry.service;

import java.io.IOException;
import java.util.List;

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
                JsonNode node = elasticService.search(indexName.toLowerCase(), searchQuery);
                resultNode.set(indexName, node);
            }
            catch (Exception e) {
                logger.error("Exception in Elastic search operation: {}", e.getMessage());
            }
        }

        try {
            auditService.auditElasticSearch( new AuditRecord().setUserId(apiMessage.getUserID()),
                    searchQuery.getEntityTypes(), inputQueryNode);
        } catch (Exception e) {
            logger.error("Exception while auditing: {}", e.getMessage());
        }

        return resultNode;

    }

	private void updateStatusFilter(SearchQuery searchQuery) {		
        List<Filter> filterList = searchQuery.getFilters();
        Filter filter = new Filter(Constants.STATUS_KEYWORD, FilterOperators.neq, Constants.STATUS_INACTIVE);
        filterList.add(filter);
	}

}
