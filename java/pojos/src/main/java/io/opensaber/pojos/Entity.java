package io.opensaber.pojos;

public class Entity {

	private Object claim;

	private String signatureValue;

	private Integer keyId;

	public Object getClaim() {
		return claim;
	}

	public void setClaim(Object claim) {
		this.claim = claim;
	}

	public String getSignatureValue() {
		return signatureValue;
	}

	public void setSignatureValue(String signatureValue) {
		this.signatureValue = signatureValue;
	}

	public Integer getKeyId() {
		return keyId;
	}

	public void setKeyId(Integer keyId) {
		this.keyId = keyId;
	}

}
