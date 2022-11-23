package dev.sunbirdrc.registry.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.sunbirdrc.actors.factory.MessageFactory;
import dev.sunbirdrc.pojos.AuditInfo;
import dev.sunbirdrc.pojos.AuditRecord;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.service.IAuditService;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.registry.util.OSSystemFieldsHelper;

/**
 * Audit service implementation for audit layer in the application
 */
@Component
@Primary
public class AuditServiceImpl implements IAuditService {

    private static Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
    @Autowired
    private ObjectMapper objectMapper;

	@Value("${audit.enabled}")
	private boolean auditEnabled;

    @Value("${audit.frame.store}")
    private String auditFrameStore;

    @Value("${audit.frame.suffix}")
    private String auditSuffix;

    @Value("${audit.frame.suffixSeparator}")
    private String auditSuffixSeparator;

    @Autowired
    private IDefinitionsManager definitionsManager;
    
    @Autowired
    private OSSystemFieldsHelper systemFieldsHelper;
    
    @Autowired
    private AuditProviderFactory auditProviderFactory;


    @Value("${search.providerName}")
    private String searchProvider;

    private boolean isFileAudit() {
    	return auditEnabled && Constants.FILE.equalsIgnoreCase(auditFrameStore);
	}
    private boolean isDBAudit() {
    	return auditEnabled && Constants.DATABASE.equalsIgnoreCase(auditFrameStore);
	}

	/***
	 * Returns if the entityType must be audited.
	 * @param entityType
	 * @return
	 */
	@Override
    public boolean shouldAudit(String entityType) {
		boolean shouldAudit = isFileAudit();
		if (!shouldAudit) {
			shouldAudit = isDBAudit();
			Definition definition = definitionsManager.getDefinition(getAuditDefinitionName(entityType, auditSuffixSeparator, auditSuffix));
			shouldAudit &= (definition != null);
		}
		return shouldAudit;
    }
	
	/***
	 * Returns the audit action acording to entity type
	 * @param entityType
	 * @return action
	 */
	public String isAuditAction(String entityType) {
		String action = Constants.AUDIT_ACTION_SEARCH;
     	// If a query is made to search audit table, call it audit.
     	if(entityType.contains(auditSuffix)) {
     		action = Constants.AUDIT_ACTION_AUDIT;
     	}
		return action;
    }

    @Override
    public void doAudit(AuditRecord auditRecord, JsonNode inputNode, Shard shard) {
    	auditProviderFactory.getAuditService(auditFrameStore).doAudit(auditRecord, inputNode, shard);
    }
    
    public void sendAuditToESActor(JsonNode inputNode, String entityType, String entityId) throws JsonProcessingException{
    	boolean elasticSearchEnabled = ("dev.sunbirdrc.registry.service.ElasticSearchService".equals(searchProvider));
		
        MessageProtos.Message message = MessageFactory.instance().createOSActorMessage(elasticSearchEnabled, "ADD",
                entityType.toLowerCase(), entityId, inputNode.get(entityType), null);
        ActorCache.instance().get(Router.ROUTER_NAME).tell(message, null);
    }

    public JsonNode convertAuditRecordToJson(AuditRecord auditRecord, String vertexLabel) throws IOException {
    	JsonNode jsonN = JSONUtil.convertObjectJsonNode(auditRecord);

        //Fetching auditInfo and creating json string
        JsonNode auditInfo = jsonN.path("auditInfo");
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(auditInfo);

        //Removing auditIfo json node from audit record
        ((ObjectNode) jsonN).remove("auditInfo");

        // Adding auditInfo with json string to audit record
        ((ObjectNode) jsonN).put("auditInfo", json);

      		
        ObjectNode root = JsonNodeFactory.instance.objectNode();        
     
  		
        root.set(vertexLabel, jsonN);
        
        JsonNode rootNode  =  root;
        
    	systemFieldsHelper.ensureCreateAuditFields(vertexLabel, rootNode.get(vertexLabel), auditRecord.getUserId());
	
    	return rootNode;        
    }
    
    @Override
    public List<AuditInfo> createAuditInfo(String auditAction, String entityType){

    	List<AuditInfo> auditItemDetails = new ArrayList<>();           
        AuditInfo auditInfo = new AuditInfo();
        auditInfo.setOp(auditAction);
        auditInfo.setPath("/" + entityType);
        
        auditItemDetails.add(auditInfo);
       
        return auditItemDetails;
    }
    @Override
    public List<AuditInfo> createAuditInfoWithJson(String auditAction, JsonNode differenceJson, String entityType){

    	List<AuditInfo> auditItemDetails = null;
    	try {
    		auditItemDetails = Arrays.asList(objectMapper.treeToValue(differenceJson, AuditInfo[].class));
    	} catch (Exception e) {
    		logger.error("Generic error in saving audit info : {}", e);
    	}
        return auditItemDetails;
    }
	@Override
	public String getAuditProvider() {
		// TODO Auto-generated method stub
		return null;
	}


}
