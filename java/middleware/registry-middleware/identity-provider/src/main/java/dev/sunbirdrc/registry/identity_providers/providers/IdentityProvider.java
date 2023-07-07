package dev.sunbirdrc.registry.identity_providers.providers;

import dev.sunbirdrc.registry.identity_providers.pojos.IdentityManager;
import dev.sunbirdrc.registry.identity_providers.pojos.IdentityProviderConfiguration;

public interface IdentityProvider {
	IdentityManager createManager(IdentityProviderConfiguration identityProviderConfiguration);
}
