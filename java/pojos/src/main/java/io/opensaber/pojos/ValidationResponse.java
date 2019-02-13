package io.opensaber.pojos;

import com.google.gson.annotations.Expose;

import java.util.HashMap;

public class ValidationResponse extends BaseErrorResponse {

	@Expose(serialize = false)
	private boolean isValid;
	@Expose(serialize = false)
	private String error;
	private HashMap<String, String> fields;

	public ValidationResponse(String type) {
		super(type);
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public HashMap<String, String> getFields() {
		return fields;
	}

	public void setFields(HashMap<String, String> fields) {
		this.fields = fields;
	}
}
