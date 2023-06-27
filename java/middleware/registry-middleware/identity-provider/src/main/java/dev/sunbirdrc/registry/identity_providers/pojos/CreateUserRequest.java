package dev.sunbirdrc.registry.identity_providers.pojos;


import lombok.Data;

@Data
public class CreateUserRequest {
	private final String userName;
	private final String email;
	private final String mobile;
	private final String entity;

	private final String password;
	public CreateUserRequest(String entity, String userName, String email, String mobile, String password) {
		this.userName = userName;
		this.email = email;
		this.mobile = mobile;
		this.entity = entity;
		this.password = password;
	}

}
