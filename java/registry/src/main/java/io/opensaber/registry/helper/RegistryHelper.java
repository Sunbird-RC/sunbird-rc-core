package io.opensaber.registry.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.flipkart.zjsonpatch.JsonPatch;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.service.DecryptionHelper;
import io.opensaber.registry.service.IReadService;
import io.opensaber.registry.service.ISearchService;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.sink.shard.ShardManager;
import io.opensaber.registry.util.ReadConfigurator;
import io.opensaber.registry.util.ReadConfiguratorFactory;
import io.opensaber.registry.util.RecordIdentifier;
import io.opensaber.registry.util.ViewTemplateManager;
import io.opensaber.views.ViewTemplate;
import io.opensaber.views.ViewTransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * This is helper class, user-service calls this class in-order to access registry functionality
 */
@Component
public class RegistryHelper {

    private static Logger logger = LoggerFactory.getLogger(RegistryHelper.class);

    @Autowired
    private ShardManager shardManager;

    @Autowired
    RegistryService registryService;

    @Autowired
    IReadService readService;

    @Autowired
    private ISearchService searchService;

    @Autowired
    private ViewTemplateManager viewTemplateManager;

    @Autowired
    private DBConnectionInfoMgr dbConnectionInfoMgr;

    @Autowired
    private DecryptionHelper decryptionHelper;

    @Autowired
    private OpenSaberInstrumentation watch;

    @Autowired
    private ObjectMapper objectMapper;
        
    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    @Value("${audit.frame.suffix}")
    public String auditSuffix;

    @Value("${audit.frame.suffixSeparator}")
    public String auditSuffixSeparator;
    /**
     * calls validation and then persists the record to registry.
     * @param inputJson
     * @return
     * @throws Exception
     */
    public String addEntity(JsonNode inputJson, String userId) throws Exception {
        return addEntity(inputJson, userId, inputJson.fields().next().getKey());
    }

    public String inviteEntity(JsonNode inputJson, String userId, String owner) throws Exception {
        String entityType = inputJson.fields().next().getKey();
        JSONUtil.addField((ObjectNode) inputJson.get(entityType), "osOwner", owner);
        return addEntity(inputJson, userId, entityType);
    }

    private String addEntity(JsonNode inputJson, String userId, String entityType) throws Exception {
        RecordIdentifier recordId = null;
        try {
            logger.info("Add api: entity type: {} and shard propery: {}", entityType, shardManager.getShardProperty());
            Shard shard = shardManager.getShard(inputJson.get(entityType).get(shardManager.getShardProperty()));
            watch.start("RegistryController.addToExistingEntity");
            String resultId = registryService.addEntity(shard,userId,inputJson);
            recordId = new RecordIdentifier(shard.getShardLabel(), resultId);
            watch.stop("RegistryController.addToExistingEntity");
            logger.info("AddEntity,{}", recordId.toString());
        } catch (Exception e) {
            logger.error("Exception in controller while adding entity !", e);
            throw new Exception(e);
        }
        return recordId.toString();
    }

    /**
     * Get entity details from the DB and modifies data according to view template
     * @param inputJson
     * @param requireLDResponse
     * @return
     * @throws Exception
     */
    public JsonNode readEntity(JsonNode inputJson, String userId, boolean requireLDResponse) throws Exception {
        logger.debug("readEntity starts");
        String entityType = inputJson.fields().next().getKey();
        String label = inputJson.get(entityType).get(dbConnectionInfoMgr.getUuidPropertyName()).asText();
        boolean includeSignatures = inputJson.get(entityType).get("includeSignatures") != null;

        return readEntity(userId, entityType, label, includeSignatures, viewTemplateManager.getViewTemplate(inputJson), requireLDResponse);

    }

    public JsonNode readEntity(String userId, String entityType, String label, boolean includeSignatures, ViewTemplate viewTemplate, boolean requireLDResponse) throws Exception {
        boolean includePrivateFields =  false;
        JsonNode resultNode = null;
        RecordIdentifier recordId = RecordIdentifier.parse(label);
        String shardId = dbConnectionInfoMgr.getShardId(recordId.getShardLabel());
        Shard shard = shardManager.activateShard(shardId);
        logger.info("Read Api: shard id: " + recordId.getShardLabel() + " for label: " + label);
        ReadConfigurator configurator = ReadConfiguratorFactory.getOne(includeSignatures);
        configurator.setIncludeTypeAttributes(requireLDResponse);
        if (viewTemplate != null) {
            includePrivateFields = viewTemplateManager.isPrivateFieldEnabled(viewTemplate,entityType);
        }
        configurator.setIncludeEncryptedProp(includePrivateFields);
        resultNode =  readService.getEntity(shard, userId, recordId.getUuid(), entityType, configurator);
        if (viewTemplate != null) {
            ViewTransformer vTransformer = new ViewTransformer();
            resultNode = includePrivateFields ? decryptionHelper.getDecryptedJson(resultNode) : resultNode;
            resultNode = vTransformer.transform(viewTemplate, resultNode);
        }
        logger.debug("readEntity ends");
        return resultNode;
    }

    /**
     * Get entity details from the DB and modifies data according to view template, requests which need only json format can call this method
     * @param inputJson
     * @return
     * @throws Exception
     */
    public JsonNode readEntity(JsonNode inputJson, String userId) throws Exception {
        return readEntity(inputJson,userId,false);
    }

    /** Search the input in the configured backend, external api's can use this method for searching
     * @param inputJson
     * @return
     * @throws Exception
     */
    public JsonNode searchEntity(JsonNode inputJson) throws Exception {
        logger.debug("searchEntity starts");
        JsonNode resultNode = searchService.search(inputJson);
        ViewTemplate viewTemplate = viewTemplateManager.getViewTemplate(inputJson);
        if (viewTemplate != null) {
            ViewTransformer vTransformer = new ViewTransformer();
            resultNode = vTransformer.transform(viewTemplate, resultNode);
        }
        // Search is tricky to support LD. Needs a revisit here.
        logger.debug("searchEntity ends");
        return resultNode;
    }

    /** Updates the input entity, external api's can use this method to update the entity
     * @param inputJson
     * @param userId
     * @return
     * @throws Exception
     */
    public String updateEntity(JsonNode inputJson, String userId) throws Exception {
        logger.debug("updateEntity starts");
        String entityType = inputJson.fields().next().getKey();
        String jsonString = objectMapper.writeValueAsString(inputJson);
        Shard shard = shardManager.getShard(inputJson.get(entityType).get(shardManager.getShardProperty()));
        String label = inputJson.get(entityType).get(dbConnectionInfoMgr.getUuidPropertyName()).asText();
        RecordIdentifier recordId = RecordIdentifier.parse(label);
        logger.info("Update Api: shard id: " + recordId.getShardLabel() + " for uuid: " + recordId.getUuid());
        registryService.updateEntity(shard, userId, recordId.getUuid(), jsonString);
        logger.debug("updateEntity ends");
        return "SUCCESS";
    }

    public void attestEntity(String entityName, JsonNode node, String[] jsonPaths, String userId) throws Exception {
        String patch = String.format("[{\"op\":\"add\", \"path\": \"attested\", \"value\": {\"attestation\":{\"id\":\"%s\"}, \"path\": \"%s\"}}]", userId, jsonPaths[0]);
        JsonPatch.applyInPlace(objectMapper.readTree(patch), node.get(entityName));
        updateEntity(node, userId);
    }

    /**
	 * Get Audit log information , external api's can use this method to get the
	 * audit log of an antity
	 * 
	 * @param inputJson
	 * @return
	 * @throws Exception
	 */

    public JsonNode getAuditLog(JsonNode inputJson) throws Exception {
        logger.debug("get audit log starts");
        String entityType = inputJson.fields().next().getKey();
        JsonNode queryNode =inputJson.get(entityType);
        
        ArrayNode newEntityArrNode = objectMapper.createArrayNode();
        newEntityArrNode.add(entityType + auditSuffixSeparator + auditSuffix);
        ((ObjectNode)queryNode).set("entityType", newEntityArrNode);
        
        JsonNode resultNode = searchService.search(queryNode);
        
        ViewTemplate viewTemplate = viewTemplateManager.getViewTemplate(inputJson);
        if (viewTemplate != null) {
            ViewTransformer vTransformer = new ViewTransformer();
            resultNode = vTransformer.transform(viewTemplate, resultNode);
        }
        logger.debug("get audit log ends");
        
        return resultNode;

    }

	

}

