package io.opensaber.registry.factory;

import io.opensaber.registry.exception.ValidationFactoryException;
import io.opensaber.registry.exception.errorconstants.ErrorConstants;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RdfValidationServiceImpl;
import io.opensaber.registry.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValidateFactory {

    @Autowired
    private RdfValidationServiceImpl validationServiceImpl;

    public ValidationService getInstance(String type) throws ValidationFactoryException {
        ValidationService validationService = null;

        switch (type) {

            case Constants.ENABLE_RDF_VALIDATION:
                validationService = validationServiceImpl;
                break;
             //provided for json validation
            case Constants.ENABLE_JSON_VALIDATION:

                break;

            default:

                throw new ValidationFactoryException(ErrorConstants.VALIDATION_IMPLEMENTATION_NOT_PROVIDED);

        }
        return validationService;
    }


}
