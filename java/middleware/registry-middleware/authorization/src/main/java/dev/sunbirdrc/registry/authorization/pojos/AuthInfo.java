package dev.sunbirdrc.registry.authorization.pojos;

import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthInfo extends SigningKeyResolverAdapter {
	// TODO - refactor deprecated class SigningKeyResolverAdapter
	private String aud;

	private String sub;

	private String name;

}
