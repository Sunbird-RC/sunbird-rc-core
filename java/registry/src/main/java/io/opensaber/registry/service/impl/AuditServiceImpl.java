package io.opensaber.registry.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import io.opensaber.actors.factory.MessageFactory;
import io.opensaber.pojos.AuditInfo;
import io.opensaber.pojos.AuditRecord;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.DateUtil;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.service.IAuditService;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.AuditFileWriter;
import io.opensaber.registry.util.Definition;
import io.opensaber.registry.util.DefinitionsManager;
import io.opensaber.registry.util.EntityParenter;
import io.opensaber.registry.util.OSSystemFieldsHelper;

/**
 * Audit service implementation for audit layer in the application
 */
public class AuditServiceImpl implements IAuditService {

	private static Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
	@Autowired
	private ObjectMapper objectMapper;
    
    @Value("${audit.frame.store}")
    private String auditFrameStore;

    @Value("${audit.frame.suffix}")
    private String auditSuffix;

    @Value("${audit.frame.suffixSeparator}")
    private String auditSuffixSeparator;

    @Autowired
    private OSSystemFieldsHelper systemFieldsHelper;
    
    @Value("${persistence.commit.enabled:true}")
    private boolean commitEnabled;
    
    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    @Autowired
    private DefinitionsManager definitionsManager;

    @Value("${search.providerName}")
    private String searchProvider;

    @Autowired
    private EntityParenter entityParenter;
    
	/**
	 * This is starting of audit in the application, audit details of read, add, update, delete and search activities
	 *
	 */
    @Override
    public void doAudit(AuditRecord auditRecord, JsonNode mergedNode,  List<String> entityTypes, String entityRootId, Shard shard)   {
        logger.debug("doAudit started");
       
        try {
        		String operation = auditRecord.getAuditInfo().get(0).getOp();
		        // If the audit is stored as file, fetchAudit from audit entity will not come to this point.
	    		if(Constants.FILE.equalsIgnoreCase(auditFrameStore)) {
	    			
	    			auditToFile(auditRecord);
	    			
	    		// Shard will be null in case of elastic search, so we are not storing audit info in such cases
	    		}else if(Constants.DATABASE.equalsIgnoreCase(auditFrameStore) && shard != null) {    
	    			for(String entityType : entityTypes){
	    				auditToDB(auditRecord, entityType, shard);
	    			}
	    		}	
    		
    			String entityType = entityTypes.get(0);    			
	    		boolean elasticSearchEnabled = ("io.opensaber.registry.service.ElasticSearchService".equals(searchProvider));
	    		
	    		JsonNode inputNode = null;
	    		if(mergedNode!=null) {
	    			inputNode = mergedNode.get(entityType);
	    		}
	 	        MessageProtos.Message message = MessageFactory.instance().createOSActorMessage(elasticSearchEnabled, operation,
	 	        		entityType, entityRootId, inputNode, auditRecord);
	 	        ActorCache.instance().get(Router.ROUTER_NAME).tell(message, null);
        }catch(AuditFailedException ae) {
        	logger.error("Error in saving audit log : {}" + ae);
        }catch(Exception e) {
        	logger.error("Error in saving audit log : {}" + e);
        }
        logger.debug("doAudit ends");
    }
    @Override
    public AuditRecord createAuditRecord(String userId, String auditAction, String id, List<Integer> transactionId) throws JsonProcessingException {
    	
    	AuditRecord auditRecord = new AuditRecord();    	 
    	//Transaction id is null incase of elastic read service
        auditRecord.setUserId(userId).setAction(auditAction)
                .setTransactionId(transactionId).setRecordId(id).
                setAuditId(UUID.randomUUID().toString()).setTimestamp(String.valueOf(DateUtil.getTimeStampLong()));
        
        return auditRecord;
    }
    @Override
    public List<AuditInfo> createAuditInfo(String operation, String auditAction, JsonNode readNode, JsonNode mergedNode, List<String> entityTypes ) throws JsonProcessingException  {
    	
    	List<AuditInfo> auditItemDetails = null;
    	if (auditAction.equalsIgnoreCase(Constants.AUDIT_ACTION_DELETE) || auditAction.equalsIgnoreCase(Constants.AUDIT_ACTION_READ) 
    			|| auditAction.equalsIgnoreCase(Constants.AUDIT_ACTION_SEARCH)|| auditAction.equalsIgnoreCase(Constants.AUDIT_ACTION_AUDIT)) {
            auditItemDetails = new ArrayList<>();
            for(String entityType : entityTypes){
	            AuditInfo auditInfo = new AuditInfo();
	            auditInfo.setOp(operation);
	            auditInfo.setPath("/"+entityType);
	            auditItemDetails.add(auditInfo);
	       }
        } else {//If action is ADD or UPDATE
        	JsonNode differenceJson = JSONUtil.diffJsonNode(readNode, mergedNode);
            auditItemDetails = Arrays.asList(objectMapper.treeToValue(differenceJson, AuditInfo[].class));
        }
    	return auditItemDetails;
    }
    
    @Override
    public void auditToFile(AuditRecord auditRecord) throws JsonProcessingException{
    	AuditFileWriter auditWriter = new AuditFileWriter();
    	auditWriter.auditToFile(auditRecord);
    }
	
	@Async("auditExecutor")
	public void auditToDB(AuditRecord auditRecord, String entityType, Shard shard) throws IOException, AuditFailedException{
		
		String entityId = "auditPlaceholderId";
		JsonNode jsonN = JSONUtil.convertObjectJsonNode(auditRecord);
		
		//Fetching auditInfo and creating json string
		JsonNode auditInfo = jsonN.path("auditInfo");
		ObjectMapper objectMapper = new ObjectMapper();
		String json = objectMapper.writeValueAsString(auditInfo);
		
		//Removing auditIfo json node from audit record 
		((ObjectNode)jsonN).remove("auditInfo");
		
		// Adding auditInfo with json string to audit record 
		((ObjectNode)jsonN).put("auditInfo", json );
	
		//Creating root node with vertex label
		//by appending the entity name with _Audit
		String vertexLabel = entityType;
		if( null != entityType && !(entityType.contains(auditSuffixSeparator+auditSuffix))) {
			vertexLabel = vertexLabel+auditSuffixSeparator+auditSuffix;
		}
		ObjectNode root = JsonNodeFactory.instance.objectNode();
		root.set(vertexLabel, jsonN); 						
		
		JsonNode rootNode  =  root;
			
		systemFieldsHelper.ensureCreateAuditFields(vertexLabel, rootNode.get(vertexLabel), auditRecord.getUserId());
		
		Transaction tx = null;
		DatabaseProvider dbProvider = shard.getDatabaseProvider();
		IRegistryDao registryDao = new RegistryDaoImpl(dbProvider, definitionsManager, uuidPropertyName);
        try (OSGraph osGraph = dbProvider.getOSGraph()) {
            Graph graph = osGraph.getGraphStore();
            tx = dbProvider.startTransaction(graph);
            entityId = registryDao.addEntity(graph, rootNode);
            if (commitEnabled) {
                dbProvider.commitTransaction(graph, tx);
            }
            
            logger.debug("Audit added : " + entityId);
        }catch(Exception e) {
        	logger.error("Audit failed : {}" + e);

        	throw new AuditFailedException("Audit failed : " + e.getMessage());
        } finally {
            tx.close();
        }
        // Add indices: executes only once.
        String shardId = shard.getShardId();
        Vertex parentVertex = entityParenter.getKnownParentVertex(vertexLabel, shardId);
        Definition definition = definitionsManager.getDefinition(vertexLabel);
        entityParenter.ensureIndexExists(dbProvider, parentVertex, definition, shardId);
	}
	
}
