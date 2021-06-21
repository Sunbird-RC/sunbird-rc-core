package io.opensaber.registry.service;

import io.opensaber.registry.model.state.StateContext;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleEngineService {
    private final KieContainer kieContainer;

    @Autowired
    public RuleEngineService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    public void doTransition(List<StateContext> stateContexts) {
        StatelessKieSession kieSession = kieContainer.newStatelessKieSession();
        kieSession.execute(stateContexts);
    }

    public void doTransition(StateContext stateContext) {
        StatelessKieSession kieSession = kieContainer.newStatelessKieSession();
        kieSession.execute(stateContext);
    }
}
