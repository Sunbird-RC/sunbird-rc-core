package dev.sunbirdrc.registry.identity_providers.pojos;

import dev.sunbirdrc.pojos.HealthIndicator;

public interface IdentityManager extends HealthIndicator {
	String createUser(CreateUserRequest createUserRequest) throws IdentityException;
}
