package dev.sunbirdrc.registry.authorization;

import lombok.Getter;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

public final class TenantJwtDecoder implements JwtDecoder {
    private volatile JwtDecoder jwtDecoder;
    private final String oidcIssuerLocation;
    @Getter
    private final String issuer;

    private TenantJwtDecoder(JwtDecoder jwtDecoder, String issuer) {
        this.jwtDecoder = jwtDecoder;
        this.issuer = issuer;
        this.oidcIssuerLocation = null;
    }

    // Lazy constructor: issuer = oidcIssuerLocation (OIDC spec requires they match)
    private TenantJwtDecoder(String oidcIssuerLocation) {
        this.jwtDecoder = null;
        this.issuer = oidcIssuerLocation;
        this.oidcIssuerLocation = oidcIssuerLocation;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        if (this.jwtDecoder == null) {
            synchronized (this) {
                if (this.jwtDecoder == null) {
                    try {
                        this.jwtDecoder = CustomJwtDecoders.buildDecoder(this.oidcIssuerLocation);
                    } catch (Exception e) {
                        throw new BadJwtException("Unable to initialize JWT decoder: " + e.getMessage(), e);
                    }
                }
            }
        }
        return this.jwtDecoder.decode(token);
    }

    public static TenantJwtDecoder from(JwtDecoder jwtDecoder, String issuer) {
        return new TenantJwtDecoder(jwtDecoder, issuer);
    }

    public static TenantJwtDecoder lazy(String oidcIssuerLocation) {
        return new TenantJwtDecoder(oidcIssuerLocation);
    }
}
