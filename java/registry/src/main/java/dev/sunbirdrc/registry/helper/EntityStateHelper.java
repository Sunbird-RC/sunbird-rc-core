package dev.sunbirdrc.registry.helper;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import dev.sunbirdrc.pojos.OwnershipsAttributes;
import dev.sunbirdrc.pojos.attestation.Action;
import dev.sunbirdrc.pojos.dto.ClaimDTO;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.middleware.service.ConditionResolverService;
import dev.sunbirdrc.registry.middleware.util.EntityUtil;
import dev.sunbirdrc.registry.model.attestation.AttestationPath;
import dev.sunbirdrc.registry.model.attestation.EntityPropertyURI;
import dev.sunbirdrc.registry.util.ClaimRequestClient;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.registry.util.RecordIdentifier;
import dev.sunbirdrc.workflow.RuleEngineService;
import dev.sunbirdrc.workflow.StateContext;
import net.minidev.json.JSONArray;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotEmpty;
import java.util.*;

import static dev.sunbirdrc.registry.middleware.util.Constants.*;

@Component
public class EntityStateHelper {

    private static final Logger logger = LoggerFactory.getLogger(EntityStateHelper.class);


    @Value("${database.uuidPropertyName}")
    private String uuidPropertyName;

    private final IDefinitionsManager definitionsManager;

    private final RuleEngineService ruleEngineService;

    private final ConditionResolverService conditionResolverService;

    private final ClaimRequestClient claimRequestClient;

    @Autowired
    public EntityStateHelper(IDefinitionsManager definitionsManager, RuleEngineService ruleEngineService,
                             ConditionResolverService conditionResolverService, ClaimRequestClient claimRequestClient) {
        this.definitionsManager = definitionsManager;
        this.ruleEngineService = ruleEngineService;
        this.conditionResolverService = conditionResolverService;
        this.claimRequestClient = claimRequestClient;
    }

    void applyWorkflowTransitions(JsonNode existing, JsonNode updated, List<AttestationPolicy> attestationPolicies) {
        String entityName = updated.fields().next().getKey();
        JsonNode modified = updated.get(entityName);
        logger.info("Detecting state changes by comparing attestation paths in existing and the updated nodes");
        List<StateContext> allContexts = new ArrayList<>();
        addSystemFieldsStateTransition(existing, modified, entityName, allContexts);
        ruleEngineService.doTransition(allContexts);
        allContexts = new ArrayList<>();
        addAttestationStateTransitions(existing, entityName, modified, allContexts, attestationPolicies);
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
        objectNode.set(ROLES, entityNode.at(String.format("/%s", entityName)).has(ROLES) ? entityNode.at(String.format("/%s%s", entityName, "/roles")) : new ObjectMapper().createArrayNode());
        return objectNode;
    }

    private void addAttestationStateTransitions(JsonNode existing, String entityName, JsonNode modified, List<StateContext> allContexts, List<AttestationPolicy> attestationPolicies) {
        List<String> ignoredProperties = definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getSystemFields();
        for (AttestationPolicy policy : attestationPolicies) {
            Set<EntityPropertyURI> targetPathPointers = new AttestationPath(policy.getNodePath())
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
                        .isAttestationProperty(true)
                        .metadataNode(metadataNodePointer.getFirst())
                        .pointerFromMetadataNode(metadataNodePointer.getSecond())
                        .loginEnabled(definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getEnableLogin())
                        .build();
                allContexts.add(stateContext);
            }
        }
    }

    JsonNode manageState(AttestationPolicy policy, JsonNode root, String propertyURL, Action action, @NotEmpty ObjectNode metaData) throws Exception {
        String entityName = root.fields().next().getKey();
        JsonNode entityNode = root.get(entityName);

        Optional<EntityPropertyURI> entityPropertyURI = EntityPropertyURI.fromEntityAndPropertyURI(entityNode, propertyURL, uuidPropertyName);
        if (!entityPropertyURI.isPresent()) {
            throw new Exception("Invalid Property Identifier : " + propertyURL);
        }
        Pair<ObjectNode, JsonPointer> metadataNodePointer = getTargetMetadataNodeInfo(entityNode, entityPropertyURI.get().getJsonPointer());

        StateContext stateContext = StateContext.builder()
                .entityName(entityName)
                .existing(entityNode.at(entityPropertyURI.get().getJsonPointer()))
                .action(action)
                .ignoredFields(definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getSystemFields())
                .isAttestationProperty(policy != null)
                .metaData(metaData)
                .metadataNode(metadataNodePointer.getFirst())
                .pointerFromMetadataNode(metadataNodePointer.getSecond())
                .build();
        ruleEngineService.doTransition(stateContext);
        return root;
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


}
