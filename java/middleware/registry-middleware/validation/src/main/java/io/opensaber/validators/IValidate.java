package io.opensaber.validators;

import io.opensaber.pojos.APIMessage;
import io.opensaber.registry.middleware.MiddlewareHaltException;

public interface IValidate {

	boolean validate(APIMessage apiMessage) throws MiddlewareHaltException;
}
