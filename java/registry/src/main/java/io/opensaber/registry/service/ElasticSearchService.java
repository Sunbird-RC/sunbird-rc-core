package io.opensaber.registry.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.opensaber.elastic.IElasticService;
import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.AuditRecord;
import io.opensaber.pojos.Filter;
import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.util.RecordIdentifier;

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

    @Override
    public JsonNode search(JsonNode inputQueryNode) throws IOException {
        logger.debug("search request body = " + inputQueryNode);

        SearchQuery searchQuery = getSearchQuery(inputQueryNode, offset, limit);

        Filter uuidFilter = getUUIDFilter(searchQuery, uuidPropertyName);
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
                logger.error("Elastic search operation - {}", e);
            }
        }
        
        //if Audit enabled in configuration yml file
        if(auditEnabled) {
        	String operation = Constants.AUDIT_ACTION_SEARCH_OP;
        	String action = Constants.AUDIT_ACTION_SEARCH;
        	if(!(searchQuery.getEntityTypes().get(0).contains(auditSuffix))) {
        		operation = Constants.AUDIT_ACTION_AUDIT_OP;
        		action = Constants.AUDIT_ACTION_AUDIT;
        	}
        	
	        AuditRecord auditRecord = auditService.createAuditRecord(apiMessage.getUserID(), action, "", null);
	        auditRecord.setAuditInfo(auditService.createAuditInfo(operation, action, null, inputQueryNode, searchQuery.getEntityTypes()));
	        auditService.doAudit(auditRecord, inputQueryNode, searchQuery.getEntityTypes(), null, null);
        }
 
        return resultNode;

    }

}
