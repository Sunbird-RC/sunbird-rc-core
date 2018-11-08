package io.opensaber.registry.authorization.pojos;

import io.jsonwebtoken.SigningKeyResolverAdapter;

public class AuthInfo extends SigningKeyResolverAdapter {

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

}
