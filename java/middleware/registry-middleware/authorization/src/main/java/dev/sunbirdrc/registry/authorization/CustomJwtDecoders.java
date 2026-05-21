package dev.sunbirdrc.registry.authorization;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.Assert;

import static org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withJwkSetUri;

final class CustomJwtDecoders {

    // Returns a lazy decoder — no HTTP call to Keycloak at startup.
    // The OIDC discovery fetch is deferred until the first token decode.
    public static TenantJwtDecoder fromOidcIssuerLocation(String oidcIssuerLocation) {
        Assert.hasText(oidcIssuerLocation, "oidcIssuerLocation cannot be empty");
        return TenantJwtDecoder.lazy(oidcIssuerLocation);
    }

    static NimbusJwtDecoder buildDecoder(String oidcIssuerLocation) {
        Map<String, Object> configuration = CustomJwtDecoderProviderConfigurationUtils.getConfigurationForOidcIssuerLocation(oidcIssuerLocation);
        String metadataIssuer = CustomJwtDecoderProviderConfigurationUtils.getIssuer(configuration);
        OAuth2TokenValidator<Jwt> jwtValidator = JwtValidators.createDefaultWithIssuer(metadataIssuer);
        NimbusJwtDecoder jwtDecoder = withJwkSetUri(configuration.get("jwks_uri").toString()).build();
        jwtDecoder.setJwtValidator(jwtValidator);
        return jwtDecoder;
    }

    private CustomJwtDecoders() {}
}

