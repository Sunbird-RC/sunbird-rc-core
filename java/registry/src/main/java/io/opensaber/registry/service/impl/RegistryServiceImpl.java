package io.opensaber.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import io.opensaber.pojos.ComponentHealthInfo;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.dao.*;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.service.*;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.*;
import io.opensaber.validators.IValidate;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

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
    @Value("${encryption.enabled}")
    private boolean encryptionEnabled;

    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    @Value("${signature.enabled}")
    private boolean signatureEnabled;

    @Value("${persistence.enabled}")
    private boolean persistenceEnabled;

    @Value("${signature.domain}")
    private String signatureDomain;

    @Value("${signature.keysURL}")
    private String signatureKeyURl;

    @Value("${frame.file}")
    private String frameFile;

    @Value("${registry.context.base}")
    private String registryContextBase;

    @Value("${registry.context.base}")
    private String registryContext;

    @Autowired
    private Shard shard;

    @Autowired
    private EntityParenter entityParenter;

    public HealthCheckResponse health() throws Exception {
        HealthCheckResponse healthCheck;
        // TODO
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
     * delete the vertex and changing the status
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
            }
            logger.info("Entity {} marked deleted", uuid);
            databaseProvider.commitTransaction(graph, tx);
        }
    }

    public String addEntity(String jsonString) throws Exception {
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
                Transaction tx = dbProvider.startTransaction(graph);
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
        }

        return entityId;
    }

    @Override
    public JsonNode getEntity(String id, ReadConfigurator configurator) throws Exception {
        DatabaseProvider dbProvider = shard.getDatabaseProvider();
        IRegistryDao registryDao = new RegistryDaoImpl(dbProvider, definitionsManager, uuidPropertyName);
        try (OSGraph osGraph = dbProvider.getOSGraph()) {
            Graph graph = osGraph.getGraphStore();
            Transaction tx = dbProvider.startTransaction(graph);
            JsonNode result = registryDao.getEntity(graph, id, configurator);

            if (!shard.getShardLabel().isEmpty()) {
                // Replace osid with shard details
                String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
                JSONUtil.addPrefix((ObjectNode) result, prefix, new ArrayList<String>(Arrays.asList(uuidPropertyName)));
            }

            shard.getDatabaseProvider().commitTransaction(graph, tx);
            dbProvider.commitTransaction(graph, tx);
            return result;
        }
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
                Vertex oldEntitySignatureVertex = uuidVertexMap.get(entitySignUUID);

                registryDao.updateVertex(graph, oldEntitySignatureVertex, newSignature);
            }

            // TODO - Validate before update
            JsonNode validationNode = mergedNode.deepCopy();
            JSONUtil.removeNode((ObjectNode) validationNode, uuidPropertyName);
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
        String parentOsid = userInputNode.get(uuidPropertyName).textValue();
        Vertex existingVertex = uuidVertexMap.getOrDefault(parentOsid, null);

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
                        String arrayRefId = RefLabelHelper.getArrayLabel(oneElement.getKey(), uuidPropertyName);
                        Vertex existArrayVertex = uuidVertexMap.getOrDefault(rootVertex.value(arrayRefId), null);
                        if (null != existArrayVertex) {
                            // updateArrayItems one by one
                            doUpdateArray(graph, registryDao, vr, existArrayVertex, (ArrayNode) oneElementNode);
                        } else {
                            // New array - Imagine optional array
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
        // We know the user is likely to update less fields and so iterate over it.
        ObjectNode result = databaseNode.deepCopy();
        inputNode.fields().forEachRemaining(prop -> {
            merge(entityType, result, (ObjectNode) prop.getValue());
        });
        return result;
    }

    private void merge(String entityType, ObjectNode result, ObjectNode inputNode) {
        inputNode.fields().forEachRemaining(prop -> {
            String propKey = prop.getKey();
            JsonNode propValue = prop.getValue();

            if ((propValue.isValueNode() && !uuidPropertyName.equalsIgnoreCase(propKey)) ||
                propValue.isArray()) {
                // Must be a value node and not a uuidPropertyName key pair
                //((ObjectNode)result.get(entityType)).set(propKey, propValue);
                ((ObjectNode)result.at(entityType)).set(propKey, propValue);
            } else if (propValue.isObject()) {
                merge(entityType + "/" + propKey, result, (ObjectNode) propValue);
            }
        });
    }
}