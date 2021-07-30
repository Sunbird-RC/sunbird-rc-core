package io.opensaber.registry.service;

import io.opensaber.registry.helper.EntityStateHelper;
import io.opensaber.registry.model.state.StateContext;
import io.opensaber.registry.util.KeycloakAdminUtil;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleEngineService {
    private final KieContainer kieContainer;
    private final KeycloakAdminUtil keycloakAdminUtil;

    @Autowired
    public RuleEngineService(KieContainer kieContainer, KeycloakAdminUtil keycloakAdminUtil) {
        this.kieContainer = kieContainer;
        this.keycloakAdminUtil = keycloakAdminUtil;
    }

    public void doTransition(List<StateContext> stateContexts, EntityStateHelper entityStateHelper) {
        StatelessKieSession kieSession = kieContainer.newStatelessKieSession();
        kieSession.setGlobal("keycloakAdminUtil", keycloakAdminUtil);
        kieSession.setGlobal("entityStateHelper", entityStateHelper);
        kieSession.execute(stateContexts);
    }

    public void doTransition(StateContext stateContext, EntityStateHelper entityStateHelper) {
        StatelessKieSession kieSession = kieContainer.newStatelessKieSession();
        kieSession.setGlobal("keycloakAdminUtil", keycloakAdminUtil);
        kieSession.setGlobal("entityStateHelper", entityStateHelper);
        kieSession.execute(stateContext);
    }
}
