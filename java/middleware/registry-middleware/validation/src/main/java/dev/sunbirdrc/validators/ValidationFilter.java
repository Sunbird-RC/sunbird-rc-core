package dev.sunbirdrc.validators;

import dev.sunbirdrc.pojos.APIMessage;
import dev.sunbirdrc.registry.middleware.Middleware;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;
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
		validationService.validate(entityType, payload, false);
		return true;
	}
}
