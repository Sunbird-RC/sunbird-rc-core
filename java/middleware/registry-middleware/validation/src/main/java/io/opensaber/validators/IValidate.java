package io.opensaber.validators;

import io.opensaber.registry.middleware.MiddlewareHaltException;

public interface IValidate {

	boolean validate(String entityType, String payload) throws MiddlewareHaltException;
}
