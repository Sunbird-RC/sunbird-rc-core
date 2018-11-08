package io.opensaber.validators.json;

import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.validators.IValidate;

public class JsonValidationServiceImpl implements IValidate {

	@Override
	public ValidationResponse validate(Object input, String methodOrigin) throws MiddlewareHaltException {
		return null;
	}
}
