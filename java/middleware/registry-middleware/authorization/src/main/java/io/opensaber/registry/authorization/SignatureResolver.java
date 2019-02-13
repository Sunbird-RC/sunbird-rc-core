package io.opensaber.registry.authorization;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;

import java.security.Key;

public class SignatureResolver extends SigningKeyResolverAdapter {

	@Override
	public Key resolveSigningKey(JwsHeader header, Claims claims) {
		// Examine header and claims
		// AuthInfo.subject = claims.getSubject();
		return null; // will throw exception, can be caught in caller
	}

}
