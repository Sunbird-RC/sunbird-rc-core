package io.opensaber.registry.model.state;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.pojos.attestation.AttestationPolicy;
import io.opensaber.registry.helper.EntityStateHelper;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.util.Definition;
import io.opensaber.registry.util.OwnershipsAttributes;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.opensaber.registry.middleware.util.Constants.*;
import static io.opensaber.registry.middleware.util.OSSystemFields.*;

@Builder
@Getter
public class StateContext {

    private static final Logger logger = LoggerFactory.getLogger(EntityStateHelper.class);

    private String entityName;
    private JsonNode existing;
    private JsonNode updated;
    private AttestationPolicy attestationPolicy;
    private ObjectNode metadataNode;
    private JsonPointer pointerFromMetadataNode;
    private Definition definition;
    private OwnershipsAttributes ownershipAttribute;
    @Builder.Default
    private boolean revertSystemFields = false;

    @Builder.Default
    private Action action = Action.SET_TO_DRAFT;
    @Builder.Default
    private List<String> ignoredFields = new ArrayList<>();
    @Builder.Default
    private ObjectNode metaData = JsonNodeFactory.instance.objectNode();


    private void setMetadata(String fieldName, JsonNode fieldValue) throws Exception {
        logger.info("setting metadata {} : {}", fieldName, fieldValue);
        metadataNode.set(fieldName + pointerFromMetadataNode.toString(), fieldValue);
    }

    public boolean isModified() {
        if (existing == null && updated != null) {
            return true;
        }
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
        setMetadata(_osState.toString(), JsonNodeFactory.instance.textNode(destinationState.toString()));
    }

    public void setClaimId() throws Exception {
        setMetadata(_osClaimId.toString(), metaData.get("claimId"));
    }

    public void setNotes() throws Exception {
        setMetadata(_osClaimNotes.toString(), metaData.get("notes"));
    }

    public void setAttestationData() throws Exception {
        setMetadata(_osAttestedData.toString(), metaData.get("attestedData"));
    }

    public JsonNode getUpdatedNode() {
        return updated != null ? updated : existing;
    }

    public boolean isOwnerNewlyAdded() {
        if (StringUtils.isEmpty(getStringValue(existing, USER_ID)) && (StringUtils.isEmpty(getStringValue(existing, EMAIL)) || StringUtils.isEmpty(getStringValue(existing, MOBILE)))) {
            return !StringUtils.isEmpty(getStringValue(updated, USER_ID)) && (!StringUtils.isEmpty(getStringValue(updated, EMAIL)) || !StringUtils.isEmpty(getStringValue(updated, MOBILE)));
        }
        return false;
    }

    private String getStringValue(JsonNode jsonNode, String key) {
        return jsonNode == null || jsonNode.get(key) == null ? null : jsonNode.get(key).textValue();
    }

    public void addOwner(String owner) {
        ArrayNode arrayNode = (ArrayNode) metadataNode.get(osOwner.toString());
        arrayNode.add(owner);
    }

    public boolean isAttestationProperty() {
        return attestationPolicy != null;
    }

    public boolean isOwnershipDetailsUpdated() {
        JsonNode patchNodes = JSONUtil.diffJsonNode(existing, updated);
        return patchNodes.size() > 0;
    }

    public boolean isOwnershipProperty() {
        return ownershipAttribute != null;
    }

    public boolean revertSystemFieldsChangedEnabled() {
        return this.revertSystemFields;
    }

}
