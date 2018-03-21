package io.opensaber.registry.authorization.pojos;

import java.security.Key;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;

public class AuthInfo extends SigningKeyResolverAdapter{
		
	private String aud;
	
	private String sub;

	public String getAud() {
		return aud;
	}

	public void setAud(String aud) {
		this.aud = aud;
	}

	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}
	
	@Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
    	this.aud = claims.getAudience();
    	this.sub = claims.getSubject();
        return null; // will throw exception, can be caught in caller
    }

}
