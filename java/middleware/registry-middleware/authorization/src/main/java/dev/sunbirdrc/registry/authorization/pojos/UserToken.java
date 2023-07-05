package dev.sunbirdrc.registry.authorization.pojos;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Getter
public class UserToken extends AbstractAuthenticationToken {
	private final List<String> entities;
	private final Jwt source;
	private String userId;
	private final String email;

	private final Map<String, Integer> consentFields;

	public UserToken(Jwt source, String userId, String email, Map<String, Integer> consentFields, List<String> entities, List<SimpleGrantedAuthority> authorities) {
		super(authorities);
		this.source = source;
		this.userId = userId;
		this.email = email;
		this.consentFields = consentFields;
		this.entities = entities;
		this.setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return null;
	}

	@Override
	public Object getPrincipal() {
		return (Principal) this.source::getId;
	}
}
