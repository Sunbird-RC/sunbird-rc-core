package dev.sunbirdrc.auth.keycloak;

import dev.sunbirdrc.registry.identity_providers.pojos.IdentityManager;
import dev.sunbirdrc.registry.identity_providers.pojos.IdentityProviderConfiguration;
import dev.sunbirdrc.registry.identity_providers.providers.IdentityProvider;

public class KeycloakProviderImpl implements IdentityProvider {
	@Override
	public IdentityManager createManager(IdentityProviderConfiguration identityProviderConfiguration) {
		return new KeycloakAdminUtil(identityProviderConfiguration);
	}
}
