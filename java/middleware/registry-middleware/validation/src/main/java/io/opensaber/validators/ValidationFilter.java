package io.opensaber.validators;

import io.opensaber.pojos.APIMessage;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import org.springframework.stereotype.Component;

@Component
public class ValidationFilter implements Middleware {
	private static final String VALIDATION_FAILURE_MSG = "Validation failed";
	private IValidate validationService;

	public ValidationFilter(IValidate validationServiceImpl) {
		this.validationService = validationServiceImpl;
	}

	@Override
	public boolean execute(APIMessage apiMessage) throws MiddlewareHaltException {
		String entityType = apiMessage.getRequest().getEntityType();
		String payload = apiMessage.getRequest().getRequestMapAsString();
		if (!validationService.validate(entityType, payload)) {
			throw new MiddlewareHaltException(VALIDATION_FAILURE_MSG);
		}
		return true;
	}
}
