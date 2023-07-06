package dev.sunbirdrc.auth.genericiam;

import dev.sunbirdrc.registry.identity_providers.pojos.IdentityManager;
import dev.sunbirdrc.registry.identity_providers.pojos.IdentityProviderConfiguration;
import dev.sunbirdrc.registry.identity_providers.providers.IdentityProvider;

public class AuthProviderImpl implements IdentityProvider {
	@Override
	public IdentityManager createManager(IdentityProviderConfiguration identityProviderConfiguration) {
		return new AdminUtil(identityProviderConfiguration);
	}
}
