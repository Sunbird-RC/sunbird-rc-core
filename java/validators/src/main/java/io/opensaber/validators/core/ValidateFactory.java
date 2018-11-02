package io.opensaber.validators.core;

import org.springframework.beans.factory.annotation.Autowired;

import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.validators.exception.ErrorConstants;
import io.opensaber.validators.exception.ValidationFactoryException;
import io.opensaber.validators.rdf.shex.RdfValidationServiceImpl;

//@Component
public class ValidateFactory {

	@Autowired
	private RdfValidationServiceImpl rdfValidationServiceImpl; 
	
    public ValidationService getInstance(String type) throws ValidationFactoryException {
        ValidationService validationService = null;

        switch (type) {

            case Constants.ENABLE_RDF_VALIDATION:
                validationService = (ValidationService) rdfValidationServiceImpl;
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
