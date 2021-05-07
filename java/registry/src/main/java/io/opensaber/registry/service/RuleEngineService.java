package io.opensaber.registry.service;

import io.opensaber.registry.model.state.StateContext;
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

    public void doTransition(StateContext stateContext) {
        KieSession kieSession = kieContainer.newKieSession();
        kieSession.insert(stateContext);
        kieSession.fireAllRules();
        kieSession.dispose();
    }
}
