package dev.sunbirdrc.registry.helper;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonPatch;
import dev.sunbirdrc.keycloak.KeycloakAdminUtil;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.pojos.attestation.Action;
import dev.sunbirdrc.pojos.attestation.States;
import dev.sunbirdrc.pojos.attestation.exception.PolicyNotFoundException;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.exception.SignatureException;
import dev.sunbirdrc.registry.exception.UnAuthorizedException;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.middleware.util.OSSystemFields;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.model.attestation.EntityPropertyURI;
import dev.sunbirdrc.registry.service.*;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.sink.shard.ShardManager;
import dev.sunbirdrc.registry.util.*;
import dev.sunbirdrc.validators.IValidate;
import dev.sunbirdrc.views.ViewTemplate;
import dev.sunbirdrc.views.ViewTransformer;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.util.*;

import static dev.sunbirdrc.registry.Constants.*;
import static dev.sunbirdrc.registry.exception.ErrorMessages.*;
import static dev.sunbirdrc.registry.middleware.util.Constants.EMAIL;
import static dev.sunbirdrc.registry.middleware.util.Constants.MOBILE;
import static dev.sunbirdrc.registry.middleware.util.OSSystemFields.*;

/**
 * This is helper class, user-service calls this class in-order to access registry functionality
 */
@Component
@Setter
public class RegistryHelper {

    private static final String ATTESTED_DATA = "attestedData";
    private static final String CLAIM_ID = "claimId";
    public static String ROLE_ANONYMOUS = "anonymous";

    private static final Logger logger = LoggerFactory.getLogger(RegistryHelper.class);

    @Autowired
    private ShardManager shardManager;

    @Autowired
    RegistryService registryService;

    @Autowired
    IReadService readService;

    @Autowired
    IValidate validationService;

    @Autowired
    private ISearchService searchService;

    @Autowired
    private ViewTemplateManager viewTemplateManager;

    @Autowired
    EntityStateHelper entityStateHelper;

    @Autowired
    private KeycloakAdminUtil keycloakAdminUtil;

    @Autowired
    private DefinitionsManager definitionsManager;

    @Autowired
    private DBConnectionInfoMgr dbConnectionInfoMgr;

    @Autowired
    private DecryptionHelper decryptionHelper;

    @Autowired
    private SunbirdRCInstrumentation watch;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    @Value("${audit.frame.suffix}")
    public String auditSuffix;

    @Value("${audit.frame.suffixSeparator}")
    public String auditSuffixSeparator;

    @Value("${conditionalAccess.internal}")
    private String internalFieldsProp;

    @Value("${conditionalAccess.private}")
    private String privateFieldsProp;

    @Value("${signature.enabled}")
    private boolean signatureEnabled;

    @Autowired
    private EntityTypeHandler entityTypeHandler;

    public String getAttestationOSID(JsonNode requestBody, String entityName, String entityId, String propertyName) throws Exception {
        JsonNode resultNode = readEntity("", entityName, entityId, false, null, false)
                .get(entityName)
                .get(propertyName);
        List<String> fieldsToRemove = getFieldsToRemove(entityName);
        return JSONUtil.getOSIDFromArrNode(resultNode, requestBody, fieldsToRemove);
    }

    @Autowired
    private SignatureService signatureService;

    public JsonNode removeFormatAttr(JsonNode requestBody) {
        String documents = "documents";
        if (requestBody.has(documents)) {
            JsonNode documentsNode = requestBody.get(documents);
            String format = "format";
            JSONUtil.removeNodes(documentsNode, Collections.singletonList(format));
            ObjectNode node = (ObjectNode) requestBody;
            node.set(documents, documentsNode);
            return node;
        }
        return requestBody;
    }

    /**
     * calls validation and then persists the record to registry.
     *
     * @param inputJson
     * @return
     * @throws Exception
     */
    public String addEntityAndSign(JsonNode inputJson, String userId) throws Exception {
        String entityType = inputJson.fields().next().getKey();
//        validationService.validate(entityType, objectMapper.writeValueAsString(inputJson), false);
//        ObjectNode existingNode = objectMapper.createObjectNode();
//        existingNode.set(entityType, objectMapper.createObjectNode());
//        entityStateHelper.applyWorkflowTransitions(existingNode, inputJson);
        String objectId = addEntityHandler(inputJson, userId, false);
        signDocument(entityType, objectId, userId);
        return objectId;
    }

    public String inviteEntity(JsonNode inputJson, String userId) throws Exception {
        String entityId = addEntityHandler(inputJson, userId, true);
        sendInviteNotification(inputJson);
        return entityId;
    }

    public String addEntityWithoutValidation(JsonNode inputJson, String userId, String entityName) throws Exception {
        return addEntity(inputJson, userId, entityName);
    }

    private String addEntityHandler(JsonNode inputJson, String userId, boolean isInvite) throws Exception {
        String entityType = inputJson.fields().next().getKey();
        validationService.validate(entityType, objectMapper.writeValueAsString(inputJson), isInvite);
        String entityName = inputJson.fields().next().getKey();
        List<AttestationPolicy> attestationPolicies = getAttestationPolicies(entityName);
        entityStateHelper.applyWorkflowTransitions(JSONUtil.convertStringJsonNode("{}"), inputJson, attestationPolicies);
        if (!StringUtils.isEmpty(userId)) {
            ArrayNode jsonNode = (ArrayNode) inputJson.get(entityName).get(osOwner.toString());
            if (jsonNode == null) {
                jsonNode = new ObjectMapper().createArrayNode();
                ((ObjectNode) inputJson.get(entityName)).set(osOwner.toString(), jsonNode);
            }
            jsonNode.add(userId);
        }
        return addEntity(inputJson, userId, entityType);
    }

    private void sendInviteNotification(JsonNode inputJson) throws Exception {
        String entityType = inputJson.fields().next().getKey();
        sendNotificationToOwners(inputJson, INVITE, String.format(INVITE_SUBJECT_TEMPLATE, entityType), String.format(INVITE_BODY_TEMPLATE, entityType));
    }

    private void sendNotificationToOwners(JsonNode inputJson, String operation, String subject, String message) throws Exception {
        String entityType = inputJson.fields().next().getKey();
        for (ObjectNode owners : entityStateHelper.getOwnersData(inputJson, entityType)) {
            String ownerMobile = owners.get(MOBILE).asText("");
            String ownerEmail = owners.get(EMAIL).asText("");
            if (!StringUtils.isEmpty(ownerMobile)) {
                registryService.callNotificationActors(operation, String.format("tel:%s", ownerMobile), subject, message);
            }
            if (!StringUtils.isEmpty(ownerEmail)) {
                registryService.callNotificationActors(operation, String.format("mailto:%s", ownerEmail), subject, message);
            }
        }
    }

    private String addEntity(JsonNode inputJson, String userId, String entityType) throws Exception {
        RecordIdentifier recordId = null;
        try {
            logger.info("Add api: entity type: {} and shard propery: {}", entityType, shardManager.getShardProperty());
            Shard shard = shardManager.getShard(inputJson.get(entityType).get(shardManager.getShardProperty()));
            watch.start("RegistryController.addToExistingEntity");
            String resultId = registryService.addEntity(shard, userId, inputJson);
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
     *
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
        boolean includePrivateFields = false;
        JsonNode resultNode = null;
        RecordIdentifier recordId = RecordIdentifier.parse(label);
        String shardId = dbConnectionInfoMgr.getShardId(recordId.getShardLabel());
        Shard shard = shardManager.activateShard(shardId);
        logger.info("Read Api: shard id: " + recordId.getShardLabel() + " for label: " + label);
        ReadConfigurator configurator = ReadConfiguratorFactory.getOne(includeSignatures);
        configurator.setIncludeTypeAttributes(requireLDResponse);
        if (viewTemplate != null) {
            includePrivateFields = viewTemplateManager.isPrivateFieldEnabled(viewTemplate, entityType);
        }
        configurator.setIncludeEncryptedProp(includePrivateFields);
        resultNode = readService.getEntity(shard, userId, recordId.getUuid(), entityType, configurator);
        if (!isOwner(resultNode.get(entityType), userId)) {
//            throw new Exception("Unauthorized");
            //TODO: return public fields
        }
        if (viewTemplate != null) {
            ViewTransformer vTransformer = new ViewTransformer();
            resultNode = includePrivateFields ? decryptionHelper.getDecryptedJson(resultNode) : resultNode;
            resultNode = vTransformer.transform(viewTemplate, resultNode);
        }
        logger.debug("readEntity ends");
        return resultNode;
    }

    private boolean isOwner(JsonNode entity, String userId) {
        String osOwner = OSSystemFields.osOwner.toString();
        return userId != null && (!entity.has(osOwner) || entity.get(osOwner).toString().contains(userId));
    }

    /**
     * Get entity details from the DB and modifies data according to view template, requests which need only json format can call this method
     *
     * @param inputJson
     * @return
     * @throws Exception
     */
    public JsonNode readEntity(JsonNode inputJson, String userId) throws Exception {
        return readEntity(inputJson, userId, false);
    }

    /**
     * Search the input in the configured backend, external api's can use this method for searching
     *
     * @param inputJson
     * @return
     * @throws Exception
     */
    public JsonNode searchEntity(JsonNode inputJson) throws Exception {
        logger.debug("searchEntity starts");
        JsonNode resultNode = searchService.search(inputJson);
        removeNonPublicFields((ObjectNode) resultNode);
        ViewTemplate viewTemplate = viewTemplateManager.getViewTemplate(inputJson);
        if (viewTemplate != null) {
            ViewTransformer vTransformer = new ViewTransformer();
            resultNode = vTransformer.transform(viewTemplate, resultNode);
        }
        // Search is tricky to support LD. Needs a revisit here.
        logger.debug("searchEntity ends");
        return resultNode;
    }

    private void removeNonPublicFields(ObjectNode searchResultNode) throws Exception {
        ObjectReader stringListReader = objectMapper.readerFor(new TypeReference<List<String>>() {
        });
        List<String> nonPublicNodePathContainers = Arrays.asList(internalFieldsProp, privateFieldsProp);
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = searchResultNode.fields();
        while (fieldIterator.hasNext()) {
            ArrayNode entityResults = (ArrayNode) fieldIterator.next().getValue();
            for (int i = 0; i < entityResults.size(); i++) {
                ObjectNode entityResult = (ObjectNode) entityResults.get(i);
                List<String> nodePathsForRemoval = new ArrayList<>();
                for (String nodePathContainer : nonPublicNodePathContainers) {
                    if (entityResult.has(nodePathContainer)) {
                        nodePathsForRemoval.addAll(stringListReader.readValue(entityResult.get(nodePathContainer)));
                    }
                }
                JSONUtil.removeNodesByPath(entityResult, nodePathsForRemoval);
                entityResult.remove(nonPublicNodePathContainers);
            }
        }
    }

    /**
     * Updates the input entity, external api's can use this method to update the entity
     *
     * @param inputJson
     * @param userId
     * @return
     * @throws Exception
     */
    private String updateEntity(JsonNode inputJson, String userId) throws Exception {
        logger.debug("updateEntity starts");
        String entityType = inputJson.fields().next().getKey();
        String jsonString = objectMapper.writeValueAsString(inputJson);
        validationService.validate(entityType, jsonString, true);
        Shard shard = shardManager.getShard(inputJson.get(entityType).get(shardManager.getShardProperty()));
        String label = inputJson.get(entityType).get(dbConnectionInfoMgr.getUuidPropertyName()).asText();
        RecordIdentifier recordId = RecordIdentifier.parse(label);
        logger.info("Update Api: shard id: " + recordId.getShardLabel() + " for uuid: " + recordId.getUuid());
        registryService.updateEntity(shard, userId, recordId.getUuid(), jsonString);
        logger.debug("updateEntity ends");
        return "SUCCESS";
    }

    public String updateProperty(JsonNode inputJson, String userId) throws Exception {
        logger.debug("updateEntity starts");
        String entityType = inputJson.fields().next().getKey();
        String jsonString = objectMapper.writeValueAsString(inputJson);
        Shard shard = shardManager.getShard(inputJson.get(entityType).get(shardManager.getShardProperty()));
        String label = inputJson.get(entityType).get(dbConnectionInfoMgr.getUuidPropertyName()).asText();
        RecordIdentifier recordId = RecordIdentifier.parse(label);
        logger.info("Update Api: shard id: " + recordId.getShardLabel() + " for uuid: " + recordId.getUuid());
        registryService.updateEntity(shard, userId, recordId.getUuid(), jsonString);
        return "SUCCESS";
    }

    public String updateEntityAndState(JsonNode inputJson, String userId) throws Exception {
        JsonNode existingNode = readEntity(inputJson, userId);
        return updateEntityAndState(existingNode, inputJson, userId);
    }

    @Async
    public void triggerAutoAttestor(String entityName, String entityId, HttpServletRequest request, JsonNode existingNode) throws Exception {
        JsonNode updatedNode = readEntity("", entityName, entityId, false, null, false);
        registryService.callAutoAttestationActor(existingNode.get(entityName), updatedNode.get(entityName), entityName, entityId, request);
    }

    private String updateEntityAndState(JsonNode existingNode, JsonNode updatedNode, String userId) throws Exception {
        String entityName = updatedNode.fields().next().getKey();
        List<AttestationPolicy> attestationPolicies = getAttestationPolicies(entityName);
        entityStateHelper.applyWorkflowTransitions(existingNode, updatedNode, attestationPolicies);
        return updateEntity(updatedNode, userId);
    }

    public void addEntityProperty(String entityName, String entityId, JsonNode inputJson, HttpServletRequest request) throws Exception {
        String propertyURI = getPropertyURI(entityId, request);
        JsonNode existingNode = readEntity("", entityName, entityId, false, null, false);
        JsonNode updateNode = existingNode.deepCopy();

        JsonPointer propertyURIPointer = JsonPointer.compile("/" + propertyURI);
        String propertyName = propertyURIPointer.last().getMatchingProperty();
        String parentURIPointer = propertyURIPointer.head().toString();

        JsonNode parentNode = getParentNode(entityName, updateNode, parentURIPointer);
        JsonNode propertyNode = parentNode.get(propertyName);

        createOrUpdateProperty(entityName, inputJson, updateNode, propertyName, (ObjectNode) parentNode, propertyNode);
        updateEntityAndState(existingNode, updateNode, "");
    }

    public String addAttestationProperty(String entityName, String entityId, String propertyName, JsonNode inputJson, HttpServletRequest request) throws Exception {
        String userId = getUserId(request, entityName);
        JsonNode existingEntityNode = readEntity(userId, entityName, entityId, false, null, false);
        JsonNode nodeToUpdate = existingEntityNode.deepCopy();
        JsonNode parentNode = nodeToUpdate.get(entityName);
        JsonNode propertyNode = parentNode.get(propertyName);
        createOrUpdateProperty(entityName, inputJson, nodeToUpdate, propertyName, (ObjectNode) parentNode, propertyNode);
        return updateEntityAndState(existingEntityNode, nodeToUpdate, userId);
    }

    private void createOrUpdateProperty(String entityName, JsonNode inputJson, JsonNode updateNode, String propertyName, ObjectNode parentNode, JsonNode propertyNode) throws JsonProcessingException {
        if (propertyNode != null && !propertyNode.isMissingNode()) {
            updateProperty(inputJson, propertyName, parentNode, propertyNode);
        } else {
            // if array property
            createProperty(entityName, inputJson, updateNode, propertyName, parentNode);
        }
    }

    private void createProperty(String entityName, JsonNode inputJson, JsonNode updateNode, String propertyName, ObjectNode parentNode) throws JsonProcessingException {
        ArrayNode newPropertyNode = objectMapper.createArrayNode().add(inputJson);
        parentNode.set(propertyName, newPropertyNode);
        try {
            validationService.validate(entityName, objectMapper.writeValueAsString(updateNode), false);
        } catch (MiddlewareHaltException me) {
            // try a field node since array validation failed
            parentNode.set(propertyName, inputJson);
        }
    }

    private void updateProperty(JsonNode inputJson, String propertyName, ObjectNode parentNode, JsonNode propertyNode) {
        if (propertyNode.isArray()) {
            ((ArrayNode) propertyNode).add(inputJson);
        } else if (propertyNode.isObject()) {
            inputJson.fields().forEachRemaining(f -> {
                ((ObjectNode) propertyNode).set(f.getKey(), f.getValue());
            });
        } else {
            parentNode.set(propertyName, inputJson);
        }
    }

    private JsonNode getParentNode(String entityName, JsonNode jsonNode, String parentURIPointer) throws Exception {
        JsonNode parentNode;
        if (parentURIPointer.equals("")) {
            parentNode = jsonNode.get(entityName);
        } else {
            Optional<EntityPropertyURI> parentURI = EntityPropertyURI.fromEntityAndPropertyURI(
                    jsonNode.get(entityName),
                    parentURIPointer,
                    uuidPropertyName
            );
            if (!parentURI.isPresent()) {
                throw new Exception(parentURI + " does not exist");
            }
            parentNode = jsonNode.get(entityName).at(parentURI.get().getJsonPointer());
        }
        return parentNode;
    }

    public void updateEntityProperty(String entityName, String entityId, JsonNode inputJson, HttpServletRequest request) throws Exception {
        String propertyURI = getPropertyURI(entityId, request);
        JsonNode existingNode = readEntity("", entityName, entityId, false, null, false);
        JsonNode updateNode = existingNode.deepCopy();

        Optional<EntityPropertyURI> entityPropertyURI = EntityPropertyURI
                .fromEntityAndPropertyURI(updateNode.get(entityName), propertyURI, uuidPropertyName);

        if (!entityPropertyURI.isPresent()) {
            throw new Exception(propertyURI + ": do not exist");
        }

        JsonNode existingPropertyNode = updateNode.get(entityName).at(entityPropertyURI.get().getJsonPointer());
        JsonNode propertyParentNode = updateNode.get(entityName).at(entityPropertyURI.get().getJsonPointer().head());
        String propertyName = entityPropertyURI.get().getJsonPointer().last().getMatchingProperty();

        if (propertyParentNode.isObject()) {
            ((ObjectNode) propertyParentNode).set(propertyName, inputJson);
        } else if (existingPropertyNode.isObject()) {
            inputJson.fields().forEachRemaining(f -> ((ObjectNode) existingPropertyNode).set(f.getKey(), f.getValue()));
        } else {
            int propertyIndex = Integer.parseInt(propertyName);
            ((ArrayNode) propertyParentNode).set(propertyIndex, inputJson);
        }
        updateEntityAndState(existingNode, updateNode, "");

    }

    public void attestEntity(String entityName, JsonNode node, String[] jsonPaths, String userId) throws Exception {
        String patch = String.format("[{\"op\":\"add\", \"path\": \"attested\", \"value\": {\"attestation\":{\"id\":\"%s\"}, \"path\": \"%s\"}}]", userId, jsonPaths[0]);
        JsonPatch.applyInPlace(objectMapper.readTree(patch), node.get(entityName));
        updateEntity(node, userId);
    }

    public void sendForAttestation(String entityName, String entityId, String notes, HttpServletRequest request, String propertyId) throws Exception {
        String propertyURI = getPropertyURI(entityId, request);
        if (!propertyId.isEmpty()) {
            propertyURI = propertyURI + "/" + propertyId;
        }
        JsonNode entityNode = readEntity("", entityName, entityId, false, null, false);
        List<AttestationPolicy> attestationPolicies = getAttestationPolicies(entityName);
        JsonNode updatedNode = entityStateHelper.sendForAttestation(entityNode, propertyURI, notes, attestationPolicies);
        updateEntity(updatedNode, "");
    }

    public void callPluginActor() throws JsonProcessingException {
        registryService.callPluginActors("CowinActor", PluginRequestMessage.builder().sourceEntity("Student").sourceOSID("234").policyName("education").attestationOSID("432").build());
    }

    public void attest(String entityName, String entityId, String uuidPath, JsonNode attestReq) throws Exception {
        JsonNode entityNode = readEntity("", entityName, entityId, false, null, false);
        JsonNode updatedNode;
        if (attestReq.get("action").asText().equals(Action.GRANT_CLAIM.toString())) {
            updatedNode = entityStateHelper.grantClaim(entityNode, uuidPath, attestReq.get("notes").asText(), getAttestationPolicies(entityName));
            sendNotificationToOwners(updatedNode, CLAIM_GRANTED, String.format(CLAIM_STATUS_SUBJECT_TEMPLATE, CLAIM_GRANTED), String.format(CLAIM_STATUS_BODY_TEMPLATE, CLAIM_GRANTED));
        } else {
            updatedNode = entityStateHelper.rejectClaim(entityNode, uuidPath, attestReq.get("notes").asText(), getAttestationPolicies(entityName));
            sendNotificationToOwners(updatedNode, CLAIM_REJECTED, String.format(CLAIM_STATUS_SUBJECT_TEMPLATE, CLAIM_REJECTED), String.format(CLAIM_STATUS_BODY_TEMPLATE, CLAIM_REJECTED));
        }
        updateEntity(updatedNode, "");
    }

    public void updateState(PluginResponseMessage pluginResponseMessage) throws Exception {
        String attestationName = pluginResponseMessage.getPolicyName();
        String attestationOSID = pluginResponseMessage.getAttestationOSID();
        String sourceEntity = pluginResponseMessage.getSourceEntity();
        AttestationPolicy attestationPolicy = getAttestationPolicy(sourceEntity, attestationName);
        String userId = "";

        JsonNode root = readEntity(userId, sourceEntity, pluginResponseMessage.getSourceOSID(), false, null, false);
        ObjectNode metaData = JsonNodeFactory.instance.objectNode();
        JsonNode additionalData = pluginResponseMessage.getAdditionalData();
        Action action = Action.valueOf(pluginResponseMessage.getStatus());
        switch (action) {
            case GRANT_CLAIM:
                Object credentialTemplate = attestationPolicy.getCredentialTemplate();
                // checking size greater than 1, bcz empty template contains osid field
                if (credentialTemplate != null) {
                    JsonNode response = objectMapper.readTree(pluginResponseMessage.getResponse());
                    Object signedData = getSignedDoc(response, credentialTemplate);
                    metaData.put(
                            ATTESTED_DATA,
                            signedData.toString()
                    );
                } else {
                    metaData.put(
                            ATTESTED_DATA,
                            pluginResponseMessage.getResponse()
                    );
                }
                break;
            case SELF_ATTEST:
                String hashOfTheFile = pluginResponseMessage.getResponse();
                metaData.put(
                        ATTESTED_DATA,
                        hashOfTheFile
                );
                break;
            case RAISE_CLAIM:
                metaData.put(
                        CLAIM_ID,
                        additionalData.get(CLAIM_ID).asText("")
                );
        }
        String propertyURI = attestationName + "/" + attestationOSID;
        uploadAttestedFiles(pluginResponseMessage, metaData);
        JsonNode nodeToUpdate = entityStateHelper.manageState(attestationPolicy, root, propertyURI, action, metaData);
        updateEntity(nodeToUpdate, userId);
    }

    private void uploadAttestedFiles(PluginResponseMessage pluginResponseMessage, ObjectNode metaData) throws Exception {
        if (!CollectionUtils.isEmpty(pluginResponseMessage.getFiles())) {
            ArrayNode fileUris = JsonNodeFactory.instance.arrayNode();
            pluginResponseMessage.getFiles().forEach(file -> {
                String propertyURI = String.format("%s/%s/%s/documents/%s", pluginResponseMessage.getSourceEntity(),
                        pluginResponseMessage.getSourceOSID(), pluginResponseMessage.getPolicyName(), file.getFileName());
                try {
                    fileStorageService.save(new ByteArrayInputStream(file.getFile()), propertyURI);
                } catch (Exception e) {
                    logger.error("Failed persisting file", e);
                    e.printStackTrace();
                }
                fileUris.add(propertyURI);
            });
            JsonNode jsonNode = metaData.get(ATTESTED_DATA);
            ObjectNode attestedObjectNode = objectMapper.readValue(jsonNode.asText(), ObjectNode.class);
            attestedObjectNode.set("files", fileUris);
            metaData.put(ATTESTED_DATA, attestedObjectNode.toString());
        }
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
        JsonNode queryNode = inputJson.get(entityType);

        ArrayNode newEntityArrNode = objectMapper.createArrayNode();
        newEntityArrNode.add(entityType + auditSuffixSeparator + auditSuffix);
        ((ObjectNode) queryNode).set("entityType", newEntityArrNode);

        JsonNode resultNode = searchService.search(queryNode);

        ViewTemplate viewTemplate = viewTemplateManager.getViewTemplate(inputJson);
        if (viewTemplate != null) {
            ViewTransformer vTransformer = new ViewTransformer();
            resultNode = vTransformer.transform(viewTemplate, resultNode);
        }
        logger.debug("get audit log ends");

        return resultNode;

    }

    public boolean doesEntityContainOwnershipAttributes(@PathVariable String entityName) {
        if (definitionsManager.getDefinition(entityName) != null) {
            return definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getOwnershipAttributes().size() > 0;
        } else {
            return false;
        }
    }

    public String getUserId(HttpServletRequest request, String entityName) throws Exception {
        if (doesEntityContainOwnershipAttributes(entityName) || getManageRoles(entityName).size() > 0) {
            return fetchUserIdFromToken(request);
        } else {
            return dev.sunbirdrc.registry.Constants.USER_ANONYMOUS;
        }
    }

    private String fetchUserIdFromToken(HttpServletRequest request) throws Exception {
        KeycloakAuthenticationToken principal = (KeycloakAuthenticationToken) request.getUserPrincipal();
        if (principal != null) {
            return principal.getAccount().getPrincipal().getName();
        }
        throw new Exception("Forbidden");
    }

    public JsonNode getRequestedUserDetails(HttpServletRequest request, String entityName) throws Exception {
        if (isInternalRegistry(entityName)) {
            return getUserInfoFromRegistry(request, entityName);
        } else if (entityTypeHandler.isExternalRegistry(entityName)) {
            return getUserInfoFromKeyCloak(request, entityName);
        }
        throw new Exception(NOT_PART_OF_THE_SYSTEM_EXCEPTION);
    }

    private boolean isInternalRegistry(String entityName) {
        return definitionsManager.getAllKnownDefinitions().contains(entityName);
    }

    private JsonNode getUserInfoFromKeyCloak(HttpServletRequest request, String entityName) {
        Set<String> roles = getUserRolesFromRequest(request);
        JsonNode rolesNode = objectMapper.convertValue(roles, JsonNode.class);
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        // To maintain the consistency with searchEntity we are using ArrayNode
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(rolesNode);
        result.set(entityName, arrayNode);
        return result;
    }

    private JsonNode getUserInfoFromRegistry(HttpServletRequest request, String entityName) throws Exception {
        KeycloakAuthenticationToken principal = (KeycloakAuthenticationToken) request.getUserPrincipal();
        if (principal != null) {
            String userId = principal.getAccount().getPrincipal().getName();
            ObjectNode payload = JsonNodeFactory.instance.objectNode();
            payload.set("entityType", JsonNodeFactory.instance.arrayNode().add(entityName));
            ObjectNode filters = JsonNodeFactory.instance.objectNode();
            filters.set(OSSystemFields.osOwner.toString(), JsonNodeFactory.instance.objectNode().put("contains", userId));
            payload.set("filters", filters);

            watch.start("RegistryController.searchEntity");
            JsonNode result = searchEntity(payload);
            watch.stop("RegistryController.searchEntity");
            return result;
        }
        throw new Exception("Forbidden");
    }

    public void authorize(String entityName, String entityId, HttpServletRequest request) throws Exception {
        String userIdFromRequest = getUserId(request, entityName);
        JsonNode response = readEntity(userIdFromRequest, entityName, entityId, false, null, false);
        JsonNode entityFromDB = response.get(entityName);
        if (!isOwner(entityFromDB, userIdFromRequest)) {
            throw new Exception(UNAUTHORIZED_OPERATION_MESSAGE);
        }
    }

    public String getPropertyIdAfterSavingTheProperty(String entityName, String entityId, JsonNode requestBody, HttpServletRequest request) throws Exception {
        JsonNode resultNode = readEntity("", entityName, entityId, false, null, false)
                .get(entityName);
        String propertyURI = getPropertyURI(entityId, request);
        JsonNode jsonNode = resultNode.get(propertyURI);
        List<String> fieldsToRemove = getFieldsToRemove(entityName);
        if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            for (JsonNode next : arrayNode) {
                JsonNode existingProperty = next.deepCopy();
                JSONUtil.removeNodes(existingProperty, fieldsToRemove);

                JsonNode requestBodyWithoutSystemFields = requestBody.deepCopy();
                JSONUtil.removeNodes(requestBodyWithoutSystemFields, fieldsToRemove);

                if (existingProperty.equals(requestBodyWithoutSystemFields)) {
                    return next.get(uuidPropertyName).asText();
                }
            }
        }
        return "";
    }

    private String getPropertyURI(String entityId, HttpServletRequest request) {
        return request.getRequestURI().split(entityId + "/")[1];
    }

    @NotNull
    public List<String> getFieldsToRemove(String entityName) {
        List<String> fieldsToRemove = new ArrayList<>();
        fieldsToRemove.add(uuidPropertyName);
        List<String> systemFields = definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getSystemFields();
        fieldsToRemove.addAll(systemFields);
        return fieldsToRemove;
    }

    public ArrayNode fetchFromDBUsingEsResponse(String entity, ArrayNode esSearchResponse) throws Exception {
        ArrayNode result = objectMapper.createArrayNode();
        for (JsonNode value : esSearchResponse) {
            JsonNode dbResponse = readEntity("", entity, value.get(uuidPropertyName).asText(), false, null, false);
            result.add(dbResponse.get(entity));
        }
        return result;
    }

    public void authorizeInviteEntity(HttpServletRequest request, String entityName) throws Exception {
        List<String> inviteRoles = definitionsManager.getDefinition(entityName)
                .getOsSchemaConfiguration()
                .getInviteRoles();
        if (inviteRoles.contains(ROLE_ANONYMOUS)) {
            return;
        }
        Set<String> userRoles = getUserRolesFromRequest(request);
        authorizeUserRole(userRoles, inviteRoles);
    }

    public void authorizeDeleteEntity(HttpServletRequest request, String entityName,String entityId) throws Exception {
        List<String> deleteRoles = definitionsManager.getDefinition(entityName)
          .getOsSchemaConfiguration()
          .getDeleteRoles();
        if (deleteRoles.contains(ROLE_ANONYMOUS)) {
            return;
        }
        Set<String> userRoles = getUserRolesFromRequest(request);
        String userIdFromRequest = getUserId(request, entityName);
        JsonNode response = readEntity(userIdFromRequest, entityName, entityId, false, null, false);
        JsonNode entityFromDB = response.get(entityName);
        final boolean hasNoValidRole = !deleteRoles.isEmpty() && deleteRoles.stream().noneMatch(userRoles::contains);
        final boolean hasInValidOwnership = !isOwner(entityFromDB, userIdFromRequest);
        if(hasNoValidRole && hasInValidOwnership){
            throw new UnAuthorizedException(UNAUTHORIZED_OPERATION_MESSAGE);
        }
    }

    public String authorizeManageEntity(HttpServletRequest request, String entityName) throws Exception {

        List<String> managingRoles = getManageRoles(entityName);
        if (managingRoles.size() > 0) {
            Set<String> userRoles = getUserRolesFromRequest(request);
            authorizeUserRole(userRoles, managingRoles);
            return fetchUserIdFromToken(request);
        } else {
            return ROLE_ANONYMOUS;
        }
    }

    private List<String> getManageRoles(String entityName) {
        return definitionsManager.getDefinition(entityName)
                    .getOsSchemaConfiguration()
                    .getRoles();
    }

    private Set<String> getUserRolesFromRequest(HttpServletRequest request) {
        KeycloakAuthenticationToken userPrincipal = (KeycloakAuthenticationToken) request.getUserPrincipal();
        return userPrincipal!=null ? userPrincipal.getAccount().getRoles():Collections.emptySet();
    }

    public void authorizeUserRole(Set<String> userRoles, List<String> allowedRoles) throws Exception {
        if (!allowedRoles.isEmpty() && allowedRoles.stream().noneMatch(userRoles::contains)) {
            throw new UnAuthorizedException(UNAUTHORIZED_OPERATION_MESSAGE);
        }
    }

    public void authorizeAttestor(String entity, HttpServletRequest request) throws Exception {
        List<String> keyCloakEntities = getUserEntities(request);
        Set<String> allTheAttestorEntities = definitionsManager.getDefinition(entity)
                .getOsSchemaConfiguration()
                .getAllTheAttestorEntities();
        if (keyCloakEntities.stream().noneMatch(allTheAttestorEntities::contains)) {
            throw new Exception(UNAUTHORIZED_EXCEPTION_MESSAGE);
        }
    }

    public List<String> getUserEntities(HttpServletRequest request) {
        KeycloakAuthenticationToken principal = (KeycloakAuthenticationToken) request.getUserPrincipal();
        Object customAttributes = principal.getAccount()
                .getKeycloakSecurityContext()
                .getToken()
                .getOtherClaims()
                .get("entity");
        return (List<String>) customAttributes;
    }

    @Async
    public void invalidateAttestation(String entityName, String entityId) throws Exception {
        String userId = "osrc";
        JsonNode entity = readEntity(userId, entityName, entityId, false, null, false)
                .get(entityName);
        for (AttestationPolicy attestationPolicy : getAttestationPolicies(entityName)) {
            String policyName = attestationPolicy.getName();

            if (entity.has(policyName) && entity.get(policyName).isArray()) {
                ArrayNode attestations = (ArrayNode) entity.get(policyName);
                updateAttestation(entity, attestationPolicy, attestations);
            }
        }
        ObjectNode newRoot = JsonNodeFactory.instance.objectNode();
        newRoot.set(entityName, entity);
        updateEntity(newRoot, userId);
    }

    private void updateAttestation(JsonNode entity, AttestationPolicy attestationPolicy, ArrayNode attestations) {
        for (JsonNode attestation : attestations) {
            if (attestation.get(_osState.name()).asText().equals(States.PUBLISHED.name())) {
                ObjectNode propertiesOSID = attestation.get("propertiesOSID").deepCopy();
                JSONUtil.removeNode(propertiesOSID, uuidPropertyName);
                Map<String, List<String>> propertiesOSIDMapper = objectMapper.convertValue(propertiesOSID, Map.class);
                JsonNode propertyData = JSONUtil.extractPropertyDataFromEntity(entity, attestationPolicy.getAttestationProperties(), propertiesOSIDMapper);
                String proof = attestation.get(_osAttestedData.name()).asText();

                boolean isValid = false;
                try {
                    Object result = signatureService.verify(proof);

                } catch (SignatureException.UnreachableException | SignatureException.VerificationException e) {
                    e.printStackTrace();
                }
                if (!isValid) {
                    // update attestation status
                    ((ObjectNode) attestation).set(_osState.name(), JsonNodeFactory.instance.textNode(States.INVALID.name()));
                }

            }
        }
    }

    public Object getSignedDoc(JsonNode result, Object credentialTemplate) throws
            SignatureException.CreationException, SignatureException.UnreachableException {
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("data", result);
        requestBodyMap.put("credentialTemplate", credentialTemplate);
        return signatureService.sign(requestBodyMap);
    }

    // TODO: can be async?
    public void signDocument(String entityName, String entityId, String userId) throws Exception {
        if (!signatureEnabled) {
            return;
        }
        Object credentialTemplate = definitionsManager.getCredentialTemplate(entityName);
        if (credentialTemplate != null) {
            ObjectNode updatedNode = (ObjectNode) readEntity(userId, entityName, entityId, false, null, false)
                    .get(entityName);
            Object signedCredentials = getSignedDoc(updatedNode, credentialTemplate);
            updatedNode.set(OSSystemFields._osSignedData.name(), JsonNodeFactory.instance.textNode(signedCredentials.toString()));
            ObjectNode updatedNodeParent = JsonNodeFactory.instance.objectNode();
            updatedNodeParent.set(entityName, updatedNode);
            updateProperty(updatedNodeParent, userId);
        }
    }

    public void deleteEntity(String entityId, String userId) throws Exception {
        RecordIdentifier recordId = RecordIdentifier.parse(entityId);
        String shardId = dbConnectionInfoMgr.getShardId(recordId.getShardLabel());
        Shard shard = shardManager.activateShard(shardId);
        registryService.deleteEntityById(shard, userId, recordId.getUuid());
    }

    //TODO: add cache
    public List<AttestationPolicy> getAttestationPolicies(String entityName) {
        List<AttestationPolicy> dbAttestationPolicies = getAttestationsFromRegistry(entityName);
        List<AttestationPolicy> schemaAttestationPolicies = definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getAttestationPolicies();
        return ListUtils.union(dbAttestationPolicies, schemaAttestationPolicies);
    }

    private List<AttestationPolicy> getAttestationsFromRegistry(String entityName) {
        try {
            JsonNode searchRequest = objectMapper.readTree("{\n" +
                    "    \"entityType\": [\n" +
                    "        \"" + ATTESTATION_POLICY + "\"\n" +
                    "    ],\n" +
                    "    \"filters\": {\n" +
                    "       \"entity\": {\n" +
                    "           \"eq\": \"" + entityName + "\"\n" +
                    "       }\n" +
                    "    }\n" +
                    "}");
            JsonNode searchResponse = searchEntity(searchRequest);
            return convertJsonNodeToAttestationList(searchResponse);
        } catch (Exception e) {
            logger.error("Error fetching attestation policy", e);
            return Collections.emptyList();
        }
    }

    private List<AttestationPolicy> convertJsonNodeToAttestationList(JsonNode searchResponse) throws java.io.IOException {
        TypeReference<List<AttestationPolicy>> typeRef
                = new TypeReference<List<AttestationPolicy>>() {
        };
        ObjectReader reader = objectMapper.readerFor(typeRef);
        return reader.readValue(searchResponse.get(ATTESTATION_POLICY));
    }

    public boolean isAttestationPolicyNameAlreadyUsed(String entityName, String policyName) {
        List<AttestationPolicy> schemaAttestationPolicies = getAttestationPolicies(entityName);
        return schemaAttestationPolicies.stream().anyMatch(policy -> policy.getName().equals(policyName));
    }

    public AttestationPolicy getAttestationPolicy(String entityName, String policyName) {
        List<AttestationPolicy> attestationPolicies = getAttestationPolicies(entityName);
        return attestationPolicies.stream()
                .filter(policy -> policy.getName().equals(policyName))
                .findFirst()
                .orElseThrow(() -> new PolicyNotFoundException("Policy " + policyName + " is not found"));
    }

    public String createAttestationPolicy(AttestationPolicy attestationPolicy, String userId) throws Exception {
        ObjectNode entity = createJsonNodeForAttestationPolicy(attestationPolicy);
        return addEntityWithoutValidation(entity, userId, ATTESTATION_POLICY);
    }

    private ObjectNode createJsonNodeForAttestationPolicy(AttestationPolicy attestationPolicy) {
        JsonNode inputJson = objectMapper.valueToTree(attestationPolicy);
        ObjectNode entity = JsonNodeFactory.instance.objectNode();
        entity.set(ATTESTATION_POLICY, inputJson);
        return entity;
    }

    public List<AttestationPolicy> findAttestationPolicyByEntityAndCreatedBy(String entityName, String userId) throws Exception {
        JsonNode searchRequest = objectMapper.readTree("{\n" +
                "    \"entityType\": [\n" +
                "        \"" + "ATTESTATION_POLICY" + "\"\n" +
                "    ],\n" +
                "    \"filters\": {\n" +
                "       \"entity\": {\n" +
                "           \"eq\": \"" + entityName + "\"\n" +
                "       },\n" +
                "       \"createdBy\": {\n" +
                "           \"eq\": \"" + userId + "\"\n" +
                "       }\n" +
                "    }\n" +
                "}");
        searchEntity(searchRequest);
        return Collections.emptyList();
    }

    public String updateAttestationPolicy(String userId, AttestationPolicy attestationPolicy) throws Exception {
        JsonNode updateJson = createJsonNodeForAttestationPolicy(attestationPolicy);
        return updateProperty(updateJson, userId);
    }

    public Optional<AttestationPolicy> findAttestationPolicyById(String userId, String policyOSID) throws Exception {
        JsonNode jsonNode = readEntity(userId, ATTESTATION_POLICY, policyOSID, false, null, false)
                .get(ATTESTATION_POLICY);
        return Optional.of(objectMapper.treeToValue(jsonNode, AttestationPolicy.class));
    }


    public void deleteAttestationPolicy(AttestationPolicy attestationPolicy) throws Exception {
        deleteEntity(attestationPolicy.getOsid(), attestationPolicy.getCreatedBy());
    }
}
