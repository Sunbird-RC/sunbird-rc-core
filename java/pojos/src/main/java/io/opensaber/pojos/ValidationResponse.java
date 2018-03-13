package io.opensaber.pojos;

import java.util.List;

public class ValidationResponse {
	
	private boolean isValid;
	private List<String> error;
	public boolean isValid() {
		return isValid;
	}
	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}
	public List<String> getError() {
		return error;
	}
	public void setError(List<String> error) {
		this.error = error;
	}

}
