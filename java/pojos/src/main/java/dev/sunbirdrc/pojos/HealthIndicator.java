package dev.sunbirdrc.pojos;

import org.springframework.web.client.RestClientException;

public interface HealthIndicator {
	String getServiceName();
	ComponentHealthInfo getHealthInfo() throws RestClientException;
}
