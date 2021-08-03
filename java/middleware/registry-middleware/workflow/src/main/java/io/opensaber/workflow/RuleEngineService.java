package io.opensaber.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.keycloak.KeycloakAdminUtil;
import io.opensaber.pojos.OwnershipsAttributes;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.middleware.util.OSSystemFields;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.opensaber.registry.middleware.util.Constants.*;

@Service
public class RuleEngineService {
    private final KieContainer kieContainer;
    private final KeycloakAdminUtil keycloakAdminUtil;
    private static final String PATH = "path";

    @Autowired
    public RuleEngineService(KieContainer kieContainer, KeycloakAdminUtil keycloakAdminUtil) {
        this.kieContainer = kieContainer;
        this.keycloakAdminUtil = keycloakAdminUtil;
    }

    public void doTransition(List<StateContext> stateContexts) {
        StatelessKieSession kieSession = kieContainer.newStatelessKieSession();
        kieSession.setGlobal("keycloakAdminUtil", keycloakAdminUtil);
        kieSession.setGlobal("ruleEngineService", this);
        kieSession.execute(stateContexts);
    }

    public void doTransition(StateContext stateContext) {
        StatelessKieSession kieSession = kieContainer.newStatelessKieSession();
        kieSession.setGlobal("keycloakAdminUtil", keycloakAdminUtil);
        kieSession.setGlobal("ruleEngineService", this);
        kieSession.execute(stateContext);
    }

    public void revertOwnershipDetails(StateContext stateContext) {
        OwnershipsAttributes ownershipAttribute = stateContext.getOwnershipAttribute();
        ObjectNode updatedNode = stateContext.getMetadataNode();
        JsonNode existing = stateContext.getExisting();
        String mobilePath = ownershipAttribute.getMobile();
        String emailPath = ownershipAttribute.getEmail();
        String userIdPath = ownershipAttribute.getUserId();
        JSONUtil.replaceFieldByPointerPath(updatedNode, mobilePath, existing.get(MOBILE));
        JSONUtil.replaceFieldByPointerPath(updatedNode, emailPath, existing.get(EMAIL));
        JSONUtil.replaceFieldByPointerPath(updatedNode, userIdPath, existing.get(USER_ID));
    }

    public void revertSystemFields(StateContext stateContext) {
        JsonNode updated = stateContext.getUpdated();
        JsonNode existing = stateContext.getExisting();
        ObjectNode metadataNode = stateContext.getMetadataNode();
        JsonNode patchNodes = JSONUtil.diffJsonNode(existing, updated);
        for (JsonNode patchNode : patchNodes) {
            String updatedPath = patchNode.get(PATH).textValue();
            for (OSSystemFields value : OSSystemFields.values()) {
                if (updatedPath.contains(value.toString())) {
                    String path = getPathToUpdate(updatedPath, value);
                    JSONUtil.replaceFieldByPointerPath(metadataNode, path, existing.at(path));
                }
            }
        }
    }

    private String getPathToUpdate(String updatedPath, OSSystemFields value) {
        return updatedPath.substring(0, updatedPath.lastIndexOf(value.toString()) + value.toString().length());
    }

}
