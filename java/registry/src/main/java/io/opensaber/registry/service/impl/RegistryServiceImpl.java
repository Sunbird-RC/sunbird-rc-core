package io.opensaber.registry.service.impl;

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.opensaber.pojos.attestation.auto.AutoAttestationPolicy;
import io.opensaber.registry.util.*;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.opensaber.actors.factory.MessageFactory;
import io.opensaber.pojos.ComponentHealthInfo;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.dao.VertexReader;
import io.opensaber.registry.dao.VertexWriter;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.service.EncryptionHelper;
import io.opensaber.registry.service.EncryptionService;
import io.opensaber.registry.service.IAuditService;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.service.SignatureHelper;
import io.opensaber.registry.service.SignatureService;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import io.opensaber.registry.sink.shard.Shard;

import javax.servlet.http.HttpServletRequest;

@Component
public class RegistryServiceImpl implements RegistryService {

    private static final String ID_REGEX = "\"@id\"\\s*:\\s*\"_:[a-z][0-9]+\",";
    private static Logger logger = LoggerFactory.getLogger(RegistryServiceImpl.class);

    @Autowired
    private EntityTypeHandler entityTypeHandler;
    @Autowired
    private EncryptionService encryptionService;
    @Autowired
    private SignatureService signatureService;
    @Autowired
    private DefinitionsManager definitionsManager;

    @Autowired
    private EncryptionHelper encryptionHelper;
    @Autowired
    private SignatureHelper signatureHelper;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${encryption.enabled}")
    private boolean encryptionEnabled;

    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    @Value("${signature.enabled}")
    private boolean signatureEnabled;

    @Value("${persistence.enabled:true}")
    private boolean persistenceEnabled;

    @Value("${persistence.commit.enabled:true}")
    private boolean commitEnabled;

    @Value("${search.providerName}")
    private String searchProvider;

    @Value("${audit.enabled}")
    private boolean auditEnabled;

    @Value("${registry.perRequest.indexCreation.enabled:false}")
    private boolean perRequestIndexCreation;

    @Value("${registry.context.base}")
    private String registryBaseUrl;

    @Autowired
    private EntityParenter entityParenter;

    @Autowired
    private OSSystemFieldsHelper systemFieldsHelper;

    @Autowired
    private IAuditService auditService;

    public HealthCheckResponse health(Shard shard) throws Exception {
        HealthCheckResponse healthCheck;
        boolean databaseServiceup = shard.getDatabaseProvider().isDatabaseServiceUp();
        boolean overallHealthStatus = databaseServiceup;
        List<ComponentHealthInfo> checks = new ArrayList<>();

        ComponentHealthInfo databaseServiceInfo = new ComponentHealthInfo(Constants.OPENSABER_DATABASE_NAME,
                databaseServiceup);
        checks.add(databaseServiceInfo);

        if (encryptionEnabled) {
            boolean encryptionServiceStatusUp = encryptionService.isEncryptionServiceUp();
            ComponentHealthInfo encryptionHealthInfo = new ComponentHealthInfo(
                    Constants.SUNBIRD_ENCRYPTION_SERVICE_NAME, encryptionServiceStatusUp);
            checks.add(encryptionHealthInfo);
            overallHealthStatus = overallHealthStatus && encryptionServiceStatusUp;
        }

        if (signatureEnabled) {
            boolean signatureServiceStatusUp = signatureService.isServiceUp();
            ComponentHealthInfo signatureServiceInfo = new ComponentHealthInfo(Constants.SUNBIRD_SIGNATURE_SERVICE_NAME,
                    signatureServiceStatusUp);
            checks.add(signatureServiceInfo);
            overallHealthStatus = overallHealthStatus && signatureServiceStatusUp;
        }

        healthCheck = new HealthCheckResponse(Constants.OPENSABER_REGISTRY_API_NAME, overallHealthStatus, checks);
        logger.info("Heath Check :  ", checks.toArray().toString());
        return healthCheck;
    }

    /**
     * delete the vertex and changes the status
     *
     * @param uuid
     * @throws Exception
     */
    @Override
    public void deleteEntityById(Shard shard, String userId, String uuid) throws Exception {
        DatabaseProvider databaseProvider = shard.getDatabaseProvider();
        IRegistryDao registryDao = new RegistryDaoImpl(databaseProvider, definitionsManager, uuidPropertyName);
        try (OSGraph osGraph = databaseProvider.getOSGraph()) {
            Graph graph = osGraph.getGraphStore();
            Transaction tx = databaseProvider.startTransaction(graph);
            ReadConfigurator configurator = ReadConfiguratorFactory.getOne(false);
            VertexReader vertexReader = new VertexReader(databaseProvider, graph, configurator, uuidPropertyName, definitionsManager);
            Vertex vertex  = vertexReader.getVertex(null, uuid);
            if (!(vertex.property(Constants.STATUS_KEYWORD).isPresent()
                    && vertex.property(Constants.STATUS_KEYWORD).value().equals(Constants.STATUS_INACTIVE))) {
                registryDao.deleteEntity(vertex);
                databaseProvider.commitTransaction(graph, tx);
                String index = vertex.property(Constants.TYPE_STR_JSON_LD).isPresent() ? (String) vertex.property(Constants.TYPE_STR_JSON_LD).value() : null;
                
                auditService.auditDelete(
                		auditService.createAuditRecord(userId, uuid, tx, index),
                		shard);
                
                callESActors( null, "DELETE", index, uuid, tx);


            }
            logger.info("Entity {} marked deleted", uuid);
        }
        

    }

    /**
     * This method adds the entity into db, calls elastic and audit asynchronously
     *
     * @param rootNode - input value as string
     * @return
     * @throws Exception
     */
    public String addEntity(Shard shard, String userId, JsonNode rootNode) throws Exception {
        Transaction tx = null;
        String entityId = "entityPlaceholderId";
        String vertexLabel = rootNode.fieldNames().next();

        systemFieldsHelper.ensureCreateAuditFields(vertexLabel, rootNode.get(vertexLabel), userId);

        if (encryptionEnabled) {
            rootNode = encryptionHelper.getEncryptedJson(rootNode);
        }

/*
        if (signatureEnabled) {
            signatureHelper.signJson(rootNode);
        }
*/

        if (persistenceEnabled) {
            DatabaseProvider dbProvider = shard.getDatabaseProvider();
            IRegistryDao registryDao = new RegistryDaoImpl(dbProvider, definitionsManager, uuidPropertyName);
            try (OSGraph osGraph = dbProvider.getOSGraph()) {
                Graph graph = osGraph.getGraphStore();
                tx = dbProvider.startTransaction(graph);
                entityId = registryDao.addEntity(graph, rootNode);
                if (commitEnabled) {
                    dbProvider.commitTransaction(graph, tx);
                }
            } finally {
                tx.close();
            }
            // Add indices: executes only once.
            if (perRequestIndexCreation) {
                String shardId = shard.getShardId();
                Vertex parentVertex = entityParenter.getKnownParentVertex(vertexLabel, shardId);
                Definition definition = definitionsManager.getDefinition(vertexLabel);
                entityParenter.ensureIndexExists(dbProvider, parentVertex, definition, shardId);
            }
            
            callESActors(rootNode, "ADD", vertexLabel, entityId, tx);

            auditService.auditAdd(
            		auditService.createAuditRecord(userId, entityId, tx, vertexLabel),
            		shard, rootNode);
            

        }
        return entityId;
    }

    @Override
    public void updateEntity(Shard shard, String userId, String id, String jsonString) throws Exception {
        JsonNode inputNode = objectMapper.readTree(jsonString);
        String entityType = inputNode.fields().next().getKey();
        systemFieldsHelper.ensureUpdateAuditFields(entityType, inputNode.get(entityType), userId);
        if (encryptionEnabled) {
            inputNode = encryptionHelper.getEncryptedJson(inputNode);
        }

        DatabaseProvider databaseProvider = shard.getDatabaseProvider();
        IRegistryDao registryDao = new RegistryDaoImpl(databaseProvider, definitionsManager, uuidPropertyName);
        try (OSGraph osGraph = databaseProvider.getOSGraph()) {
            Graph graph = osGraph.getGraphStore();
            Transaction tx = databaseProvider.startTransaction(graph);

            // Read the node and
            // TODO - decrypt properties to pass validation
            ReadConfigurator readConfigurator = ReadConfiguratorFactory.getForUpdateValidation();
            VertexReader vr = new VertexReader(databaseProvider, graph, readConfigurator, uuidPropertyName, definitionsManager);
            JsonNode readNode = vr.read(entityType, id);

            String rootId = readNode.findPath(Constants.ROOT_KEYWORD).textValue();
            if (rootId != null && !rootId.equals(id)) {
                // Child node is getting updated individually. So, read the parent to
                // validate the parent record
                logger.debug("Reading the parent record {}", rootId);
                // Here we don't know the parent entity type
                readNode = vr.read(readNode.findPath(Constants.ROOT_KEYWORD).textValue());
            } else {
                logger.debug("Updating the parent record {}", rootId);
                // Update is for the parent entity.
                // Nothing to do as the record has been already read.
                rootId = id;
            }
            String parentEntityType = readNode.fields().next().getKey();
            HashMap<String, Vertex> uuidVertexMap = vr.getUuidVertexMap();

            // Merge the new changes
            JsonNode mergedNode = mergeWrapper("/" + parentEntityType, (ObjectNode) readNode, (ObjectNode) inputNode);
            logger.debug("After merge the payload is " + mergedNode.toString());

            // Re-sign, i.e., remove and add entity signature again
/*
            if (signatureEnabled) {
                logger.debug("Removing earlier signature and adding new one");
                String entitySignUUID = signatureHelper.removeEntitySignature(parentEntityType, (ObjectNode) mergedNode);
                JsonNode newSignature = signatureHelper.signJson(mergedNode);
                String rootOsid = mergedNode.get(entityType).get(uuidPropertyName).asText();
                ObjectNode objectSignNode = (ObjectNode) newSignature;
                objectSignNode.put(uuidPropertyName, entitySignUUID);
                objectSignNode.put(Constants.ROOT_KEYWORD, rootOsid);
                Vertex oldEntitySignatureVertex = uuidVertexMap.get(entitySignUUID);

                registryDao.updateVertex(graph, oldEntitySignatureVertex, newSignature, entityType);
            }
*/

            // TODO - Validate before update
            JsonNode validationNode = mergedNode.deepCopy();
            List<String> removeKeys = new LinkedList<>();
            removeKeys.add(uuidPropertyName);
            removeKeys.add(Constants.TYPE_STR_JSON_LD);
            JSONUtil.removeNodes((ObjectNode) validationNode, removeKeys);
//            iValidate.validate(entityNodeType, mergedNode.toString());
//            logger.debug("Validated payload before update");

            // Finally update
            // Input nodes have shard labels
            if (!shard.getShardLabel().isEmpty()) {
                // Replace osid without shard details
                String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
                JSONUtil.trimPrefix((ObjectNode)inputNode, uuidPropertyName, prefix);
            }

            // The entity type is a child and so could be different from parent entity type.
            doUpdate(shard, graph, registryDao, vr, inputNode.get(entityType), entityType, null);

            databaseProvider.commitTransaction(graph, tx);

            if(isInternalRegistry(entityType)) {
                callESActors(mergedNode, "UPDATE", entityType, id, tx);
            }
            auditService.auditUpdate(
            		auditService.createAuditRecord(userId, rootId, tx, entityType), 
            		shard, mergedNode, readNode);


        }
    }

    private boolean isInternalRegistry(String entityType) {
        return definitionsManager.getAllKnownDefinitions().contains(entityType);
    }

    @Override
   @Async("taskExecutor")	
   public void callESActors(JsonNode rootNode, String operation, String parentEntityType, String entityRootId, Transaction tx) throws JsonProcessingException {			      
			logger.debug("callESActors started");       
			rootNode = rootNode != null ? rootNode.get(parentEntityType): rootNode;
			boolean elasticSearchEnabled = (searchProvider.equals("io.opensaber.registry.service.ElasticSearchService"));	
			MessageProtos.Message message = MessageFactory.instance().createOSActorMessage(elasticSearchEnabled, operation,	
			        parentEntityType.toLowerCase(), entityRootId, rootNode,null);	
			ActorCache.instance().get(Router.ROUTER_NAME).tell(message, null);
			logger.debug("callESActors ends");	
    }

    @Async("taskExecutor")
    @Override
    public void callNotificationActors(String operation, String to, String subject, String message) throws JsonProcessingException {
        logger.debug("callNotificationActors started");
        MessageProtos.Message messageProto = MessageFactory.instance().createNotificationActorMessage(operation, to, subject, message);
        ActorCache.instance().get(Router.ROUTER_NAME).tell(messageProto, null);
        logger.debug("callESActors ends");
    }

    @Async("taskExecutor")
    @Override
    public void callAutoAttestationActor(JsonNode existingNode, JsonNode updatedNode, String entityName, String entityId, HttpServletRequest request) throws JsonProcessingException {
        logger.info("Setting up the message to call auto attestation actor");
        AutoAttestationPolicy autoAttestationPolicy = definitionsManager.getDefinition(entityName)
                .getOsSchemaConfiguration()
                .getAutoAttestationPolicy(IteratorUtils.toList(updatedNode.fieldNames()));
        String accessToken = request.getHeader("Authorization");
        String valuePath = autoAttestationPolicy.getValuePath();
        if(existingNode.isNull() || !JSONUtil.readValFromJsonTree(valuePath, existingNode).equals(JSONUtil.readValFromJsonTree(valuePath, updatedNode))) {
            logger.info("Calling auto attestation actor");
            logger.info("Url {}", registryBaseUrl);

            MessageProtos.Message message = MessageFactory.instance().createAutoAttestationMessage(autoAttestationPolicy, updatedNode, accessToken, registryBaseUrl);
            ActorCache.instance().get(Router.ROUTER_NAME).tell(message, null);
        }
    }

    private void doUpdateArray(Shard shard, Graph graph, IRegistryDao registryDao, VertexReader vr, Vertex blankArrVertex, ArrayNode arrayNode, String parentName) {
        HashMap<String, Vertex> uuidVertexMap = vr.getUuidVertexMap();
        Set<Object> updatedUuids = new HashSet<Object>();
        Set<String> previousArrayItemsUuids = vr.getArrayItemUuids(blankArrVertex);
        
        VertexWriter vertexWriter = new VertexWriter(graph, shard.getDatabaseProvider() , uuidPropertyName);
        
        for (JsonNode item : arrayNode) {
            if (item.isObject()) {
                if (item.get(uuidPropertyName) != null) {
                    Vertex existingItem = uuidVertexMap.getOrDefault(item.get(uuidPropertyName).textValue(), null);
                    if (existingItem != null) {
                        try {
                            registryDao.updateVertex(graph, existingItem, item, parentName);
                        } catch (Exception e) {
                            logger.error("Can't update item {}", item.toString());
                        }
                        updatedUuids.add(item.get(uuidPropertyName).textValue());
                    }
                } else {
                    // New item got added.
                    Vertex newItem = vertexWriter.writeSingleNode(blankArrVertex, vr.getInternalType(blankArrVertex), item);
                    updatedUuids.add(shard.getDatabaseProvider().getId(newItem));
                }
            }
        }
        
        //Update the array_node with list of updated uuids
        vertexWriter.updateArrayNode(blankArrVertex, vr.getInternalType(blankArrVertex), new ArrayList<Object>(updatedUuids));
        
        doDelete(registryDao, vr, previousArrayItemsUuids, updatedUuids);

    }

    /**Delete the previous array items Uuids which are not updated
     * 
     * @param registryDao
     * @param vr
     * @param previousArrayItemsUuids
     * @param updatedUuids
     */
    private void doDelete(IRegistryDao registryDao, VertexReader vr, Set<String> previousArrayItemsUuids,Set<Object> updatedUuids) {
    	HashMap<String, Vertex> uuidVertexMap = vr.getUuidVertexMap();
        for (String itemUuid : previousArrayItemsUuids) {
        	itemUuid = ArrayHelper.unquoteString(itemUuid);
			if (!updatedUuids.contains(itemUuid)) {
				// delete this item
				registryDao.deleteEntity(uuidVertexMap.get(itemUuid));
			}
        }
    }

    private void doUpdate(Shard shard, Graph graph, IRegistryDao registryDao, VertexReader vr, JsonNode userInputNode, String userInputKey, Vertex parentVertex) throws Exception {
        HashMap<String, Vertex> uuidVertexMap = vr.getUuidVertexMap();
        Vertex rootVertex = vr.getRootVertex();

        // For each of the input node, take the following actions as it is fit
        // Simple object - just update that object (new uuid will not be issued)
        // Simple NEW object - need to update the root
        // Array object - need to delete and then add new one
        JsonNode parentOsidNode = userInputNode.get(uuidPropertyName);
        Vertex existingVertex = null;
        try {
            String parentOsid = (parentOsidNode == null) ? "" : parentOsidNode.textValue();
            existingVertex = uuidVertexMap.getOrDefault(parentOsid, null);
        } catch (IllegalStateException propException) {
            logger.debug("Root vertex {} doesn't have any property named {}",
                    shard.getDatabaseProvider().getId(rootVertex), uuidPropertyName);
        }

        if (existingVertex != null) {
            // Existing vertex - just add/update properties
            Iterator<Map.Entry<String, JsonNode>> fieldsItr = userInputNode.fields();
            while (fieldsItr.hasNext()) {
                Map.Entry<String, JsonNode> oneElement = fieldsItr.next();
                JsonNode oneElementNode = oneElement.getValue();
                if (!oneElement.getKey().equals(uuidPropertyName) &&
                        oneElementNode.isValueNode() || oneElementNode.isArray()) {
                    logger.info("Value or array node, going to update {}", oneElement.getKey());

                    if (oneElementNode.isArray()) {
                        // Arrays are treated specially - we create a blank node and then
                        // individual items
                        String arrayRefLabel = RefLabelHelper.getArrayLabel(oneElement.getKey(), uuidPropertyName);
                        Vertex existArrayVertex = null;
                        try {
                            existArrayVertex = uuidVertexMap.getOrDefault(rootVertex.value(arrayRefLabel), null);
                        } catch (IllegalStateException propException) {
                            logger.debug("Root vertex {} doesn't have any property named {}",
                                    shard.getDatabaseProvider().getId(rootVertex), arrayRefLabel);
                        }

                        if (null != existArrayVertex) {
                            // updateArrayItems one by one
                            doUpdateArray(shard, graph, registryDao, vr, existArrayVertex, (ArrayNode) oneElementNode, userInputKey);
                        } else {
                            VertexWriter vertexWriter = new VertexWriter(graph, shard.getDatabaseProvider(), uuidPropertyName);
                            vertexWriter.createArrayNode(rootVertex, oneElement.getKey(), (ArrayNode) oneElementNode);
                        }
                        registryDao.updateVertex(graph, existArrayVertex, oneElementNode, oneElement.getKey());
                    } else {
                        registryDao.updateVertex(graph, existingVertex, userInputNode, userInputKey);
                    }
                } else if (oneElementNode.isObject()) {
                    logger.info("Object node {}", oneElement.toString());
                    doUpdate(shard, graph, registryDao, vr, oneElementNode, oneElement.getKey(), existingVertex); //todo this is adding to existing parent node merging inner structure.
                    //registryDao.updateVertex(graph, rootVertex, userInputNode);
                }
            }
        } else {
            // Likely a new addition
            logger.info("Adding a new node to existing one");
            // Attach new vertex to the parent vertex
            registryDao.updateVertex(graph, parentVertex, userInputNode, userInputKey);
        }
    }

    /**
     * Merging input json node to DB entity node, this method in turn calls
     * mergeDestinationWithSourceNode method for deep copy of properties and
     * objects
     *
     * @param databaseNode - the one found in db
     * @param inputNode - the one passed by user
     * @return
     */
    private ObjectNode mergeWrapper(String entityType, ObjectNode databaseNode, ObjectNode inputNode) {
        List<String> ignoredProps = new ArrayList<String>() {
        };
        ignoredProps.add(uuidPropertyName);
        // We know the user is likely to update less fields and so iterate over it.
        ObjectNode result = databaseNode.deepCopy();
        inputNode.fields().forEachRemaining(prop -> {
            JSONUtil.merge(entityType, result, (ObjectNode) prop.getValue(), ignoredProps);
        });
        return result;
    }

}