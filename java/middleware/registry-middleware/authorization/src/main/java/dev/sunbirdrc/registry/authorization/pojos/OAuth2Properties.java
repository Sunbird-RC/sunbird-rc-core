package dev.sunbirdrc.registry.authorization.pojos;


import lombok.Data;

@Data
public class OAuth2Properties {
	private String rolesPath = "realm_access.roles";
	private String emailPath = "email";
	private String consentPath = "consent";
	private String entityPath = "entity";
	private String userIdPath = "sub";
}
