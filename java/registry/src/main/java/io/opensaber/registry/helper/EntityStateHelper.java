package io.opensaber.registry.helper;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.pojos.attestation.AttestationPolicy;
import io.opensaber.pojos.dto.ClaimDTO;
import io.opensaber.registry.middleware.service.ConditionResolverService;
import io.opensaber.registry.model.attestation.AttestationPath;
import io.opensaber.registry.model.attestation.EntityPropertyURI;
import io.opensaber.registry.model.state.Action;
import io.opensaber.registry.model.state.StateContext;
import io.opensaber.registry.service.RuleEngineService;
import io.opensaber.registry.util.ClaimRequestClient;
import io.opensaber.registry.util.DefinitionsManager;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotEmpty;
import java.util.*;

@Component
public class EntityStateHelper {

    @Value("${database.uuidPropertyName}")
    private String uuidPropertyName;

    @Autowired
    private DefinitionsManager definitionsManager;

    @Autowired
    private RuleEngineService ruleEngineService;

    @Autowired
    private ConditionResolverService conditionResolverService;

    @Autowired
    private ClaimRequestClient claimRequestClient;

    public void changeStateAfterUpdate(JsonNode existing, JsonNode updated) {
        String entityName = updated.fields().next().getKey();
        JsonNode modified = updated.get(entityName);

        List<String> ignoredProperties = definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getSystemFields();
        List<AttestationPolicy> attestationPolicies = definitionsManager.getAttestationPolicy(entityName);
        List<StateContext> allContexts = new ArrayList<>();

        for (AttestationPolicy policy: attestationPolicies) {
            Set<EntityPropertyURI> targetPathPointers = new AttestationPath(policy.getProperty())
                    .getEntityPropertyURIs(modified, uuidPropertyName);

            for(EntityPropertyURI tp: targetPathPointers) {
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
                        .build();
                allContexts.add(stateContext);
            }
        }
        ruleEngineService.doTransition(allContexts);
    }

    public JsonNode sendForAttestation(JsonNode entityNode, String propertyURL) throws Exception {
       return manageState(entityNode, propertyURL, Action.RAISE_CLAIM, JsonNodeFactory.instance.objectNode());
    }

    public JsonNode attestClaim(JsonNode entityNode, String uuidPath) throws Exception {
        return manageState(entityNode, uuidPath, Action.GRANT_CLAIM, JsonNodeFactory.instance.objectNode());
    }

    private JsonNode manageState(JsonNode root, String propertyURL, Action action, @NotEmpty ObjectNode metaData) throws Exception {
        String entityName = root.fields().next().getKey();
        JsonNode entityNode = root.get(entityName);
        Optional<AttestationPolicy> matchingPolicy = getMatchingAttestationPolicy(entityName, entityNode, propertyURL);
        if (!matchingPolicy.isPresent()) throw new Exception(propertyURL + " did not match any attestation policy");
        AttestationPolicy policy = matchingPolicy.get();

        Optional<EntityPropertyURI> entityPropertyURI = EntityPropertyURI.fromEntityAndPropertyURI(entityNode, propertyURL, uuidPropertyName);
        if (!entityPropertyURI.isPresent()) {
            throw new Exception("Invalid Property Identifier : "+ propertyURL);
        }

        Pair<ObjectNode, JsonPointer> metadataNodePointer = getTargetMetadataNodeInfo(entityNode, entityPropertyURI.get().getJsonPointer());

        if (action.equals(Action.RAISE_CLAIM)) {
            metaData.set(
                    "claimId",
                    JsonNodeFactory.instance.textNode(raiseClaim(entityName, propertyURL, metadataNodePointer.getFirst(), policy))
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
                .build();
        ruleEngineService.doTransition(stateContext);
        return root;
    }

    public Optional<AttestationPolicy> getMatchingAttestationPolicy(String entityName, JsonNode rootNode, String uuidPath) {
        int uuidPathDepth = uuidPath.split("/").length;
        String matchingUUIDPath = "/" + uuidPath;
        for (AttestationPolicy policy: definitionsManager.getAttestationPolicy(entityName)) {
            if (policy.getProperty().split("/").length != uuidPathDepth) continue;
           if (new AttestationPath(policy.getProperty())
                    .getEntityPropertyURIs(rootNode, uuidPropertyName)
                    .stream().anyMatch(p -> p.getPropertyURI().equals(matchingUUIDPath))) {
               return Optional.of(policy);
           }
        }
        return Optional.empty();
    }

    public String raiseClaim(String entityName, String uuidPath, JsonNode node, AttestationPolicy attestationPolicy) {
        String resolvedConditions =  conditionResolverService.resolve(node, "REQUESTER", attestationPolicy.getConditions(), Collections.emptyList());
        ClaimDTO claimDTO = new ClaimDTO();
        claimDTO.setEntity(entityName);
        claimDTO.setEntityId(node.get(uuidPropertyName).asText());
        claimDTO.setPropertyURI(uuidPath);
        claimDTO.setConditions(resolvedConditions);
        claimDTO.setAttestorEntity(attestationPolicy.getAttestorEntity());
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

}
