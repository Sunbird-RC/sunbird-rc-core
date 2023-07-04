package dev.sunbirdrc.auth.auth0;

import dev.sunbirdrc.registry.identity_providers.pojos.IdentityManager;
import dev.sunbirdrc.registry.identity_providers.pojos.IdentityProviderConfiguration;
import dev.sunbirdrc.registry.identity_providers.providers.IdentityProvider;

public class Auth0ProviderImpl implements IdentityProvider {
	@Override
	public IdentityManager createManager(IdentityProviderConfiguration identityProviderConfiguration) {
		return new Auth0AdminUtil(identityProviderConfiguration);
	}
}
