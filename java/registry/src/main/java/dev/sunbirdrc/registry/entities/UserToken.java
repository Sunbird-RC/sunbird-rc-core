package dev.sunbirdrc.registry.entities;

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
	private final String email;

	private final Map consentFields;

	public UserToken(Jwt source, String email, Map consentFields, List<String> entities, List<SimpleGrantedAuthority> collect) {
		super(collect);
		this.source = source;
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
		return (Principal) () -> this.source.getId();
	}
}
