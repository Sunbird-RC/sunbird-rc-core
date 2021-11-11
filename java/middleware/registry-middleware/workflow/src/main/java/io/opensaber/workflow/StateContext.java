package io.opensaber.workflow;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.pojos.OwnershipsAttributes;
import io.opensaber.pojos.attestation.Action;
import io.opensaber.pojos.attestation.AttestationPolicy;
import io.opensaber.pojos.attestation.States;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.middleware.util.OSSystemFields;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
public class StateContext {

    private static final Logger logger = LoggerFactory.getLogger(StateContext.class);

    private String entityName;
    private JsonNode existing;
    private JsonNode updated;
    private AttestationPolicy attestationPolicy;
    private ObjectNode metadataNode;
    private JsonPointer pointerFromMetadataNode;
    private OwnershipsAttributes ownershipAttribute;
    private Boolean loginEnabled;

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
        setMetadata(OSSystemFields._osState.toString(), JsonNodeFactory.instance.textNode(destinationState.toString()));
    }

    public void setClaimId() throws Exception {
        setMetadata(OSSystemFields._osClaimId.toString(), metaData.get("claimId"));
    }

    public void setAttestationData() throws Exception {
        setMetadata(OSSystemFields._osAttestedData.toString(), metaData.get("attestedData"));
    }

    public JsonNode getUpdatedNode() {
        return updated != null ? updated : existing;
    }

    public boolean isLoginEnabled() {
        return loginEnabled;
    }

    public boolean isOwnerNewlyAdded() {
        if (StringUtils.isEmpty(getStringValue(existing, Constants.USER_ID)) && (StringUtils.isEmpty(getStringValue(existing, Constants.EMAIL)) || StringUtils.isEmpty(getStringValue(existing, Constants.MOBILE)))) {
            return !StringUtils.isEmpty(getStringValue(updated, Constants.USER_ID)) && (!StringUtils.isEmpty(getStringValue(updated, Constants.EMAIL)) || !StringUtils.isEmpty(getStringValue(updated, Constants.MOBILE)));
        }
        return false;
    }

    private String getStringValue(JsonNode jsonNode, String key) {
        return jsonNode == null || jsonNode.get(key) == null ? null : jsonNode.get(key).textValue();
    }

    public void addOwner(String owner) throws IOException {
        ArrayNode arrayNode = (ArrayNode) metadataNode.get(OSSystemFields.osOwner.toString());
        if (arrayNode == null) {
            metadataNode.set(OSSystemFields.osOwner.toString(), new ObjectMapper().readTree("[\"" + owner + "\"]"));
        } else {
            arrayNode.add(owner);
        }
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

    public Boolean getLoginEnabled() {
        return loginEnabled;
    }

    public void setLoginEnabled(Boolean loginEnabled) {
        this.loginEnabled = loginEnabled;
    }
}
