package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.actors.factory.MessageFactory;
import dev.sunbirdrc.elastic.IElasticService;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.HealthCheckResponse;
import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.registry.dao.IRegistryDao;
import dev.sunbirdrc.registry.dao.RegistryDaoImpl;
import dev.sunbirdrc.registry.dao.VertexReader;
import dev.sunbirdrc.registry.dao.VertexWriter;
import dev.sunbirdrc.registry.exception.SignatureException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.middleware.util.OSSystemFields;
import dev.sunbirdrc.registry.service.*;
import dev.sunbirdrc.registry.sink.DatabaseProvider;
import dev.sunbirdrc.registry.sink.OSGraph;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.sunbirdrc.registry.Constants.CREDENTIAL_TEMPLATE;
import static dev.sunbirdrc.registry.Constants.Schema;

@Service
@Qualifier("sync")
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
    private IDefinitionsManager definitionsManager;

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

    @Value("${elastic.search.add_shard_prefix:true}")
    private boolean addShardPrefixForESRecord;

    @Value("${registry.context.base}")
    private String registryBaseUrl;

    @Autowired
    private EntityParenter entityParenter;

    @Autowired
    private OSSystemFieldsHelper systemFieldsHelper;

    @Autowired
    private IAuditService auditService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private IElasticService elasticService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private List<HealthIndicator> healthIndicators;

    public HealthCheckResponse health(Shard shard) throws Exception {
        HealthCheckResponse healthCheck;
        AtomicBoolean overallHealthStatus = new AtomicBoolean(true);
        List<ComponentHealthInfo> checks = new ArrayList<>();
        healthIndicators.parallelStream().forEach(healthIndicator -> {
            ComponentHealthInfo healthInfo = healthIndicator.getHealthInfo();
            checks.add(healthInfo);
            overallHealthStatus.set(overallHealthStatus.get() & healthInfo.isHealthy());
        });

        healthCheck = new HealthCheckResponse(Constants.SUNBIRDRC_REGISTRY_API, overallHealthStatus.get(), checks);
        logger.info("Heath Check :  ", checks.toArray().toString());
        return healthCheck;
    }

    /**
     * delete the vertex and changes the status
     *
     * @param uuid
     * @return
     * @throws Exception
     */
    @Override
    public Vertex deleteEntityById(Shard shard, String userId, String uuid) throws Exception {
        DatabaseProvider databaseProvider = shard.getDatabaseProvider();
        IRegistryDao registryDao = new RegistryDaoImpl(databaseProvider, definitionsManager, uuidPropertyName);
        try (OSGraph osGraph = databaseProvider.getOSGraph()) {
            Graph graph = osGraph.getGraphStore();
            Transaction tx = databaseProvider.startTransaction(graph);
            ReadConfigurator configurator = ReadConfiguratorFactory.getOne(false);
            VertexReader vertexReader = new VertexReader(databaseProvider, graph, configurator, uuidPropertyName, definitionsManager);
            Vertex vertex = vertexReader.getVertex(null, uuid);
            String index = vertex.property(Constants.TYPE_STR_JSON_LD).isPresent() ? (String) vertex.property(Constants.TYPE_STR_JSON_LD).value() : null;
            if (!StringUtils.isEmpty(index) && index.equals(Schema)) {
                schemaService.deleteSchemaIfExists(vertex);
            }
            if (!(vertex.property(Constants.STATUS_KEYWORD).isPresent()
                    && vertex.property(Constants.STATUS_KEYWORD).value().equals(Constants.STATUS_INACTIVE))) {
                registryDao.deleteEntity(vertex);
                databaseProvider.commitTransaction(graph, tx);
                auditService.auditDelete(
                        auditService.createAuditRecord(userId, uuid, tx, index),
                        shard);
                if (isElasticSearchEnabled()) {
                    callESActors(null, "DELETE", index, uuid, tx);
                }


            }
            logger.info("Entity {} marked deleted", uuid);
            return vertex;
        }


    }

    /**
     * This method adds the entity into db, calls elastic and audit asynchronously
     *
     * @param rootNode - input value as string
     * @param skipSignature
     * @return
     * @throws Exception
     */
    public String addEntity(Shard shard, String userId, JsonNode rootNode, boolean skipSignature) throws Exception {
        Transaction tx = null;
        String entityId = "entityPlaceholderId";
        String vertexLabel = rootNode.fieldNames().next();

        systemFieldsHelper.ensureCreateAuditFields(vertexLabel, rootNode.get(vertexLabel), userId);

        if (encryptionEnabled) {
            rootNode = encryptionHelper.getEncryptedJson(rootNode);
        }

        if (!skipSignature) {
            generateCredentials(rootNode, vertexLabel);
        }
        if (vertexLabel.equals(Schema)) {
            schemaService.addSchema(rootNode);
        }

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
                if (tx != null) {
                    tx.close();
                }
            }
            // Add indices: executes only once.
            if (perRequestIndexCreation) {
                String shardId = shard.getShardId();
                Vertex parentVertex = entityParenter.getKnownParentVertex(vertexLabel, shardId);
                Definition definition = definitionsManager.getDefinition(vertexLabel);
                entityParenter.ensureIndexExists(dbProvider, parentVertex, definition, shardId);
            }

            if (isElasticSearchEnabled()) {
                if (addShardPrefixForESRecord && !shard.getShardLabel().isEmpty()) {
                    // Replace osid with shard details
                    String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
                    JSONUtil.addPrefix((ObjectNode) rootNode, prefix, new ArrayList<>(Collections.singletonList(uuidPropertyName)));
                }
                JsonNode nodeWithPublicData = JsonNodeFactory.instance.objectNode().set(vertexLabel,
                        JSONUtil.removeNodesByPath(rootNode.get(vertexLabel), definitionsManager.getExcludingFieldsForEntity(vertexLabel)));
                callESActors(nodeWithPublicData, "ADD", vertexLabel, entityId, tx);
            }
            auditService.auditAdd(
                    auditService.createAuditRecord(userId, entityId, tx, vertexLabel),
                    shard, rootNode);



        }
        return entityId;
    }


    private void generateCredentials(JsonNode rootNode, String vertexLabel) throws SignatureException.UnreachableException, SignatureException.CreationException {
        Object credentialTemplate = definitionsManager.getCredentialTemplate(vertexLabel);
        if (signatureEnabled && credentialTemplate != null) {
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("data", rootNode.get(vertexLabel));
            requestBodyMap.put("credentialTemplate", credentialTemplate);
            Object signedCredentials = signatureService.sign(requestBodyMap);
            ((ObjectNode) rootNode.get(vertexLabel)).set(OSSystemFields._osSignedData.name(), JsonNodeFactory.instance.textNode(signedCredentials.toString()));
        }
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
            // TODO: need to revoke and re-sign the entity
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
                JSONUtil.trimPrefix((ObjectNode) inputNode, uuidPropertyName, prefix);
            }

            generateCredentials(inputNode, entityType);

            if (entityType.equals(Schema)) {
                schemaService.updateSchema(readNode, inputNode);
            }

            // The entity type is a child and so could be different from parent entity type.
            doUpdate(shard, graph, registryDao, vr, inputNode.get(entityType), entityType, null);

            databaseProvider.commitTransaction(graph, tx);

            if(isInternalRegistry(entityType) && isElasticSearchEnabled()) {
                if (addShardPrefixForESRecord && !shard.getShardLabel().isEmpty()) {
                    // Replace osid with shard details
                    String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
                    JSONUtil.addPrefix((ObjectNode) mergedNode, prefix, new ArrayList<>(Collections.singletonList(uuidPropertyName)));
                }
                JsonNode nodeWithPublicData = JsonNodeFactory.instance.objectNode().set(entityType,
                        JSONUtil.removeNodesByPath(mergedNode.get(entityType), definitionsManager.getExcludingFieldsForEntity(entityType)));
                callESActors(nodeWithPublicData, "UPDATE", entityType, id, tx);
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
        rootNode = rootNode != null ? rootNode.get(parentEntityType) : rootNode;
        boolean elasticSearchEnabled = isElasticSearchEnabled();
        MessageProtos.Message message = MessageFactory.instance().createOSActorMessage(elasticSearchEnabled, operation,
                parentEntityType.toLowerCase(), entityRootId, rootNode, null);
        ActorCache.instance().get(Router.ROUTER_NAME).tell(message, null);
        logger.debug("callESActors ends");
    }

    private boolean isElasticSearchEnabled() {
        return (searchProvider.equals("dev.sunbirdrc.registry.service.ElasticSearchService"));
    }

    @Override
    @Async("taskExecutor")
    public void callNotificationActors(String operation, String to, String subject, String message) throws JsonProcessingException {
        logger.debug("callNotificationActors started");
        MessageProtos.Message messageProto = MessageFactory.instance().createNotificationActorMessage(operation, to, subject, message);
        ActorCache.instance().get(Router.ROUTER_NAME).tell(messageProto, null);
        logger.debug("callNotificationActors ends");
    }

    private void doUpdateArray(Shard shard, Graph graph, IRegistryDao registryDao, VertexReader vr, Vertex blankArrVertex, ArrayNode arrayNode, String parentName) {
        HashMap<String, Vertex> uuidVertexMap = vr.getUuidVertexMap();
        Set<Object> updatedUuids = new HashSet<Object>();
        Set<String> previousArrayItemsUuids = vr.getArrayItemUuids(blankArrVertex);

        VertexWriter vertexWriter = new VertexWriter(graph, shard.getDatabaseProvider(), uuidPropertyName);

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

    /**
     * Delete the previous array items Uuids which are not updated
     *
     * @param registryDao
     * @param vr
     * @param previousArrayItemsUuids
     * @param updatedUuids
     */
    private void doDelete(IRegistryDao registryDao, VertexReader vr, Set<String> previousArrayItemsUuids, Set<Object> updatedUuids) {
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
     * @param inputNode    - the one passed by user
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