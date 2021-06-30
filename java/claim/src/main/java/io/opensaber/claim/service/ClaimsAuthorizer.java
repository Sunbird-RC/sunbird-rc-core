package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.claim.entity.Claim;
import io.opensaber.registry.middleware.service.ConditionResolverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class ClaimsAuthorizer {

    private static final String ATTESTOR = "ATTESTOR";
    private static final Logger logger = LoggerFactory.getLogger(ClaimsAuthorizer.class);

    @Autowired
    private ConditionResolverService conditionResolverService;

    public boolean isAuthorized(Claim claim, JsonNode attestorNode) {
        try {
            String resolvedCondition = conditionResolverService.resolve(
                    attestorNode,
                    ATTESTOR,
                    claim.getConditions(),
                    Collections.emptyList()
            );
            return conditionResolverService.evaluate(resolvedCondition);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
    }
}
