package dev.sunbirdrc.registry.authorization.pojos;


import lombok.Data;

@Data
public class OAuth2Properties {
	private String roles = "realm_access.roles";
	private String email = "email";
	private String consent = "xyz";
	private String entity = "entity";
}
