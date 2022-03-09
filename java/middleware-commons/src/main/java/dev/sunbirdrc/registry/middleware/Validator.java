package dev.sunbirdrc.registry.middleware;

import dev.sunbirdrc.pojos.ValidationResponse;

public interface Validator {

	/**
	 * This method can be implemented to custom validate data
	 *
	 * @return ValidationResponse object which contains the validation status and
	 *         information on the errors if present during validation
	 */
	ValidationResponse validate();

}
