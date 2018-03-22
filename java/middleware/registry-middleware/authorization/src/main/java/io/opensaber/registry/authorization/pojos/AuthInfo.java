package io.opensaber.registry.authorization.pojos;

import java.security.Key;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;

public class AuthInfo extends SigningKeyResolverAdapter{
		
	private String aud;
	
	private String sub;
	
	private String name;

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
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
    	this.aud = claims.getAudience();
    	this.sub = claims.getSubject();
    	this.name = (String)claims.get("name");
        return null; // will throw exception, can be caught in caller
    }

}
