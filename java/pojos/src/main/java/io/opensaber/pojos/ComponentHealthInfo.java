package io.opensaber.pojos;

public class ComponentHealthInfo {

	private String name;
	private boolean healthy;
	private String err;
	private String errmsg;

	public ComponentHealthInfo(String name, boolean healthy) {
		this.name = name;
		this.healthy = healthy;
		this.err = "";
		this.errmsg = "";
	}

	public ComponentHealthInfo(String name, boolean healthy, String err, String errmsg) {
		this.name = name;
		this.healthy = healthy;
		this.err = err;
		this.errmsg = errmsg;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isHealthy() {
		return healthy;
	}

	public void setHealthy(boolean healthy) {
		this.healthy = healthy;
	}

	public String getErr() {
		return err;
	}

	public void setErr(String err) {
		this.err = err;
	}

	public String getErrmsg() {
		return errmsg;
	}

	public void setErrmsg(String errmsg) {
		this.errmsg = errmsg;
	}
}
