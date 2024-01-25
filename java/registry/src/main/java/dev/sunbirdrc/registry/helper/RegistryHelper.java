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
import com.jayway.jsonpath.PathNotFoundException;
import dev.sunbirdrc.actors.factory.PluginRouter;
import dev.sunbirdrc.pojos.*;
import dev.sunbirdrc.pojos.attestation.Action;
import dev.sunbirdrc.pojos.attestation.States;
import dev.sunbirdrc.pojos.attestation.exception.PolicyNotFoundException;
import dev.sunbirdrc.registry.dao.VertexReader;
import dev.sunbirdrc.registry.authorization.pojos.UserToken;
import dev.sunbirdrc.registry.entities.*;
import dev.sunbirdrc.registry.exception.SignatureException;
import dev.sunbirdrc.registry.exception.UnAuthorizedException;
import dev.sunbirdrc.registry.exception.UnreachableException;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;
import dev.sunbirdrc.registry.middleware.service.ConditionResolverService;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.middleware.util.OSSystemFields;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.model.EventType;
import dev.sunbirdrc.registry.model.attestation.EntityPropertyURI;
import dev.sunbirdrc.registry.model.dto.AttestationRequest;
import dev.sunbirdrc.registry.service.*;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.sink.shard.ShardManager;
import dev.sunbirdrc.registry.util.*;
import dev.sunbirdrc.validators.IValidate;
import dev.sunbirdrc.views.FunctionDefinition;
import dev.sunbirdrc.views.FunctionExecutor;
import dev.sunbirdrc.views.ViewTemplate;
import dev.sunbirdrc.views.ViewTransformer;
import io.minio.errors.*;
import lombok.Setter;
import org.agrona.Strings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static dev.sunbirdrc.pojos.attestation.Action.GRANT_CLAIM;
import static dev.sunbirdrc.registry.Constants.*;
import static dev.sunbirdrc.registry.exception.ErrorMessages.*;
import static dev.sunbirdrc.registry.middleware.util.Constants.*;
import static dev.sunbirdrc.registry.middleware.util.OSSystemFields._osState;
import static dev.sunbirdrc.registry.middleware.util.OSSystemFields.osOwner;

/**
 * This is helper class, user-service calls this class in-order to access registry functionality
 */
@Component
@Setter
public class RegistryHelper {
    private static final String SIGNED_HASH = "signedHash";
    private static final String ATTESTED_DATA = "attestedData";
    private static final String CLAIM_ID = "claimId";
    private static final String ATTESTATION_RESPONSE = "attestationResponse";
    public static String ROLE_ANONYMOUS = "anonymous";

    private static final Logger logger = LoggerFactory.getLogger(RegistryHelper.class);

    @Value("${authentication.enabled:true}") boolean securityEnabled;
    @Value("${notification.service.enabled}") boolean notificationEnabled;
    @Value("${invite.required_validation_enabled}") boolean skipRequiredValidationForInvite = true;
    @Value("${invite.signature_enabled}") boolean skipSignatureForInvite = true;
    @Autowired(required = false)
    private NotificationHelper notificationHelper;
    @Autowired
    private ShardManager shardManager;

    @Autowired
    RegistryService registryService;

    @Autowired
    @Qualifier("async")
    RegistryService registryAsyncService;

    @Autowired
    IReadService readService;

    @Autowired
    IValidate validationService;

    @Autowired
    private ISearchService searchService;

    @Autowired
    private NativeSearchService nativeSearchService;

    @Autowired
    private ViewTemplateManager viewTemplateManager;

    @Autowired
    EntityStateHelper entityStateHelper;

    @Autowired
    private IDefinitionsManager definitionsManager;

    @Autowired
    private DBConnectionInfoMgr dbConnectionInfoMgr;

    @Value("${encryption.enabled}")
    private boolean encryptionEnabled;
    @Autowired(required = false)
    private DecryptionHelper decryptionHelper;

    @Autowired
    private SunbirdRCInstrumentation watch;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${filestorage.enabled}")
    private boolean fileStorageEnabled;
    @Autowired(required = false)
    private FileStorageService fileStorageService;

    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    @Value("${audit.frame.suffix}")
    public String auditSuffix;
    @Value("${event.enabled}")
    private boolean isEventsEnabled;
    @Value("${audit.frame.suffixSeparator}")
    public String auditSuffixSeparator;

    @Value("${conditionalAccess.internal}")
    private String internalFieldsProp;

    @Value("${conditionalAccess.private}")
    private String privateFieldsProp;

    @Value("${signature.enabled}")
    private boolean signatureEnabled;

    @Value("${workflow.enabled:true}")
    private boolean workflowEnabled;

    @Value("${attestationPolicy.search_enabled:false}")
    private boolean attestationPolicySearchEnabled;
    @Value("${view_template.decrypt_private_fields:false}")
    private boolean viewTemplateDecryptPrivateFields;

    @Value("${registry.hard_delete_enabled}")
    private boolean isHardDeleteEnabled;

    @Autowired
    private EntityTypeHandler entityTypeHandler;

    @Autowired(required = false)
    private SignatureService signatureService;

    @Autowired
    private ConditionResolverService conditionResolverService;

    private FunctionExecutor functionExecutor = new FunctionExecutor();

    @Autowired
    private AsyncRequest asyncRequest;

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
    public String addEntity(JsonNode inputJson, String userId) throws Exception {
        String entityId = addEntityHandler(inputJson, userId, false, false);
        if(notificationEnabled) notificationHelper.sendNotification(inputJson, CREATE);
        return entityId;
    }

    public String inviteEntity(JsonNode inputJson, String userId) throws Exception {
        String entityId = addEntityHandler(inputJson, userId, skipRequiredValidationForInvite, skipSignatureForInvite);
        if(notificationEnabled) notificationHelper.sendNotification(inputJson, INVITE);
        return entityId;
    }

    private String addEntityWithoutValidation(JsonNode inputJson, String userId, String entityName) throws Exception {
        return addEntity(inputJson, userId, entityName, true);
    }

    private String addEntityHandler(JsonNode inputJson, String userId, boolean skipRequiredValidation, boolean skipSignature) throws Exception {
        String entityType = inputJson.fields().next().getKey();
        validationService.validate(entityType, objectMapper.writeValueAsString(inputJson), skipRequiredValidation);
        String entityName = inputJson.fields().next().getKey();
        if (workflowEnabled) {
            List<AttestationPolicy> attestationPolicies = getAttestationPolicies(entityName);
            inputJson = entityStateHelper.applyWorkflowTransitions(JSONUtil.convertStringJsonNode("{}"), inputJson, attestationPolicies);
        }
        if (!StringUtils.isEmpty(userId)) {
            ArrayNode jsonNode = (ArrayNode) inputJson.get(entityName).get(osOwner.toString());
            if (jsonNode == null) {
                jsonNode = new ObjectMapper().createArrayNode();
                ((ObjectNode) inputJson.get(entityName)).set(osOwner.toString(), jsonNode);
            }
            jsonNode.add(userId);
        }
        return addEntity(inputJson, userId, entityType, skipSignature);
    }

    private String addEntity(JsonNode inputJson, String userId, String entityType, boolean skipSignature) throws Exception {
        RecordIdentifier recordId;
        try {
            logger.info("Add api: entity type: {} and shard propery: {}", entityType, shardManager.getShardProperty());
            Shard shard = shardManager.getShard(inputJson.get(entityType).get(shardManager.getShardProperty()));
            watch.start("RegistryController.addToExistingEntity");
            String resultId;
            if (asyncRequest.isEnabled()) {
                resultId = registryAsyncService.addEntity(shard, userId, inputJson, skipSignature);
                recordId = new RecordIdentifier(null, resultId);
            } else {
                resultId = registryService.addEntity(shard, userId, inputJson, skipSignature);
                recordId = new RecordIdentifier(shard.getShardLabel(), resultId);
            }
            watch.stop("RegistryController.addToExistingEntity");
            logger.info("AddEntity,{}", recordId.toString());
        } catch (Exception e) {
            logger.error("Exception in controller while adding entity !, {}", ExceptionUtils.getStackTrace(e));
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

    public JsonNode readEntity(String userId, String entityType, String label, boolean includeSignatures,
                               ViewTemplate viewTemplate, boolean requireLDResponse) throws Exception {
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
            if (viewTemplateDecryptPrivateFields) {
                if (!encryptionEnabled) {
                    throw new UnreachableException("Encryption should be enabled to decrypt private fields");
                }
                resultNode = includePrivateFields ? decryptionHelper.getDecryptedJson(resultNode) : resultNode;
            }
            resultNode = vTransformer.transform(viewTemplate, resultNode);
        } else if (encryptionEnabled) {
            resultNode = decryptionHelper.getDecryptedJson(resultNode);
        }
        logger.debug("readEntity ends");
        if(isEventsEnabled) {
            registryService.maskAndEmitEvent(resultNode.get(entityType), entityType, EventType.READ, userId, label);
        }
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
        return searchEntity(inputJson, searchService);
    }

    private JsonNode searchEntity(JsonNode inputJson, ISearchService service) throws Exception {
        logger.debug("searchEntity starts");
        JsonNode resultNode = service.search(inputJson);
        ViewTemplate viewTemplate = viewTemplateManager.getViewTemplate(inputJson);
        if (viewTemplate != null) {
            ViewTransformer vTransformer = new ViewTransformer();
            resultNode = vTransformer.transform(viewTemplate, resultNode);
        }
        // Search is tricky to support LD. Needs a revisit here.
        logger.debug("searchEntity ends");
        return resultNode;
    }

    /**
     * Updates the input entity, external api's can use this method to update the entity
     *
     * @param inputJson
     * @param userId
     * @throws Exception
     */
    private void updateEntity(JsonNode inputJson, String userId) throws Exception {
        logger.debug("updateEntity starts");
        String entityType = inputJson.fields().next().getKey();
        String jsonString = objectMapper.writeValueAsString(inputJson);
        validationService.validate(entityType, jsonString, true);
        Shard shard = shardManager.getShard(inputJson.get(entityType).get(shardManager.getShardProperty()));
        String label = inputJson.get(entityType).get(dbConnectionInfoMgr.getUuidPropertyName()).asText();
        RecordIdentifier recordId = RecordIdentifier.parse(label);
        logger.info("Update Api: shard id: " + recordId.getShardLabel() + " for uuid: " + recordId.getUuid());
        registryService.updateEntity(shard, userId, recordId.getUuid(), jsonString, false);
        logger.debug("updateEntity ends");
    }

    public String updateProperty(JsonNode inputJson, String userId) throws Exception {
        logger.debug("updateEntity starts");
        String entityType = inputJson.fields().next().getKey();
        String jsonString = objectMapper.writeValueAsString(inputJson);
        Shard shard = shardManager.getShard(inputJson.get(entityType).get(shardManager.getShardProperty()));
        String label = inputJson.get(entityType).get(dbConnectionInfoMgr.getUuidPropertyName()).asText();
        RecordIdentifier recordId = RecordIdentifier.parse(label);
        logger.info("Update Api: shard id: " + recordId.getShardLabel() + " for uuid: " + recordId.getUuid());
        registryService.updateEntity(shard, userId, recordId.getUuid(), jsonString, false);
        if(notificationEnabled) notificationHelper.sendNotification(inputJson, UPDATE);
        return "SUCCESS";
    }

    public void updateEntityAndState(JsonNode existingNode, JsonNode updatedNode, String userId) throws Exception {
        if (workflowEnabled) {
            String entityName = updatedNode.fields().next().getKey();
            List<AttestationPolicy> attestationPolicies = getAttestationPolicies(entityName);
            updatedNode = entityStateHelper.applyWorkflowTransitions(existingNode, updatedNode, attestationPolicies);
        }
        updateEntity(updatedNode, userId);
        if(notificationEnabled) notificationHelper.sendNotification(updatedNode, UPDATE);
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

    public String triggerAttestation(AttestationRequest attestationRequest, AttestationPolicy attestationPolicy) throws Exception {
        addAttestationProperty(attestationRequest);
        //TODO: remove reading the entity after update
        String attestationOSID = getAttestationOSID(attestationRequest);

        String condition = "";
        if (attestationPolicy.isInternal()) {
            // Resolve condition for REQUESTER
            condition = conditionResolverService.resolve(attestationRequest.getPropertyData(), REQUESTER,
                    attestationPolicy.getConditions(), Collections.emptyList());
        }

        updateGetFileUrl(attestationRequest.getAdditionalInput());

        String propertyData = null;
        if (attestationRequest.getPropertyData() != null) {
            propertyData = attestationRequest.getPropertyData().toString();
        }

        PluginRequestMessage message = PluginRequestMessageCreator.create(
                propertyData, condition, attestationOSID, attestationRequest.getEntityName(),
                attestationRequest.getEmailId(), attestationRequest.getEntityId(), attestationRequest.getAdditionalInput(),
                Action.RAISE_CLAIM.name(), attestationPolicy.getName(), attestationPolicy.getAttestorPlugin(),
                attestationPolicy.getAttestorEntity(), attestationPolicy.getAttestorSignin(),
                attestationRequest.getPropertiesOSID(), attestationRequest.getEmailId());

        PluginRouter.route(message);
        return attestationOSID;
    }


    private void updateGetFileUrl(JsonNode additionalInput) throws UnreachableException {
        if(additionalInput!= null && additionalInput.has(FILE_URL)) {
            if (!fileStorageEnabled) {
                throw new UnreachableException("File Storage Service is not enabled");
            }
            ArrayNode fileUrls = (ArrayNode)(additionalInput.get(FILE_URL));
            ArrayNode signedUrls = JsonNodeFactory.instance.arrayNode();
            for (JsonNode fileNode : fileUrls) {
                String fileUrl = fileNode.asText();
                try {
                    String sharableUrl = fileStorageService.getSignedUrl(fileUrl);
                    signedUrls.add(sharableUrl);
                } catch (ServerException | InternalException | XmlParserException | InvalidResponseException
                         | InvalidKeyException | NoSuchAlgorithmException | IOException
                         | ErrorResponseException | InsufficientDataException e) {
                    logger.error("Fetching signed file url failed: {}", ExceptionUtils.getStackTrace(e));
                }
            }
            ((ObjectNode)additionalInput).replace(FILE_URL, signedUrls);
        }
    }

    private String getAttestationOSID(AttestationRequest attestationRequest) throws Exception {
        JsonNode resultNode = readEntity("", attestationRequest.getEntityName(), attestationRequest.getEntityId(),
                false, null, false)
                .get(attestationRequest.getEntityName())
                .get(attestationRequest.getName());
        List<String> fieldsToRemove = getFieldsToRemove(attestationRequest.getEntityName());
        return JSONUtil.getOSIDFromArrNode(resultNode, JSONUtil.convertObjectJsonNode(attestationRequest), fieldsToRemove);
    }

    private void addAttestationProperty(AttestationRequest attestationRequest) throws Exception {
        JsonNode existingEntityNode = readEntity(attestationRequest.getUserId(), attestationRequest.getEntityName(),
                attestationRequest.getEntityId(), false, null, false);
        JsonNode nodeToUpdate = existingEntityNode.deepCopy();
        JsonNode parentNode = nodeToUpdate.get(attestationRequest.getEntityName());
        JsonNode propertyNode = parentNode.get(attestationRequest.getName());
        ObjectNode attestationJsonNode = (ObjectNode) JSONUtil.convertObjectJsonNode(attestationRequest);
        if (attestationRequest.getPropertyData() != null) {
            attestationJsonNode.set("propertyData", JsonNodeFactory.instance.textNode(attestationRequest.getPropertyData().toString()));
        }
        createOrUpdateProperty(attestationRequest.getEntityName(), attestationJsonNode, nodeToUpdate, attestationRequest.getName(), (ObjectNode) parentNode, propertyNode);
        updateEntityAndState(existingEntityNode, nodeToUpdate, attestationRequest.getUserId());
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

    public void updateEntityProperty(String entityName, String entityId, JsonNode inputJson, HttpServletRequest request, JsonNode existingNode) throws Exception {
        String propertyURI = getPropertyURI(entityId, request);
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
                    if (!signatureEnabled) {
                        throw new UnreachableException("Signature service not enabled!");
                    }
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
        triggerNextFLowIfExists(pluginResponseMessage, sourceEntity, attestationPolicy, action, nodeToUpdate, userId);
    }

    private void triggerNextFLowIfExists(PluginResponseMessage pluginResponseMessage, String sourceEntity,
                                         AttestationPolicy attestationPolicy, Action action, JsonNode sourceNode, String userId) throws Exception {
        if (action == GRANT_CLAIM && !StringUtils.isEmpty(attestationPolicy.getOnComplete())) {
            if (attestationPolicy.getCompletionType() == FlowType.ATTESTATION) {
                try {
                    AttestationPolicy nextAttestationPolicy = getAttestationPolicy(sourceEntity, attestationPolicy.getCompletionValue());
                    AttestationRequest attestationRequest = AttestationRequest.builder().entityName(pluginResponseMessage.getSourceEntity())
                            .entityId(pluginResponseMessage.getSourceOSID()).name(attestationPolicy.getCompletionValue())
                            .additionalInput(pluginResponseMessage.getAdditionalData()).emailId(pluginResponseMessage.getEmailId())
                            .userId(pluginResponseMessage.getUserId()).propertiesOSID(pluginResponseMessage.getPropertiesOSID())
                            .propertyData(JSONUtil.convertStringJsonNode(pluginResponseMessage.getResponse())).build();
                    triggerAttestation(attestationRequest, nextAttestationPolicy);
                } catch (PolicyNotFoundException e) {
                    logger.error("Next level attestation policy not found: {}", ExceptionUtils.getStackTrace(e));
                }
            } else if (attestationPolicy.getCompletionType() == FlowType.FUNCTION) {
                FunctionDefinition functionDefinition = definitionsManager.getDefinition(sourceEntity).getOsSchemaConfiguration()
                        .getFunctionDefinition(attestationPolicy.getCompletionFunctionName());
                if (functionDefinition != null ) {
                    try {
                        JsonNode inputJsonNode = generateInputForFunctionExecutor(sourceEntity, sourceNode.deepCopy(), pluginResponseMessage);
                        JsonNode executedJsonNode = functionExecutor.execute(attestationPolicy.getCompletionValue(), functionDefinition, inputJsonNode);
                        ObjectNode updatedNode = convertToSourceNode(sourceEntity, (ObjectNode) executedJsonNode);
                        if (JSONUtil.diffJsonNode(sourceNode, updatedNode).size() > 0) {
                            updateEntity(updatedNode, userId);
                        }
                    } catch (JsonProcessingException e) {
                        logger.error("Exception while executing function definition: {} {}, {}", attestationPolicy.getOnComplete(), functionDefinition, ExceptionUtils.getStackTrace(e));
                        throw e;
                    }
                } else {
                    logger.error("Invalid function name specified for onComplete: {}", attestationPolicy.getOnComplete());
                }
            } else {
                logger.error("Invalid on complete config {}", attestationPolicy.getOnComplete());
            }
        }
    }

    @NotNull
    private ObjectNode convertToSourceNode(String sourceEntity, ObjectNode executedJsonNode) {
        ObjectNode updatedNode = JsonNodeFactory.instance.objectNode();
        executedJsonNode.remove(ATTESTATION_RESPONSE);
        updatedNode.set(sourceEntity, executedJsonNode);
        return updatedNode;
    }

    private JsonNode generateInputForFunctionExecutor(String sourceEntity, JsonNode sourceNode, PluginResponseMessage pluginResponseMessage) throws IOException {
        ObjectNode jsonNode = (ObjectNode) sourceNode.get(sourceEntity);
        jsonNode.set(ATTESTATION_RESPONSE, JSONUtil.convertObjectJsonNode(pluginResponseMessage));
        return jsonNode;
    }

    private void uploadAttestedFiles(PluginResponseMessage pluginResponseMessage, ObjectNode metaData) throws Exception {
        if (!CollectionUtils.isEmpty(pluginResponseMessage.getFiles())) {
            if (!fileStorageEnabled) {
                throw new UnreachableException("File Storage Service is not enabled");
            }
            ArrayNode fileUris = JsonNodeFactory.instance.arrayNode();
            pluginResponseMessage.getFiles().forEach(file -> {
                String propertyURI = String.format("%s/%s/%s/documents/%s", pluginResponseMessage.getSourceEntity(),
                        pluginResponseMessage.getSourceOSID(), pluginResponseMessage.getPolicyName(), file.getFileName());
                try {
                    fileStorageService.save(new ByteArrayInputStream(file.getFile()), propertyURI);
                } catch (Exception e) {
                    logger.error("Failed persisting file: {}", ExceptionUtils.getStackTrace(e));
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
        ((ObjectNode) queryNode).set(ENTITY_TYPE, newEntityArrNode);

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

    public String getUserId(String entityName) throws Exception {
        if (doesEntityOperationRequireAuthorization(entityName)) {
            return fetchUserIdFromToken();
        } else {
            return dev.sunbirdrc.registry.Constants.USER_ANONYMOUS;
        }
    }

    private String fetchUserIdFromToken() throws Exception {
        if(!securityEnabled){
            return DEFAULT_USER;
        }
        return getPrincipalUserId();
    }

    public String getPrincipalUserId() throws Exception {
        UserToken userToken = (UserToken) SecurityContextHolder.getContext().getAuthentication();
        if (userToken != null) {
            return userToken.getUserId();
        }
        throw new Exception("Forbidden");
    }

    public String fetchEmailIdFromToken(HttpServletRequest request, String entityName) throws Exception {
        if (doesEntityContainOwnershipAttributes(entityName) || getManageRoles(entityName).size() > 0) {
            UserToken principal = (UserToken) request.getUserPrincipal();
            if (principal != null) {
                try{
                    return principal.getEmail();
                }catch (Exception exception){
                    return principal.getName();
                }
            }
        }
        return USER_ANONYMOUS;
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
        String userId = getUserId(entityName);
        if (userId != null) {
            ObjectNode payload = getSearchByOwnerQuery(entityName, userId);

            watch.start("RegistryController.searchEntity");
            JsonNode result = searchEntity(payload);
            watch.stop("RegistryController.searchEntity");
            if(result != null && result.get(entityName) != null && !result.get(entityName).isEmpty()) {
                String uuid = result.get(entityName).get(0).get(uuidPropertyName).asText();
                JsonNode user = readEntity(userId, entityName, uuid, true, null, false);
                ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                arrayNode.add(user.get(entityName));
                ((ObjectNode) result).set(entityName, arrayNode);
            }
            return result;
        }
        throw new Exception("Forbidden");
    }

    @NotNull
    private ObjectNode getSearchByOwnerQuery(String entityName, String userId) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.set(ENTITY_TYPE, JsonNodeFactory.instance.arrayNode().add(entityName));
        ObjectNode filters = JsonNodeFactory.instance.objectNode();
        filters.set(OSSystemFields.osOwner.toString(), JsonNodeFactory.instance.objectNode().put("contains", userId));
        payload.set(FILTERS, filters);
        return payload;
    }

    public String authorize(String entityName, String entityId, HttpServletRequest request) throws Exception {
        String userIdFromRequest = getUserId(entityName);
        if (getManageRoles(entityName).size() > 0) {
            try {
                return authorizeManageEntity(request, entityName);
            } catch (Exception e) {
                logger.error("Exception while authorizing roles: {}", ExceptionUtils.getStackTrace(e));
            }
        }
        JsonNode response = readEntity(userIdFromRequest, entityName, entityId, false, null, false);
        JsonNode entityFromDB = response.get(entityName);
        if (doesEntityContainOwnershipAttributes(entityName) && !isOwner(entityFromDB, userIdFromRequest)) {
            throw new Exception(UNAUTHORIZED_OPERATION_MESSAGE);
        }
        return userIdFromRequest;
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

    public String getPropertyURI(String entityId, HttpServletRequest request) {
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
    public JsonNode searchEntitiesByUserId(String entity, String userId, String viewTemplateId) throws Exception {
        ObjectNode searchByOwnerQuery = getSearchByOwnerQuery(entity, userId);
        if (!Strings.isEmpty(viewTemplateId)) {
            searchByOwnerQuery.put(VIEW_TEMPLATE_ID, viewTemplateId);
        }
        return searchEntity(searchByOwnerQuery, nativeSearchService);
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

    public String authorizeDeleteEntity(HttpServletRequest request, String entityName, String entityId) throws Exception {
        List<String> deleteRoles = getManageRoles(entityName);
        if (deleteRoles.contains(ROLE_ANONYMOUS)) {
            return entityName;
        }
        Set<String> userRoles = getUserRolesFromRequest(request);
        String userIdFromRequest = getUserId(entityName);
        JsonNode response = readEntity(userIdFromRequest, entityName, entityId, false, null, false);
        JsonNode entityFromDB = response.get(entityName);
        final boolean hasNoValidRole = !deleteRoles.isEmpty() && deleteRoles.stream().noneMatch(userRoles::contains);
        final boolean hasInValidOwnership = !isOwner(entityFromDB, userIdFromRequest);
        if(hasNoValidRole || hasInValidOwnership){
            throw new UnAuthorizedException(UNAUTHORIZED_OPERATION_MESSAGE);
        }
        return userIdFromRequest;
    }

    public String authorizeManageEntity(HttpServletRequest request, String entityName) throws Exception {

        List<String> managingRoles = getManageRoles(entityName);
        if (managingRoles.size() > 0) {
            if (!securityEnabled || managingRoles.contains(ROLE_ANONYMOUS)) {
                return ROLE_ANONYMOUS;
            }
            Set<String> userRoles = getUserRolesFromRequest(request);
            authorizeUserRole(userRoles, managingRoles);
            return fetchUserIdFromToken();
        } else {
            return ROLE_ANONYMOUS;
        }
    }

    private List<String> getManageRoles(String entityName) {
        if (definitionsManager.getDefinition(entityName) != null) {
            return definitionsManager.getDefinition(entityName)
                    .getOsSchemaConfiguration()
                    .getRoles();
        } else {
            return Collections.emptyList();
        }

    }

    private Set<String> getUserRolesFromRequest(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = new HashSet<>();
        if (authentication != null) {
            authentication.getAuthorities().forEach(authority -> roles.add(authority.getAuthority()));
        }
        return roles;
    }

    private void authorizeUserRole(Set<String> userRoles, List<String> allowedRoles) throws Exception {
        if (!allowedRoles.isEmpty() && allowedRoles.stream().noneMatch(userRoles::contains)) {
            throw new UnAuthorizedException(UNAUTHORIZED_OPERATION_MESSAGE);
        }
    }

    public void authorizeAttestor(String entity, HttpServletRequest request) throws Exception {
        List<String> userEntities = getUserEntities(request);
        Set<String> allTheAttestorEntities = definitionsManager.getDefinition(entity)
                .getOsSchemaConfiguration()
                .getAllTheAttestorEntities();
        if (userEntities.stream().noneMatch(allTheAttestorEntities::contains)) {
            throw new Exception(UNAUTHORIZED_EXCEPTION_MESSAGE);
        }
    }

    public List<String> getUserEntities(HttpServletRequest request) {
        UserToken principal = (UserToken) request.getUserPrincipal();
        return principal.getEntities();
    }

    @Async
    public void invalidateAttestation(String entityName, String entityId, String userId, @Nullable String propertyToUpdate) throws Exception {
        JsonNode entity = null;
        for (AttestationPolicy attestationPolicy : getAttestationPolicies(entityName)) {
            String policyName = attestationPolicy.getName();
            if (entity == null) {
                entity = readEntity(userId, entityName, entityId, false, null, false)
                        .get(entityName);
            }
            if (entity.has(policyName) && entity.get(policyName).isArray()) {
                ArrayNode attestations = (ArrayNode) entity.get(policyName);
                updateAttestation(attestations, propertyToUpdate);
            }
        }
        if (entity != null) {
            ObjectNode newRoot = JsonNodeFactory.instance.objectNode();
            newRoot.set(entityName, entity);
            updateEntity(newRoot, userId);
        }
    }

    public String getPropertyToUpdate(HttpServletRequest request, String entityId){
        String propertyURI = getPropertyURI(entityId, request);
        return propertyURI.split("/")[0];
    }
    private void updateAttestation(ArrayNode attestations,String propertyToUpdate) {
        for (JsonNode attestation : attestations) {
            if (attestation.get(_osState.name()).asText().equals(States.PUBLISHED.name())
              && !attestation.get("name").asText().equals(propertyToUpdate)
            ){
                ObjectNode propertiesOSID = attestation.get("propertiesOSID").deepCopy();
                JSONUtil.removeNode(propertiesOSID, uuidPropertyName);
                ((ObjectNode) attestation).set(_osState.name(), JsonNodeFactory.instance.textNode(States.INVALID.name()));
            }
        }
    }

    public Object getSignedDoc(JsonNode result, Object credentialTemplate) throws
            SignatureException.CreationException, SignatureException.UnreachableException {
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("data", result);
        requestBodyMap.put(CREDENTIAL_TEMPLATE, credentialTemplate);
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

    public Vertex deleteEntity(String entityName, String entityId, String userId) throws Exception {
        RecordIdentifier recordId = RecordIdentifier.parse(entityId);
        String shardId = dbConnectionInfoMgr.getShardId(recordId.getShardLabel());
        Shard shard = shardManager.activateShard(shardId);
        ReadConfigurator configurator = ReadConfiguratorFactory.getOne(false);
        JsonNode deletedNode = null;
        if(isHardDeleteEnabled) {
            deletedNode = readEntity(userId, entityName, entityId, false, null, false);
        }
        Vertex vertex = registryService.deleteEntityById(shard, entityName, userId, recordId.getUuid());
        if (!isHardDeleteEnabled) {
            VertexReader vertexReader = new VertexReader(shard.getDatabaseProvider(), vertex.graph(), configurator, uuidPropertyName, definitionsManager);
            deletedNode = JsonNodeFactory.instance.objectNode().set(entityName, vertexReader.constructObject(vertex));
        }
        if(notificationEnabled) notificationHelper.sendNotification(deletedNode, DELETE);
        return vertex;
    }

    public JsonNode revokeAnEntity (String entityName, String entityId, String userId, JsonNode currentJsonNode) throws Exception {
        RecordIdentifier recordId = RecordIdentifier.parse(entityId);
        String shardId = dbConnectionInfoMgr.getShardId(recordId.getShardLabel());
        Shard shard = shardManager.activateShard(shardId);
        ((ObjectNode) currentJsonNode).put(OSSystemFields._osSignedData.name(), "");
        ObjectNode newRootNode = objectMapper.createObjectNode();
        newRootNode.set(entityName, JSONUtil.convertObjectJsonNode(currentJsonNode));
        String jsonString = objectMapper.writeValueAsString(newRootNode);
        registryService.updateEntity(shard, userId, recordId.getUuid(),jsonString, true);
        return currentJsonNode;
    }

    //TODO: add cache
    public List<AttestationPolicy> getAttestationPolicies(String entityName) {
        List<AttestationPolicy> dbAttestationPolicies = getAttestationsFromRegistry(entityName);
        List<AttestationPolicy> schemaAttestationPolicies = definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getAttestationPolicies();
        return ListUtils.union(dbAttestationPolicies, schemaAttestationPolicies);
    }

    private List<AttestationPolicy> getAttestationsFromRegistry(String entityName) {
        if (attestationPolicySearchEnabled) {
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
                logger.error("Error fetching attestation policy: {}", ExceptionUtils.getStackTrace(e));
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    private List<AttestationPolicy> convertJsonNodeToAttestationList(JsonNode searchResponse) throws java.io.IOException {
        TypeReference<List<AttestationPolicy>> typeRef
                = new TypeReference<List<AttestationPolicy>>() {
        };
        ObjectReader reader = objectMapper.readerFor(typeRef);
        if (searchResponse.size() == 0) {
            return Collections.emptyList();
        }
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

    public String addRevokedCredential(JsonNode credential, String userId) throws Exception {
        ObjectNode entity = JsonNodeFactory.instance.objectNode();
        entity.set(ATTESTATION_POLICY, credential);
        return addEntityWithoutValidation(entity, userId, REVOKED_CREDENTIAL);
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


    public void deleteAttestationPolicy(String entityName, AttestationPolicy attestationPolicy) throws Exception {
        deleteEntity(entityName, attestationPolicy.getOsid(), attestationPolicy.getCreatedBy());
    }

    public boolean doesEntityOperationRequireAuthorization(String entity) {
        return securityEnabled && !getManageRoles(entity).contains("anonymous") && (doesEntityContainOwnershipAttributes(entity) || getManageRoles(entity).size() > 0);
    }

    boolean hasAttestationPropertiesChanged(JsonNode updatedNode, JsonNode existingNode, AttestationPolicy attestationPolicy, String entityName) {
        boolean result = false;
        List<String> paths = new ArrayList<>(attestationPolicy == null ? CollectionUtils.emptyCollection() : attestationPolicy.getAttestationProperties().values());
        for(String path : paths) {
            JsonNode extractedExistingAttestationNode = null;
            JsonNode extractedUpdatedAttestationNode = null;
            try {
                extractedExistingAttestationNode = JSONUtil.extractPropertyDataFromEntity(existingNode.get(entityName), attestationPolicy.getAttestationProperties(), null);
                extractedUpdatedAttestationNode = JSONUtil.extractPropertyDataFromEntity(updatedNode, attestationPolicy.getAttestationProperties(), null);
                if(!StringUtils.isEmpty(path) && !(extractedExistingAttestationNode.toString())
                        .equals(extractedUpdatedAttestationNode.toString())) {
                    result = true;
                }
            } catch (PathNotFoundException e) {
                result = true;
            }

        }
        return result;
    }

    public void autoRaiseClaim(String entityName, String entityId, String userId, JsonNode existingNode, JsonNode newRootNode, String emailId) throws Exception {
        if (workflowEnabled) {
            List<AttestationPolicy> attestationPolicies = getAttestationPolicies(entityName);
            for (AttestationPolicy attestationPolicy : attestationPolicies) {
                if (attestationPolicy.getType() != null && attestationPolicy.getType().equals(AttestationType.AUTOMATED)) {
                    JsonNode updatedNode = readEntity(newRootNode, entityId).get(entityName);
                    if (existingNode == null || hasAttestationPropertiesChanged(updatedNode, existingNode, attestationPolicy, entityName)) {
                        AttestationRequest attestationRequest = new AttestationRequest();
                        attestationRequest.setEntityId(entityId);
                        attestationRequest.setName(attestationPolicy.getName());
                        attestationRequest.setEntityName(entityName);
                        JsonNode node = JSONUtil.extractPropertyDataFromEntity(updatedNode, attestationPolicy.getAttestationProperties(), new HashMap<>());
                        attestationRequest.setPropertyData(node);
                        attestationRequest.setUserId(userId);
                        attestationRequest.setEmailId(emailId);
                        triggerAttestation(attestationRequest, attestationPolicy);
                    }
                }
            }
        }
    }

    public void revokeExistingCredentials(String entity, String entityId, String userId, String signedData) throws Exception {
        if (!StringUtils.isEmpty(signedData)) {
            RevokedCredential revokedCredential = RevokedCredential.builder().entity(entity).entityId(entityId)
                    .signedData(signedData).signedHash(generateHash(signedData)).userId(userId).build();
            ObjectNode newRootNode = objectMapper.createObjectNode();
            newRootNode.set(REVOKED_CREDENTIAL, JSONUtil.convertObjectJsonNode(revokedCredential));
            String revokedId = addEntity(newRootNode, userId, REVOKED_CREDENTIAL, false);
            logger.info("Added deleted credential to revoked list: {}", revokedId);

        }
    }

    private String generateHash(String signedData) {
        return DigestUtils.md5DigestAsHex(signedData.getBytes()).toUpperCase();
    }

    public boolean checkIfCredentialIsRevoked(String signedData) throws Exception {
        ObjectNode searchNode = JsonNodeFactory.instance.objectNode();
        searchNode.set(ENTITY_TYPE, JsonNodeFactory.instance.arrayNode().add(REVOKED_CREDENTIAL));
        searchNode.set(FILTERS,
                JsonNodeFactory.instance.objectNode().set(SIGNED_HASH,
                        JsonNodeFactory.instance.objectNode().put("eq", generateHash(signedData))));
        JsonNode searchResponse = searchEntity(searchNode);
        return searchResponse.get(REVOKED_CREDENTIAL) != null && searchResponse.get(REVOKED_CREDENTIAL).size() > 0;
    }

    public static ResponseEntity<Object> ServiceNotEnabledResponse(String message, Response response, ResponseParams responseParams) {
        responseParams.setErrmsg(message + " not enabled!");
        responseParams.setStatus(Response.Status.UNSUCCESSFUL);
        if (response != null) {
            response.setResponseCode("SERVICE_UNAVAILABLE");
        } else {
            response = new Response(Response.API_ID.GET, "SERVICE_UNAVAILABLE", responseParams);
        }
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
