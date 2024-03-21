package dev.sunbirdrc.registry.authorization;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.Assert;

import static org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withJwkSetUri;

final class CustomJwtDecoders {

    public static TenantJwtDecoder fromOidcIssuerLocation(String oidcIssuerLocation) {
        Assert.hasText(oidcIssuerLocation, "oidcIssuerLocation cannot be empty");
        Map<String, Object> configuration = CustomJwtDecoderProviderConfigurationUtils.getConfigurationForOidcIssuerLocation(oidcIssuerLocation);
        return withProviderConfiguration(configuration);
    }
    
    private static TenantJwtDecoder withProviderConfiguration(Map<String, Object> configuration) {
        String metadataIssuer = CustomJwtDecoderProviderConfigurationUtils.getIssuer(configuration);
        OAuth2TokenValidator<Jwt> jwtValidator = JwtValidators.createDefaultWithIssuer(metadataIssuer);
        NimbusJwtDecoder jwtDecoder = withJwkSetUri(configuration.get("jwks_uri").toString()).build();
        jwtDecoder.setJwtValidator(jwtValidator);
        return TenantJwtDecoder.from(jwtDecoder, metadataIssuer);
    }

    private CustomJwtDecoders() {}
}

