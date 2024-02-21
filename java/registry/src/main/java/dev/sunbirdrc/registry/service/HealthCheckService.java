package dev.sunbirdrc.registry.service;

import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.HealthCheckResponse;
import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.sink.shard.Shard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
public class HealthCheckService {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

    @Autowired(required = false)
    private List<HealthIndicator> healthIndicators;

    public HealthCheckResponse health(Shard shard) throws Exception {
        HealthCheckResponse healthCheck;
        AtomicBoolean overallHealthStatus = new AtomicBoolean(true);
        List<ComponentHealthInfo> checks = new ArrayList<>();
        if (healthIndicators != null) {
            healthIndicators.parallelStream().forEach(healthIndicator -> {
                ComponentHealthInfo healthInfo = healthIndicator.getHealthInfo();
                checks.add(healthInfo);
                overallHealthStatus.set(overallHealthStatus.get() & healthInfo.isHealthy());
            });
        }

        healthCheck = new HealthCheckResponse(Constants.SUNBIRDRC_REGISTRY_API, overallHealthStatus.get(), checks);
        logger.info("Heath Check : {}", checks.stream().map(ComponentHealthInfo::getName).collect(Collectors.toList()));
        return healthCheck;
    }
}
