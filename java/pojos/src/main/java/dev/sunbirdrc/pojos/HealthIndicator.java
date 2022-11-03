package dev.sunbirdrc.pojos;

public interface HealthIndicator {
	String getServiceName();
	ComponentHealthInfo getHealthInfo();
}
