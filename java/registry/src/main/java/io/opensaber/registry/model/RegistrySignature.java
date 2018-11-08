package io.opensaber.registry.model;

/**
 * Represents what registry puts up its signature stamp.
 */
public class RegistrySignature {
	private final String signatureType = "RSASignature2018";
	private final String creator = "registry";
	private String createdTimestamp;
	private String nonce;

	public String getCreatedTimestamp() {
		return createdTimestamp;
	}

	public void setCreatedTimestamp(String createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}

	public String getNonce() {
		return nonce;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}
}
