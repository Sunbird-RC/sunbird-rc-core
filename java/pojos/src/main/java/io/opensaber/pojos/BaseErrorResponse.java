package io.opensaber.pojos;

public class BaseErrorResponse {
	private String type;

	public BaseErrorResponse(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
