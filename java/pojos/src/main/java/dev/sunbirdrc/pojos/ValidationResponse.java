package dev.sunbirdrc.pojos;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

public class ValidationResponse extends BaseErrorResponse {

	@Expose(serialize = false)
	private boolean isValid;
	@Setter
    @Getter
    @Expose(serialize = false)
	private String error;
	@Setter
    @Getter
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

}
