package io.opensaber.registry.model.state;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.pojos.attestation.AttestationPolicy;
import io.opensaber.registry.helper.EntityStateHelper;
import io.opensaber.registry.middleware.util.JSONUtil;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Builder
public class StateContext {

    private static final Logger logger = LoggerFactory.getLogger(EntityStateHelper.class);

    private String entityName;
    private JsonNode existing;
    private JsonNode updated;
    private AttestationPolicy attestationPolicy;
    private ObjectNode metadataNode;
    private JsonPointer pointerFromMetadataNode;

    @Builder.Default
    private Action action = Action.SET_TO_DRAFT;
    @Builder.Default
    private List<String> ignoredFields = new ArrayList<>();
    @Builder.Default
    private ObjectNode metaData =JsonNodeFactory.instance.objectNode();


    private void setMetadata(String fieldName, JsonNode fieldValue) throws Exception {
        logger.info("setting metadata {} : {}", fieldName, fieldValue);
        metadataNode.set(fieldName + pointerFromMetadataNode.toString(), fieldValue);
    }

    public boolean isModified() {
        if (existing == null  && updated != null) { return true; }
        if (existing != null && updated != null) {
            JsonNode relevantExistingSubNode = existing.deepCopy();
            JSONUtil.removeNodes(relevantExistingSubNode, ignoredFields);
            JsonNode relevantUpdatedSubNode = updated.deepCopy();
            JSONUtil.removeNodes(relevantUpdatedSubNode, ignoredFields);
            return !relevantExistingSubNode.equals(relevantUpdatedSubNode);
        }
        return false;
    }

    public boolean isClaimApproved() {
        return action.equals(Action.GRANT_CLAIM);
    }

    public boolean isClaimRejected() {
        return action.equals(Action.REJECT_CLAIM);
    }

    public boolean isAttestationRequested() {
        return action.equals(Action.RAISE_CLAIM);
    }

    public void setState(States destinationState) throws Exception {
        setMetadata("_osState", JsonNodeFactory.instance.textNode(destinationState.toString()));
    }

    public void setClaimId() throws Exception {
        setMetadata("_osClaimId", metaData.get("claimId"));
    }

    public void setRejectionMessage() throws Exception {
        setMetadata("_osClaimNotes", metaData.get("notes"));
    }

    public void setAttestationData() throws Exception {
        setMetadata("_osAttestedData", metaData.get("attestedData"));
    }

    public JsonNode getUpdatedNode() {
        return updated != null ? updated : existing;
    }
}
