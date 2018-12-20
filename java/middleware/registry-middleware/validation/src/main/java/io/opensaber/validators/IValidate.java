package io.opensaber.validators;

import io.opensaber.pojos.APIMessage;
import io.opensaber.registry.middleware.MiddlewareHaltException;

public interface IValidate {

	public boolean validate(String objString, String entityType) throws MiddlewareHaltException;
}
