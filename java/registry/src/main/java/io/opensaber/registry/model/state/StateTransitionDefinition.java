package io.opensaber.registry.model.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.registry.middleware.util.JSONUtil;

public class StateTransitionDefinition {
    JsonNode existingNode;
    String currentRole;
    JsonNode requestBody;

    public StateTransitionDefinition(JsonNode existingNode, String currentRole) {
        this.existingNode = existingNode;
        this.currentRole = currentRole;
    }

    public StateTransitionDefinition(String currentRole, JsonNode requestBody) {
        this.currentRole = currentRole;
        this.requestBody = requestBody;
    }

    public StateTransitionDefinition(JsonNode existingNode, String currentRole, JsonNode requestBody) {
        this.existingNode = existingNode;
        this.currentRole = currentRole;
        this.requestBody = requestBody;
    }

    public boolean isAttributesContainsSameValue() {
        // TODO: Add logic to compare the request body with existing json nodes.

        return true;
    }
    public void setState(String destinationState) {
        ((ObjectNode)existingNode).put("state", destinationState);
    }

    public void print() {
        System.out.println(existingNode.toString());
    }

    public JsonNode getExistingNode() {
        return existingNode;
    }

    public void setExistingNode(JsonNode existingNode) {
        this.existingNode = existingNode;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    public JsonNode getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(JsonNode requestBody) {
        this.requestBody = requestBody;
    }
}
