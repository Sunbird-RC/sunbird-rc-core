package io.opensaber.registry.service;

import io.opensaber.registry.model.state.StateTransitionDefinition;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RuleEngineService {
    private final KieContainer kieContainer;

    @Autowired
    public RuleEngineService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    public StateTransitionDefinition doTransition(StateTransitionDefinition stateTransitionDefinition) {
        KieSession kieSession = kieContainer.newKieSession();
        kieSession.insert(stateTransitionDefinition);
        kieSession.fireAllRules();
        kieSession.dispose();
        return stateTransitionDefinition;
    }
}
