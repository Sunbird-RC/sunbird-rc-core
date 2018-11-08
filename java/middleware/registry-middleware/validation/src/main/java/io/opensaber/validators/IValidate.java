package io.opensaber.validators;

import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.MiddlewareHaltException;

public interface IValidate {

	public ValidationResponse validate(Object input, String methodOrigin) throws MiddlewareHaltException;
}
