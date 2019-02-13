package io.opensaber.registry.authorization;

import io.opensaber.registry.authorization.pojos.AuthInfo;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class AuthorizationToken extends AbstractAuthenticationToken {

	private final AuthInfo authInfo;
	private String token;

	public AuthorizationToken(AuthInfo authInfo, List<SimpleGrantedAuthority> grantedAuthorities) {
		super(grantedAuthorities);
		this.authInfo = authInfo;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
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
