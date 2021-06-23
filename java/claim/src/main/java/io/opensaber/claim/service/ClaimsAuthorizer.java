package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.claim.entity.Claim;
import io.opensaber.registry.middleware.service.ConditionResolverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class ClaimsAuthorizer {

    private static final String ATTESTOR = "ATTESTOR";

    @Autowired
    private ConditionResolverService conditionResolverService;

    public boolean isAuthorized(Claim claim, JsonNode attestorNode) {
        String resolvedCondition = conditionResolverService.resolve(
                attestorNode,
                ATTESTOR,
                claim.getConditions(),
                Collections.emptyList()
        );
        return conditionResolverService.evaluate(resolvedCondition);
    }
}
