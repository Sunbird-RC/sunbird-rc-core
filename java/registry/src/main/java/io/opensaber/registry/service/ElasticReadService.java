package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.audit.IAuditService;
import io.opensaber.elastic.IElasticService;
import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.AuditInfo;
import io.opensaber.pojos.AuditRecord;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.DateUtil;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.ReadConfigurator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class provide read option with Elastic search
 * database to operate
 *
 */
@Component
public class ElasticReadService implements IReadService {

    private static Logger logger = LoggerFactory.getLogger(ElasticReadService.class);

    @Autowired
    private IElasticService elasticService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IAuditService auditService;

    /**
     * This method interacts with the Elasticsearch and reads the record
     *
     * @param id           - osid
     * @param entityType   - elastic-search index
     * @param configurator
     * @return
     * @throws Exception
     */
    @Override
    public JsonNode getEntity(Shard shard, String userId, String id, String entityType, ReadConfigurator configurator) throws Exception {
        JsonNode result = null;
        AuditRecord auditRecord = null;
        Map<String, Object> response = null;
        try {
            response = elasticService.readEntity(entityType.toLowerCase(), id);
        } catch (IOException e) {
            logger.error("Exception in reading a record to ElasticSearch", e);
        }
        if (response == null) {
            throw new RecordNotFoundException("Record with " + id + " not found in Elastic-search");
        }
        result = objectMapper.convertValue(response, JsonNode.class);
        if (!configurator.isIncludeSignatures()) {
            JSONUtil.removeNode((ObjectNode) result, Constants.SIGNATURES_STR);
        }
        auditRecord = new AuditRecord();
        auditRecord.setUserId(userId).setAction(Constants.AUDIT_ACTION_READ).setRecordId(id).
                setAuditId(UUID.randomUUID().toString()).setTimeStamp(DateUtil.getTimeStamp());
        AuditInfo auditInfo = new AuditInfo();
        auditInfo.setOp(Constants.AUDIT_ACTION_READ_OP);
        auditInfo.setPath("/" + entityType);
        auditRecord.setAuditInfo(Arrays.asList(auditInfo));
        auditService.audit(auditRecord);
        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        resultNode.set(entityType, result);
        return resultNode;
    }

}
