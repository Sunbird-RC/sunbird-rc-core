package io.opensaber.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import io.opensaber.pojos.ComponentHealthInfo;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.dao.VertexReader;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.schema.configurator.ISchemaConfigurator;
import io.opensaber.registry.service.EncryptionHelper;
import io.opensaber.registry.service.EncryptionService;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.service.SignatureService;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.ReadConfigurator;
import io.opensaber.validators.IValidate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    EncryptionService encryptionService;
    @Autowired
    SignatureService signatureService;
    @Autowired
    Gson gson;
    @Autowired
    private IRegistryDao registryDao;
    @Autowired
    private ISchemaConfigurator schemaConfigurator;
    @Autowired
    private EncryptionHelper encryptionHelper;
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

    @Value("${registry.rootEntity.type}")
    private String registryRootEntityType;

    @Value("${registry.context.base}")
    private String registryContext;

    @Autowired
    RegistryDaoImpl tpGraphMain;
    
    @Autowired 
    private Shard shard;

    @Autowired
    DBConnectionInfoMgr dbConnectionInfoMgr;

    @Autowired
    private IValidate iValidate;

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

    @Override
    public boolean deleteEntityById(String id) throws AuditFailedException, RecordNotFoundException {
        return false;
    }

    public String addEntity(String jsonString) throws Exception {
        String entityId = "entityPlaceholderId";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonString);

        if (encryptionEnabled) {
            rootNode = encryptionHelper.getEncryptedJson(rootNode);
        }

        if (signatureEnabled) {
            /*Map signReq = new HashMap<String, Object>();
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(frameFile);
            String fileString = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
            Map<String, Object> reqMap = JSONUtil.frameJsonAndRemoveIds(ID_REGEX, dataObject, gson,
                    fileString);
            signReq.put("entity", reqMap);
            Map<String, Object> entitySignMap = (Map<String, Object>) signatureService.sign(signReq);
            entitySignMap.put("createdDate", rs.getCreatedTimestamp());
            entitySignMap.put("keyUrl", signatureKeyURl);
            signedRdfModel = RDFUtil.getUpdatedSignedModel(rdfModel, registryContext, signatureDomain,
                    entitySignMap, ModelFactory.createDefaultModel());
            rootLabel = addEntity(signedRdfModel, subject, property);*/
        }

        if (persistenceEnabled) {
            entityId = tpGraphMain.addEntity(rootNode);
        }

        return entityId;
    }

    public JsonNode getEntity(String id, ReadConfigurator configurator) {
        JsonNode result = tpGraphMain.getEntity(id, configurator);
        return result;
    }

    @Override
    public void updateEntity(String jsonString) throws Exception {
        Iterator<Vertex> vertexIterator;
        Vertex rootVertex;
        List<String> privatePropertyList = schemaConfigurator.getAllPrivateProperties();

        JsonNode rootNode = objectMapper.readTree(jsonString);
        rootNode = encryptionHelper.getEncryptedJson(rootNode);
        String idProp = rootNode.elements().next().get(uuidPropertyName).asText();
        JsonNode childElementNode = rootNode.elements().next();
        DatabaseProvider databaseProvider = shard.getDatabaseProvider();
        ReadConfigurator readConfigurator = new ReadConfigurator();
        readConfigurator.setIncludeSignatures(false);

        try (OSGraph osGraph = databaseProvider.getOSGraph()) {
            Graph graph = osGraph.getGraphStore();
            try (Transaction tx = databaseProvider.startTransaction(graph)) {
                VertexReader vr = new VertexReader(graph, readConfigurator, uuidPropertyName, privatePropertyList);
                vertexIterator = graph.vertices(idProp);
                rootVertex = vertexIterator.hasNext() ? vertexIterator.next() : null;
                ObjectNode entityNode = (ObjectNode) vr.read(rootVertex.id().toString());
                entityNode = merge(entityNode, rootNode);
                // TODO Fix validation fails here
                boolean isValidate = iValidate.validate(entityNode.toString(), "Teacher");
                tpGraphMain.updateVertex(rootVertex, childElementNode);
            }
        }
    }



    /** Merging input json node to DB entity node, this method in turn calls mergeDestinationWithSourceNode method for deep copy of properties and objects
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
     * @param entityNode
     * @param entityKey
     */
    private void mergeDestinationWithSourceNode(ObjectNode propKeyValue, ObjectNode entityNode, String entityKey) {
        ObjectNode subEntity = (ObjectNode) entityNode.get(entityKey);
        propKeyValue.fields().forEachRemaining(prop -> {
            if(prop.getValue().isValueNode()){
                subEntity.set(prop.getKey(),prop.getValue());
            } else if(prop.getValue().isObject()){
                if(subEntity.get(prop.getKey()).size() == 0) {
                    subEntity.set(prop.getKey(),prop.getValue());
                } else if (subEntity.get(prop.getKey()).isObject()) {
                    ArrayNode arrnode = JsonNodeFactory.instance.arrayNode();
                    arrnode.add(subEntity.get(prop.getKey()));
                    arrnode.add(prop.getValue());
                    subEntity.set(prop.getKey(),arrnode);
                }
            }
        });
    }

}