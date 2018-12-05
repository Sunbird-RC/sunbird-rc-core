package io.opensaber.pojos;

import java.util.List;

public class HealthCheckResponse {

	private String name;
	private boolean healthy;
	private List<ComponentHealthInfo> checks;

	public HealthCheckResponse(String name, boolean healthy, List<ComponentHealthInfo> checks) {
		this.name = name;
		this.healthy = healthy;
		this.checks = checks;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isHealthy() {
		return healthy;
	}

	public void setHealthy(boolean healthy) {
		this.healthy = healthy;
	}

	public List<ComponentHealthInfo> getChecks() {
		return checks;
	}

	public void setChecks(List<ComponentHealthInfo> checks) {
		this.checks = checks;
	}

	@Override
	public String toString() {
		return "HealthCheckResponse{" + "name='" + name + '\'' + ", healthy=" + healthy + ", checks=" + checks + '}';
	}
}
