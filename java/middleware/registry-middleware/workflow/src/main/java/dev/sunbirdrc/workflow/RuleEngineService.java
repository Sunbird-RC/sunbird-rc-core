package dev.sunbirdrc.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.OwnershipsAttributes;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.middleware.util.OSSystemFields;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import dev.sunbirdrc.registry.identity_providers.pojos.IdentityManager;

import java.util.List;

import static dev.sunbirdrc.registry.middleware.util.Constants.*;

@Service
public class RuleEngineService {
    private final KieContainer kieContainer;
    private final IdentityManager identityManager;
    private final boolean authenticationEnabled;
    private static final String PATH = "path";

    @Autowired
    public RuleEngineService(KieContainer kieContainer,@Nullable IdentityManager identityManager, @Value("${authentication.enabled:true}") boolean authenticationEnabled) {
        this.kieContainer = kieContainer;
        this.identityManager = identityManager;
        this.authenticationEnabled = authenticationEnabled;
    }

    public void doTransition(List<StateContext> stateContexts) {
        StatelessKieSession kieSession = kieContainer.newStatelessKieSession();
        if(authenticationEnabled) kieSession.setGlobal("identityManager", identityManager);
        kieSession.setGlobal("ruleEngineService", this);
        kieSession.execute(stateContexts);
    }

    public void doTransition(StateContext stateContext) {
        StatelessKieSession kieSession = kieContainer.newStatelessKieSession();
        if(authenticationEnabled) kieSession.setGlobal("identityManager", identityManager);
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
