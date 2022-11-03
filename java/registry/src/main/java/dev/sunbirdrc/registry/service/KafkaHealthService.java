package dev.sunbirdrc.registry.service;

import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.HealthIndicator;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_KAFKA_SERVICE_NAME;

@Service
@ConditionalOnProperty("async.enabled")
public class KafkaHealthService implements HealthIndicator {
	private static final Logger logger = LoggerFactory.getLogger(KafkaHealthService.class);
	@Autowired
	private AdminClient kafkaAdminClient;

	@Value("${async.enabled}")
	private boolean kafkaEnabled;

	@Override
	public String getServiceName() {
		return SUNBIRD_KAFKA_SERVICE_NAME;
	}

	@Override
	public ComponentHealthInfo getHealthInfo() {
		if (kafkaEnabled) {
			try {
				final DescribeClusterOptions options = new DescribeClusterOptions()
						.timeoutMs(10000);
				DescribeClusterResult clusterDescription = kafkaAdminClient.describeCluster(options);
				return new ComponentHealthInfo(getServiceName(), clusterDescription.nodes().get().size() > 0);
			} catch (Exception e) {
				logger.error("Kafka connection exception,", e);
				return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, e.getMessage());
			}
		} else {
			return new ComponentHealthInfo(getServiceName(), true, "ASYNC_ENABLED", "false");
		}
	}
}
