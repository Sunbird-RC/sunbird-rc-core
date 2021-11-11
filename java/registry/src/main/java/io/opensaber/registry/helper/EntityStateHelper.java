package io.opensaber.registry.helper;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.opensaber.pojos.OwnershipsAttributes;
import io.opensaber.pojos.attestation.Action;
import io.opensaber.pojos.attestation.AttestationPolicy;
import io.opensaber.pojos.dto.ClaimDTO;
import io.opensaber.registry.middleware.service.ConditionResolverService;
import io.opensaber.registry.middleware.util.EntityUtil;
import io.opensaber.registry.model.attestation.AttestationPath;
import io.opensaber.registry.model.attestation.EntityPropertyURI;
import io.opensaber.registry.util.ClaimRequestClient;
import io.opensaber.registry.util.DefinitionsManager;
import io.opensaber.registry.util.RecordIdentifier;
import io.opensaber.workflow.RuleEngineService;
import io.opensaber.workflow.StateContext;
import net.minidev.json.JSONArray;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotEmpty;
import java.util.*;

import static io.opensaber.registry.middleware.util.Constants.*;

@Component
public class EntityStateHelper {

    private static final Logger logger = LoggerFactory.getLogger(EntityStateHelper.class);


    @Value("${database.uuidPropertyName}")
    private String uuidPropertyName;

    private final DefinitionsManager definitionsManager;

    private final RuleEngineService ruleEngineService;

    private final ConditionResolverService conditionResolverService;

    private final ClaimRequestClient claimRequestClient;

    @Autowired
    public EntityStateHelper(DefinitionsManager definitionsManager, RuleEngineService ruleEngineService, ConditionResolverService conditionResolverService, ClaimRequestClient claimRequestClient) {
        this.definitionsManager = definitionsManager;
        this.ruleEngineService = ruleEngineService;
        this.conditionResolverService = conditionResolverService;
        this.claimRequestClient = claimRequestClient;
    }

    void applyWorkflowTransitions(JsonNode existing, JsonNode updated) {
        String entityName = updated.fields().next().getKey();
        JsonNode modified = updated.get(entityName);
        logger.info("Detecting state changes by comparing attestation paths in existing and the updated nodes");
        List<StateContext> allContexts = new ArrayList<>();
        addSystemFieldsStateTransition(existing, modified, entityName, allContexts);
        ruleEngineService.doTransition(allContexts);
        allContexts = new ArrayList<>();
        addAttestationStateTransitions(existing, entityName, modified, allContexts);
        addOwnershipStateTransitions(existing, entityName, updated, allContexts);
        ruleEngineService.doTransition(allContexts);
    }

    private void addSystemFieldsStateTransition(JsonNode existing, JsonNode modified, String entityName, List<StateContext> allContexts) {
        StateContext stateContext = StateContext.builder()
                .entityName(entityName)
                .existing(existing.get(entityName))
                .updated(modified)
                .metadataNode((ObjectNode) modified)
                .revertSystemFields(true)
                .loginEnabled(definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getEnableLogin())
                .build();
        allContexts.add(stateContext);
    }

    public List<ObjectNode> getOwnersData(JsonNode jsonNode, String entityName) {
        List<ObjectNode> owners = new ArrayList<>();
        List<OwnershipsAttributes> ownershipAttributes = definitionsManager.getOwnershipAttributes(entityName);
        for (OwnershipsAttributes ownershipAttribute : ownershipAttributes) {
            ObjectNode ownerNode = createOwnershipNode(jsonNode, entityName, ownershipAttribute);
            owners.add(ownerNode);
        }
        return owners;
    }

    private void addOwnershipStateTransitions(JsonNode existing, String entityName, JsonNode modified, List<StateContext> allContexts) {
        List<OwnershipsAttributes> ownershipAttributes = definitionsManager.getOwnershipAttributes(entityName);
        for (OwnershipsAttributes ownershipAttribute : ownershipAttributes) {
            ObjectNode existingNode = createOwnershipNode(existing, entityName, ownershipAttribute);
            ObjectNode modifiedNode = createOwnershipNode(modified, entityName, ownershipAttribute);
            StateContext stateContext = StateContext.builder()
                    .entityName(entityName)
                    .existing(existingNode)
                    .updated(modifiedNode)
                    .metadataNode((ObjectNode) modified.get(entityName))
                    .ownershipAttribute(ownershipAttribute)
                    .loginEnabled(definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getEnableLogin())
                    .build();
            allContexts.add(stateContext);
        }
    }

    private ObjectNode createOwnershipNode(JsonNode entityNode, String entityName, OwnershipsAttributes ownershipAttribute) {
        String mobilePath = ownershipAttribute.getMobile();
        String emailPath = ownershipAttribute.getEmail();
        String userIdPath = ownershipAttribute.getUserId();
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(MOBILE, entityNode.at(String.format("/%s%s", entityName, mobilePath)).asText(""));
        objectNode.put(EMAIL, entityNode.at(String.format("/%s%s", entityName, emailPath)).asText(""));
        objectNode.put(USER_ID, entityNode.at(String.format("/%s%s", entityName, userIdPath)).asText(""));
        return objectNode;
    }

    private void addAttestationStateTransitions(JsonNode existing, String entityName, JsonNode modified, List<StateContext> allContexts) {
        List<String> ignoredProperties = definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getSystemFields();
        List<AttestationPolicy> attestationPolicies = definitionsManager.getAttestationPolicy(entityName);
        for (AttestationPolicy policy : attestationPolicies) {
            Set<EntityPropertyURI> targetPathPointers = new AttestationPath(policy.getProperty())
                    .getEntityPropertyURIs(modified, uuidPropertyName);
            logger.info("Updated nodes of interest: {}", targetPathPointers);
            for (EntityPropertyURI tp : targetPathPointers) {
                Optional<EntityPropertyURI> entityPropertyURI = EntityPropertyURI.fromEntityAndPropertyURI(
                        existing.get(entityName), tp.getPropertyURI(), uuidPropertyName
                );
                JsonNode existingSubNode = JsonNodeFactory.instance.objectNode();
                if (entityPropertyURI.isPresent()) {
                    existingSubNode = existing.get(entityName).at(entityPropertyURI.get().getJsonPointer());
                }

                Pair<ObjectNode, JsonPointer> metadataNodePointer = getTargetMetadataNodeInfo(modified, tp.getJsonPointer());
                StateContext stateContext = StateContext.builder()
                        .entityName(entityName)
                        .existing(existingSubNode)
                        .updated(modified.at(tp.getJsonPointer()))
                        .ignoredFields(ignoredProperties)
                        .attestationPolicy(policy)
                        .metadataNode(metadataNodePointer.getFirst())
                        .pointerFromMetadataNode(metadataNodePointer.getSecond())
                        .loginEnabled(definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getEnableLogin())
                        .build();
                allContexts.add(stateContext);
            }
        }
    }

    JsonNode sendForAttestation(JsonNode entityNode, String propertyURL, String notes) throws Exception {
        logger.info("Sending {} for attestation", propertyURL);
        ObjectNode metaData = JsonNodeFactory.instance.objectNode();
        metaData.set("notes", JsonNodeFactory.instance.textNode(notes));
        return manageState(entityNode, propertyURL, Action.RAISE_CLAIM, metaData);
    }

    JsonNode grantClaim(JsonNode entityNode, String propertyURI, String notes) throws Exception {
        logger.info("Claim related to {} marked as granted. Adding attestedData to metadata", propertyURI);
        ObjectNode metaData = JsonNodeFactory.instance.objectNode();
        metaData.set("notes", JsonNodeFactory.instance.textNode(notes));
        return manageState(entityNode, propertyURI, Action.GRANT_CLAIM, metaData);
    }

    JsonNode rejectClaim(JsonNode entityNode, String propertyURI, String notes) throws Exception {
        logger.info("Claim related to {} marked as rejected. Adding notes to metadata", propertyURI);
        ObjectNode metaData = JsonNodeFactory.instance.objectNode();
        metaData.set("notes", JsonNodeFactory.instance.textNode(notes));
        return manageState(entityNode, propertyURI, Action.REJECT_CLAIM, metaData);
    }


    private JsonNode manageState(JsonNode root, String propertyURL, Action action, @NotEmpty ObjectNode metaData) throws Exception {
        String entityName = root.fields().next().getKey();
        JsonNode entityNode = root.get(entityName);
        Optional<AttestationPolicy> matchingPolicy = getMatchingAttestationPolicy(entityName, entityNode, propertyURL);
        if (!matchingPolicy.isPresent()) throw new Exception(propertyURL + " did not match any attestation policy");
        AttestationPolicy policy = matchingPolicy.get();

        Optional<EntityPropertyURI> entityPropertyURI = EntityPropertyURI.fromEntityAndPropertyURI(entityNode, propertyURL, uuidPropertyName);
        if (!entityPropertyURI.isPresent()) {
            throw new Exception("Invalid Property Identifier : " + propertyURL);
        }

        Pair<ObjectNode, JsonPointer> metadataNodePointer = getTargetMetadataNodeInfo(entityNode, entityPropertyURI.get().getJsonPointer());

        if (action.equals(Action.RAISE_CLAIM)) {
            metaData.set(
                    "claimId",
                    JsonNodeFactory.instance.textNode(raiseClaim(
                            entityName,
                            entityNode.get(uuidPropertyName).asText(),
                            propertyURL,
                            metadataNodePointer.getFirst(),
                            policy,
                            EntityUtil.getFullNameOfTheEntity(entityNode),
                            metaData.get("notes").asText()
                    ))
            );
        } else if (action.equals(Action.GRANT_CLAIM)) {
            metaData.put(
                    "attestedData",
                    generateAttestedData(entityNode, policy, propertyURL)
            );
        }

        StateContext stateContext = StateContext.builder()
                .entityName(entityName)
                .existing(entityNode.at(entityPropertyURI.get().getJsonPointer()))
                .action(action)
                .ignoredFields(definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getSystemFields())
                .attestationPolicy(policy)
                .metaData(metaData)
                .metadataNode(metadataNodePointer.getFirst())
                .pointerFromMetadataNode(metadataNodePointer.getSecond())
                .loginEnabled(definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getEnableLogin())
                .build();
        ruleEngineService.doTransition(stateContext);
        return root;
    }

    private Optional<AttestationPolicy> getMatchingAttestationPolicy(String entityName, JsonNode rootNode, String uuidPath) {
        int uuidPathDepth = uuidPath.split("/").length;
        String matchingUUIDPath = "/" + uuidPath;
        for (AttestationPolicy policy : definitionsManager.getAttestationPolicy(entityName)) {
            if (policy.getProperty().split("/").length != uuidPathDepth) continue;
            if (new AttestationPath(policy.getProperty())
                    .getEntityPropertyURIs(rootNode, uuidPropertyName)
                    .stream().anyMatch(p -> p.getPropertyURI().equals(matchingUUIDPath))) {
                return Optional.of(policy);
            }
        }
        return Optional.empty();
    }

    private String raiseClaim(String entityName, String entityId, String propertyURI, JsonNode metadataNode, AttestationPolicy attestationPolicy, String requestorName, String notes) {
        String resolvedConditions = conditionResolverService.resolve(metadataNode, "REQUESTER", attestationPolicy.getConditions(), Collections.emptyList());
        ClaimDTO claimDTO = new ClaimDTO();
        claimDTO.setEntity(entityName);
        claimDTO.setEntityId(RecordIdentifier.getUUID(entityId));
        claimDTO.setPropertyURI(propertyURI);
        claimDTO.setConditions(resolvedConditions);
        claimDTO.setAttestorEntity(attestationPolicy.getAttestorEntity());
        claimDTO.setRequestorName(requestorName);
        claimDTO.setNotes(notes);
        return claimRequestClient.riseClaimRequest(claimDTO).get("id").toString();
    }

    private Pair<ObjectNode, JsonPointer> getTargetMetadataNodeInfo(JsonNode rootNode, JsonPointer targetPointer) {
        JsonNode metadataNode = rootNode.at(targetPointer);
        LinkedList<JsonPointer> traversed = new LinkedList<>();
        while (!metadataNode.isObject()) {
            traversed.addFirst(targetPointer.last());
            metadataNode = rootNode.at(targetPointer.head());
            targetPointer = targetPointer.head();
        }
        return new Pair<>(
                (ObjectNode) metadataNode,
                traversed.stream().reduce(JsonPointer.compile(""), JsonPointer::append)
        );
    }

    private String generateAttestedData(JsonNode entityNode, AttestationPolicy attestationPolicy, String propertyURI) {
        String PROPERTY_ID = "PROPERTY_ID";
        String propertyId = "";
        if (attestationPolicy.getProperty().endsWith("[]")) {
            propertyId = JsonPointer.compile("/" + propertyURI).last().getMatchingProperty();
        }

        Map<String, Object> attestedData = new HashMap<>();
        for (String path : attestationPolicy.getPaths()) {
            if (path.contains(PROPERTY_ID)) {
                path = path.replace(PROPERTY_ID, propertyId);
            }
            DocumentContext context = JsonPath.parse(entityNode.toString());
            Object result = context.read(path);
            if (result.getClass().equals(JSONArray.class)) {
                HashMap<String, Object> extractedVal = (HashMap) ((JSONArray) result).get(0);
                attestedData.putAll(extractedVal);
            } else if (result.getClass().equals(LinkedHashMap.class)) {
                attestedData.putAll((HashMap) result);
            } else {
                // It means it is just a value,
                attestedData.putAll(
                        new HashMap<String, Object>() {{
                            put(attestationPolicy.getProperty(), result);
                        }}
                );
            }
        }
        return new ObjectMapper().valueToTree(attestedData).toString();
    }


}
