package io.opensaber.registry.service.impl;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.google.gson.*;
import io.opensaber.pojos.*;
import io.opensaber.registry.dao.*;
import io.opensaber.registry.exception.*;
import io.opensaber.registry.middleware.util.*;
import io.opensaber.registry.model.*;
import io.opensaber.registry.service.*;
import io.opensaber.registry.sink.*;
import io.opensaber.registry.sink.shard.*;
import io.opensaber.registry.util.*;
import org.apache.tinkerpop.gremlin.structure.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.util.*;

@Component
public class RegistryServiceImpl implements RegistryService {

    private static final String ID_REGEX = "\"@id\"\\s*:\\s*\"_:[a-z][0-9]+\",";
    private static Logger logger = LoggerFactory.getLogger(RegistryServiceImpl.class);

    @Autowired
    EncryptionService encryptionService;
    @Autowired
    SignatureService signatureService;
    @Autowired
    Gson gson;
    @Autowired
    private IRegistryDao registryDao;
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
    DBConnectionInfoMgr dbConnectionInfoMgr;

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
        try (OSGraph osGraph = dbProvider.getOSGraph()) {
            Graph graph = osGraph.getGraphStore();
            Transaction tx = dbProvider.startTransaction(graph);
            JsonNode result = registryDao.getEntity(graph, id, configurator);
            shard.getDatabaseProvider().commitTransaction(graph, tx);
            dbProvider.commitTransaction(graph, tx);
            return result;
        }
    }

    @Override
    public void updateEntity(String id, String jsonString) throws Exception {
        Iterator<Vertex> vertexIterator = null;
        Vertex inputNodeVertex = null;
        Vertex rootVertex = null;

        JsonNode rootNode = objectMapper.readTree(jsonString);

        if (encryptionEnabled) {
            rootNode = encryptionHelper.getEncryptedJson(rootNode);
        }

        JsonNode childElementNode = rootNode.elements().next();
        DatabaseProvider databaseProvider = shard.getDatabaseProvider();
        ReadConfigurator readConfigurator = ReadConfiguratorFactory.getForUpdateValidation();

        try (OSGraph osGraph = databaseProvider.getOSGraph()) {
            Graph graph = osGraph.getGraphStore();
            Transaction tx = databaseProvider.startTransaction(graph);
            VertexReader vr = new VertexReader(databaseProvider, graph, readConfigurator, uuidPropertyName,
                    definitionsManager);
            String entityNodeType;

            if (null != tx) {
                ObjectNode entityNode = null;
                ReadConfigurator configurator = ReadConfiguratorFactory.getOne(true);
                VertexReader vertexReader = new VertexReader(databaseProvider, graph, configurator, uuidPropertyName, definitionsManager);
                inputNodeVertex = vertexReader.getVertex(null, id);
                if ((inputNodeVertex.property(Constants.STATUS_KEYWORD).isPresent() && inputNodeVertex
                        .property(Constants.STATUS_KEYWORD).value().equals(Constants.STATUS_INACTIVE))) {
                    throw new RecordNotFoundException("Cannot perform the operation");
                }
                if (inputNodeVertex.property(Constants.ROOT_KEYWORD).isPresent()) {
                    rootVertex = vertexReader.getVertex(null, inputNodeVertex.property(Constants.ROOT_KEYWORD).value().toString());
                } else {
                    rootVertex = inputNodeVertex;
                }

                entityNode = (ObjectNode) vr.read(shard.getDatabaseProvider().getId(rootVertex));

                // merge with entitynode
                entityNode = merge(entityNode, rootNode);
                entityNodeType = entityNode.fields().next().getKey();
                // TO-DO validation is failing
                // boolean isValidate =
                // iValidate.validate("Teacher",entityNode.toString());
                registryDao.updateVertex(graph, inputNodeVertex, childElementNode);
                // sign the entitynode
                if (signatureEnabled) {
                    signatureHelper.signJson(entityNode);
                    JsonNode signNode = signatureHelper.getItemSignature(entityNodeType,
                            entityNode.get(entityNodeType).get(Constants.SIGNATURES_STR));

                    Iterator<Vertex> vertices = rootVertex.vertices(Direction.IN, Constants.SIGNATURES_STR);
                    if (null != vertices && vertices.hasNext()) {
                        Vertex signArrayNode = vertices.next();
                        Iterator<Vertex> sign = signArrayNode.vertices(Direction.OUT, signatureHelper.getEntitySignaturePrefix() + entityNodeType);
                        Vertex signVertex = sign.next();
                        // Other signatures are not updated, only the entity
                        // level signature.
                        registryDao.updateVertex(graph, signVertex, signNode);
                    }
                }
                databaseProvider.commitTransaction(graph, tx);
            } else {
                // TinkerGraph section for test cases
                vertexIterator = graph.vertices(new Long(id));
                inputNodeVertex = vertexIterator.hasNext() ? vertexIterator.next() : null;
                ObjectNode entityNode = (ObjectNode) vr.read(inputNodeVertex.id().toString());
                entityNode = merge(entityNode, rootNode);
                entityNodeType = entityNode.fields().next().getKey();

                // TO-DO validation is failing
                // boolean isValidate =
                // iValidate.validate("Teacher",entityNode.toString());
                registryDao.updateVertex(graph, inputNodeVertex, childElementNode);

                // sign the entitynode
                if (signatureEnabled) {
                    signatureHelper.signJson(entityNode);
                    JsonNode signNode = signatureHelper.getItemSignature(entityNodeType,
                            entityNode.get(entityNodeType).get(Constants.SIGNATURES_STR));
                    Iterator<Vertex> vertices = rootVertex.vertices(Direction.IN, Constants.SIGNATURES_STR);
                    while (null != vertices && vertices.hasNext()) {
                        Vertex signVertex = vertices.next();
                        if (signVertex.property(Constants.SIGNATURE_FOR).value().equals(entityNodeType)) {
                            registryDao.updateVertex(graph, signVertex, signNode);
                        }
                    }

                }
            }
        }

    }

    /**
     * Merging input json node to DB entity node, this method in turn calls
     * mergeDestinationWithSourceNode method for deep copy of properties and
     * objects
     *
     * @param entityNode
     * @param rootNode
     * @return
     */
    private ObjectNode merge(ObjectNode entityNode, JsonNode rootNode) {
        rootNode.fields().forEachRemaining(entryJsonNode -> {
            ObjectNode propKeyValue = (ObjectNode) entryJsonNode.getValue();
            mergeDestinationWithSourceNode(propKeyValue, entityNode, entryJsonNode.getKey());
        });
        return entityNode;
    }

    /**
     * @param propKeyValue
     *            - user given entity node
     * @param entityNode
     *            - read from the database
     * @param entityKey
     *            - user given entity key (wrapper node supplied by the user)
     */
    private void mergeDestinationWithSourceNode(ObjectNode propKeyValue, ObjectNode entityNode, String entityKey) {
        ObjectNode subEntity = (ObjectNode) entityNode.findValue(entityKey);
        propKeyValue.fields().forEachRemaining(prop -> {
            String propKey = prop.getKey();
            JsonNode propValue = prop.getValue();
            if (propValue.isValueNode() && !uuidPropertyName.equalsIgnoreCase(propKey)) {
                subEntity.set(propKey, propValue);
            } else if (propValue.isObject()) {
                if (subEntity.get(propKey).size() == 0) {
                    subEntity.set(propKey, propValue);
                } else if (subEntity.get(propKey).isObject()) {
                    // As of now filtering only @type
                    List<String> filterKeys = Arrays.asList(Constants.JsonldConstants.TYPE);
                    // removing keys with name osid and type
                    JSONUtil.removeNodes((ObjectNode) subEntity.get(propKey), filterKeys);
                    // constructNewNodeToParent
                    subEntity.set(propKey, propValue);
                }
            } else if (subEntity.get(propKey).isArray()) {
                List<String> filterKeys = Arrays.asList(Constants.JsonldConstants.TYPE);
                propValue.forEach(arrayElement -> {
                    if(arrayElement.isObject()){
                        // removing keys with name @type
                        JSONUtil.removeNodes((ObjectNode) arrayElement, filterKeys);
                    }
                });
                // constructNewNodeToParent
                subEntity.set(propKey, propValue);
            }
        });
    }

}