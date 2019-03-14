package io.opensaber.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.elastic.IElasticService;
import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.ComponentHealthInfo;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.dao.VertexReader;
import io.opensaber.registry.dao.VertexWriter;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.DateUtil;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.model.AuditInfo;
import io.opensaber.registry.model.AuditRecord;
import io.opensaber.registry.service.EncryptionHelper;
import io.opensaber.registry.service.EncryptionService;
import io.opensaber.registry.service.IAuditService;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.service.SignatureHelper;
import io.opensaber.registry.service.SignatureService;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.Definition;
import io.opensaber.registry.util.DefinitionsManager;
import io.opensaber.registry.util.EntityParenter;
import io.opensaber.registry.util.ReadConfigurator;
import io.opensaber.registry.util.ReadConfiguratorFactory;
import io.opensaber.registry.util.RecordIdentifier;
import io.opensaber.registry.util.RefLabelHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RegistryServiceImpl implements RegistryService {

    private static final String ID_REGEX = "\"@id\"\\s*:\\s*\"_:[a-z][0-9]+\",";
    private static Logger logger = LoggerFactory.getLogger(RegistryServiceImpl.class);

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
    @Autowired
    private IElasticService elasticService;
    @Autowired
    private IAuditService auditService;
    @Autowired
    private APIMessage apiMessage;
    @Value("${encryption.enabled}")
    private boolean encryptionEnabled;

    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    @Value("${signature.enabled}")
    private boolean signatureEnabled;

    @Value("${persistence.enabled}")
    private boolean persistenceEnabled;

    @Value("${elastic.search.enabled}")
    private boolean elasticSearchEnabled;

    @Autowired
    private Shard shard;

    @Autowired
    private EntityParenter entityParenter;

    private AuditRecord auditRecord;

    public void setShard(Shard shard) {
        this.shard = shard;
    }

    public HealthCheckResponse health() throws Exception {
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
    public void deleteEntityById(String uuid) throws Exception {
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
                if(elasticSearchEnabled) {
                    elasticService.deleteEntity(index, uuid);
                }
                auditRecord = new AuditRecord();
                AuditInfo auditInfo = new AuditInfo();
                auditInfo.setOp(Constants.AUDIT_ACTION_REMOVE_OP);
                auditInfo.setPath("/"+vertex.label());
                auditRecord.setAction(Constants.AUDIT_ACTION_DELETE).setUserId(apiMessage.getUserID()).setTransactionId(new LinkedList<>(Arrays.asList(tx.hashCode()))).
                        setRecordId(uuid).setAuditInfo(Arrays.asList(auditInfo)).setAuditId(UUID.randomUUID().toString()).
                        setTimeStamp(DateUtil.getTimeStamp());
                auditService.audit(auditRecord);
            }
            logger.info("Entity {} marked deleted", uuid);
        }
    }

    /**
     * This method adds the entity into db, calls elastic and audit asynchronously
     *
     * @param jsonString - input value as string
     * @return
     * @throws Exception
     */
    public String addEntity(String jsonString) throws Exception {
        Transaction tx = null;
        String entityId = "entityPlaceholderId";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonString);

        if (encryptionEnabled) {
            rootNode = encryptionHelper.getEncryptedJson(rootNode);
        }

        if (signatureEnabled) {
            signatureHelper.signJson(rootNode);
        }

        if (persistenceEnabled) {
            String vertexLabel = null;
            DatabaseProvider dbProvider = shard.getDatabaseProvider();
            IRegistryDao registryDao = new RegistryDaoImpl(dbProvider, definitionsManager, uuidPropertyName);
            try (OSGraph osGraph = dbProvider.getOSGraph()) {
                Graph graph = osGraph.getGraphStore();
                tx = dbProvider.startTransaction(graph);
                entityId = registryDao.addEntity(graph, rootNode);
                shard.getDatabaseProvider().commitTransaction(graph, tx);
                dbProvider.commitTransaction(graph, tx);

                vertexLabel = rootNode.fieldNames().next();
            }
            //Add indices: executes only once.
            String shardId = shard.getShardId();
            Vertex parentVertex = entityParenter.getKnownParentVertex(vertexLabel, shardId);
            Definition definition = definitionsManager.getDefinition(vertexLabel);
            entityParenter.ensureIndexExists(dbProvider, parentVertex, definition, shardId);
            //call to elastic search
            if(elasticSearchEnabled) {
                elasticService.addEntity(vertexLabel.toLowerCase(), entityId, rootNode);
            }
            auditRecord = new AuditRecord();
            auditRecord.setAction(Constants.AUDIT_ACTION_ADD).setUserId(apiMessage.getUserID()).setLatestNode(rootNode).setTransactionId(new LinkedList<>(Arrays.asList(tx.hashCode()))).
                    setRecordId(entityId).setAuditId(UUID.randomUUID().toString()).setTimeStamp(DateUtil.getTimeStamp());
            auditService.audit(auditRecord);
        }

        return entityId;
    }

    /**
     * This method interacts with the Elasticsearch and reads the record, if the record not found call is shifted to native db
     *
     * @param id           - osid
     * @param entityType   - elastic-search index
     * @param configurator
     * @return
     * @throws Exception
     */
    @Override
    public JsonNode getEntity(String id, String entityType, ReadConfigurator configurator) throws Exception {
        JsonNode result = null;
        Map<String, Object> response = elasticService.readEntity(entityType.toLowerCase(), id);
        result = response != null ? objectMapper.convertValue(response, JsonNode.class) : null;
        if (result == null) {
            DatabaseProvider dbProvider = shard.getDatabaseProvider();
            IRegistryDao registryDao = new RegistryDaoImpl(dbProvider, definitionsManager, uuidPropertyName);
            try (OSGraph osGraph = dbProvider.getOSGraph()) {
                Graph graph = osGraph.getGraphStore();
                Transaction tx = dbProvider.startTransaction(graph);
                result = registryDao.getEntity(graph, id, configurator);

                if (!shard.getShardLabel().isEmpty()) {
                    // Replace osid with shard details
                    String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
                    JSONUtil.addPrefix((ObjectNode) result, prefix, new ArrayList<String>(Arrays.asList(uuidPropertyName)));
                }

                shard.getDatabaseProvider().commitTransaction(graph, tx);
                dbProvider.commitTransaction(graph, tx);
            }
        }
        return result;
    }

    @Override
    public void updateEntity(String id, String jsonString) throws Exception {
        JsonNode inputNode = objectMapper.readTree(jsonString);
        String entityType = inputNode.fields().next().getKey();

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
            }
            String parentEntityType = readNode.fields().next().getKey();
            HashMap<String, Vertex> uuidVertexMap = vr.getUuidVertexMap();

            // Merge the new changes
            JsonNode mergedNode = mergeWrapper("/" + parentEntityType, (ObjectNode) readNode, (ObjectNode) inputNode);
            logger.debug("After merge the payload is " + mergedNode.toString());

            // Re-sign, i.e., remove and add entity signature again
            if (signatureEnabled) {
                logger.debug("Removing earlier signature and adding new one");
                String entitySignUUID = signatureHelper.removeEntitySignature(parentEntityType, (ObjectNode) mergedNode);
                JsonNode newSignature = signatureHelper.signJson(mergedNode);
                String rootOsid = mergedNode.get(entityType).get(uuidPropertyName).asText();
                ObjectNode objectSignNode = (ObjectNode) newSignature;
                objectSignNode.put(uuidPropertyName, entitySignUUID);
                objectSignNode.put(Constants.ROOT_KEYWORD, rootOsid);
                Vertex oldEntitySignatureVertex = uuidVertexMap.get(entitySignUUID);

                registryDao.updateVertex(graph, oldEntitySignatureVertex, newSignature);
            }

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
                JSONUtil.trimPrefix((ObjectNode) inputNode, prefix);
            }

            // The entity type is a child and so could be different from parent entity type.
            doUpdate(graph, registryDao, vr, inputNode.get(entityType));

            databaseProvider.commitTransaction(graph, tx);
            // elastic-search updation starts here
            logger.info("updating node {} " ,mergedNode);
            if(elasticSearchEnabled) {
                elasticService.updateEntity(parentEntityType,rootId,mergedNode);
            }
            auditRecord = new AuditRecord();
            auditRecord.setUserId(apiMessage.getUserID()).setAction(Constants.AUDIT_ACTION_UPDATE).setExistingNode(readNode)
                    .setLatestNode(mergedNode).setTransactionId(new LinkedList<>(Arrays.asList(tx.hashCode()))).setUserId(id).setRecordId(id).
                    setAuditId(UUID.randomUUID().toString()).setTimeStamp(DateUtil.getTimeStamp());
            auditService.audit(auditRecord);
        }
    }

    private void doUpdateArray(Graph graph, IRegistryDao registryDao, VertexReader vr, Vertex blankArrVertex, ArrayNode arrayNode) {
        HashMap<String, Vertex> uuidVertexMap = vr.getUuidVertexMap();
        Set<String> updatedUuids = new HashSet<>();

        for (JsonNode item : arrayNode) {
            if (item.isObject()) {
                if (item.get(uuidPropertyName) != null && item.get(uuidPropertyName) != null) {
                    Vertex existingItem = uuidVertexMap.getOrDefault(item.get(uuidPropertyName).textValue(), null);
                    if (existingItem != null) {
                        try {
                            registryDao.updateVertex(graph, existingItem, item);
                        } catch (Exception e) {
                            logger.error("Can't update item {}", item.toString());
                        }
                        updatedUuids.add(item.get(uuidPropertyName).textValue());
                    } else {
                        // New item got added.
                        VertexWriter vertexWriter = new VertexWriter(graph, shard.getDatabaseProvider() , uuidPropertyName);
                        vertexWriter.writeSingleNode(blankArrVertex, vr.getInternalType(blankArrVertex), item);
                    }
                }
            }
        }

        doDelete(registryDao, vr, blankArrVertex, updatedUuids);
    }

    private void doDelete(IRegistryDao registryDao, VertexReader vr, Vertex blankArrVertex, Set<String> updatedUuids) {
        HashMap<String, Vertex> uuidVertexMap = vr.getUuidVertexMap();
        Set<String> previousArrayItemsUuids = vr.getArrayItemUuids(blankArrVertex);
        for (String itemUuid : previousArrayItemsUuids) {
            if (!updatedUuids.contains(itemUuid)) {
                // delete this item
                registryDao.deleteEntity(uuidVertexMap.get(itemUuid));
            }
        }
    }

    private void doUpdate(Graph graph, IRegistryDao registryDao, VertexReader vr, JsonNode userInputNode) throws Exception {
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
                            doUpdateArray(graph, registryDao, vr, existArrayVertex, (ArrayNode) oneElementNode);
                        } else {
                            VertexWriter vertexWriter = new VertexWriter(graph, shard.getDatabaseProvider(), uuidPropertyName);
                            vertexWriter.createArrayNode(rootVertex, oneElement.getKey(), (ArrayNode) oneElementNode);
                        }
                        registryDao.updateVertex(graph, existArrayVertex, oneElementNode);
                    } else {
                        registryDao.updateVertex(graph, existingVertex, userInputNode);
                    }
                } else if (oneElementNode.isObject()) {
                    logger.info("Object node {}", oneElement.toString());
                    doUpdate(graph, registryDao, vr, oneElementNode);
                }
            }
        } else {
            // Likely a new addition
            logger.info("Adding a new node to existing one");
            registryDao.updateVertex(graph, rootVertex, userInputNode);
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