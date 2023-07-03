package org.sunbirdrc.id.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * User role carries the tenant related role information for the user. A user
 * can have multiple roles per tenant based on the need of the tenant. A user
 * may also have multiple roles for multiple tenants. Author : Narendra
 */

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TenantRole {
	@JsonProperty("tenantId")
	@NotNull
	private String tenantId = null;

	@JsonProperty("roles")
	@NotNull
	private List<Role> roles = new ArrayList<Role>();
}
