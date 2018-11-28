package io.opensaber.validators;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.opensaber.pojos.APIMessage;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;

@Component
public class ValidationFilter implements Middleware {
	private IValidate validationService;

	@Autowired
	private APIMessage apiMessage;

	public ValidationFilter(IValidate validationServiceImpl) {
		this.validationService = validationServiceImpl;
	}

	@Override
	public boolean execute(APIMessage apiMessage) throws MiddlewareHaltException {
		validationService.validate(apiMessage);
		return true;
	}
}
