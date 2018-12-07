package io.opensaber.registry.model;

public class DBConnectionInfo {
	
	private String shardId;
	private String uri;
	private String username;
	private String password;
	
	public String getShardId() {
		return shardId;
	}
	public void setShardId(String shardId) {
		this.shardId = shardId;
	}

	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
}
