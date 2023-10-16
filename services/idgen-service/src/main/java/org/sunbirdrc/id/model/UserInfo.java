package org.sunbirdrc.id.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * This is acting ID token of the authenticated user on the server. Any value
 * provided by the clients will be ignored and actual user based on authtoken
 * will be used on the server. Author : Narendra
 */

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
	@JsonProperty("tenantId")
	@Size(max=256)
	@NotNull
	private String tenantId = null;

	@JsonProperty("id")
	private Integer id = null;

	@JsonProperty("username")
	@Size(max=64)
	@NotNull
	private String username = null;

	@JsonProperty("mobile")
	@Pattern(regexp = "^[0-9]{10}$", message = "MobileNumber should be 10 digit number")
	private String mobile = null;

	@JsonProperty("email")
	@Size(max=128)
	private String email = null;

	@JsonProperty("primaryrole")
	@NotNull
	private List<Role> primaryrole = new ArrayList<Role>();

	@JsonProperty("additionalroles")
	private List<TenantRole> additionalroles = new ArrayList<TenantRole>();
}
