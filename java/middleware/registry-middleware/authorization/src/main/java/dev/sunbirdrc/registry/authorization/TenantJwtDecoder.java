package dev.sunbirdrc.registry.authorization;

import lombok.Getter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

public final class TenantJwtDecoder implements JwtDecoder {
    JwtDecoder jwtDecoder;
    @Getter
    String issuer;
    private TenantJwtDecoder(JwtDecoder jwtDecoder, String issuer) {
        this.jwtDecoder = jwtDecoder;
        this.issuer = issuer;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        return this.jwtDecoder.decode(token);
    }

    public static TenantJwtDecoder from(JwtDecoder jwtDecoder, String issuer) {
        return new TenantJwtDecoder(jwtDecoder, issuer);
    }
}
