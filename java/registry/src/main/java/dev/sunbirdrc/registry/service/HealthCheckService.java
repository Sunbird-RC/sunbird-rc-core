package dev.sunbirdrc.registry.service;

import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.HealthCheckResponse;
import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.sink.shard.Shard;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;

@Component
public class HealthCheckService {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

    @Autowired(required = false)
    private List<HealthIndicator> healthIndicators;

    public HealthCheckResponse health(Shard shard) {
        HealthCheckResponse healthCheck;
        AtomicBoolean overallHealthStatus = new AtomicBoolean(true);
        List<ComponentHealthInfo> checks = new ArrayList<>();
        if (healthIndicators != null) {
            healthIndicators.parallelStream().forEach(healthIndicator -> {
                ComponentHealthInfo healthInfo = null;
                try {
                    healthInfo = healthIndicator.getHealthInfo();
                } catch (RestClientException e) {
                    logger.error("RestClientException when checking the health of the {}: {}", healthIndicator.getServiceName(), ExceptionUtils.getStackTrace(e));
                    healthInfo = new ComponentHealthInfo(healthIndicator.getServiceName(), false, CONNECTION_FAILURE, e.getMessage());
                }
                checks.add(healthInfo);
                overallHealthStatus.set(overallHealthStatus.get() & (healthInfo.isHealthy()));
            });
        }

        healthCheck = new HealthCheckResponse(Constants.SUNBIRDRC_REGISTRY_API, overallHealthStatus.get(), checks);
        logger.info("Heath Check : {}", checks.stream().map(ComponentHealthInfo::getName).collect(Collectors.toList()));
        return healthCheck;
    }
}
