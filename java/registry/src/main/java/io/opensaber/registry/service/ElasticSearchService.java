package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.audit.IAuditService;
import io.opensaber.elastic.IElasticService;
import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.AuditInfo;
import io.opensaber.pojos.AuditRecord;
import io.opensaber.pojos.Filter;
import io.opensaber.pojos.FilterOperators;
import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.DateUtil;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import io.opensaber.registry.util.RecordIdentifier;
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

    @Override
    public JsonNode search(JsonNode inputQueryNode) throws IOException {
        logger.debug("search request body = " + inputQueryNode);
        AuditRecord auditRecord = new AuditRecord();
        List<AuditInfo> auditInfoLst = new LinkedList<>();
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
                if(node !=  null) {
                    AuditInfo auditInfo = new AuditInfo();
                    auditInfo.setOp(Constants.AUDIT_ACTION_SEARCH_OP);
                    auditInfo.setPath(indexName);
                    auditInfoLst.add(auditInfo);
                }
            }
            catch (Exception e) {
                logger.error("Elastic search operation - {}", e);
            }

        }
        auditRecord.setAuditInfo(auditInfoLst).setUserId(apiMessage.getUserID()).setAction(Constants.AUDIT_ACTION_SEARCH).
                setAuditId(UUID.randomUUID().toString()).setTimeStamp(DateUtil.getTimeStamp());
        auditService.audit(auditRecord);
        return resultNode;

    }

}
