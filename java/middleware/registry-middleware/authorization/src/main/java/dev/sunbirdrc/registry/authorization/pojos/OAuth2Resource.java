package dev.sunbirdrc.registry.authorization.pojos;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class OAuth2Resource {
	@NotBlank
	private String uri;
	private String rolesPath = "realm_access.roles";
	private String emailPath = "email";
	private String consentPath = "consent";
	private String entityPath = "entity";
	private String userIdPath = "sub";
}
