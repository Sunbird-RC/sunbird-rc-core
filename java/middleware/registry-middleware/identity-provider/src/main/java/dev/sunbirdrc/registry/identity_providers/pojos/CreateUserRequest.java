package dev.sunbirdrc.registry.identity_providers.pojos;


public class CreateUserRequest {
	private final String userName;
	private final String email;
	private final String mobile;
	private final String entity;

	public CreateUserRequest(String entity, String userName, String email, String mobile) {
		this.userName = userName;
		this.email = email;
		this.mobile = mobile;
		this.entity = entity;
	}

	public String getUserName() {
		return userName;
	}

	public String getEmail() {
		return email;
	}

	public String getMobile() {
		return mobile;
	}

	public String getEntity() {
		return entity;
	}
}
