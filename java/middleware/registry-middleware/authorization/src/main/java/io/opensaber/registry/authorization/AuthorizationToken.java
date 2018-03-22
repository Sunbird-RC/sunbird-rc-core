package io.opensaber.registry.authorization;

import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import io.opensaber.registry.authorization.pojos.AuthInfo;


public class AuthorizationToken extends AbstractAuthenticationToken{
	
	private String token;
    private final AuthInfo authInfo;

    public AuthorizationToken( AuthInfo authInfo, List<SimpleGrantedAuthority> grantedAuthorities ) {
    	super(grantedAuthorities);
        this.authInfo = authInfo;
    }

    public String getToken() {
        return token;
    }

    public void setToken( String token ) {
        this.token = token;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }


    public Object getCredentials() {
        return token;
    }

    public AuthInfo getPrincipal() {
        return authInfo;
    }

}
