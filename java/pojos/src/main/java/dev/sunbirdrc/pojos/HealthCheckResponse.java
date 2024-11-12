package dev.sunbirdrc.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class HealthCheckResponse {

	private String name;
	private boolean healthy;
	private List<ComponentHealthInfo> checks;

    @Override
	public String toString() {
		return "HealthCheckResponse{" + "name='" + name + '\'' + ", healthy=" + healthy + ", checks=" + checks + '}';
	}
}
