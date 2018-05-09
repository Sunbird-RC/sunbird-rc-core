package io.opensaber.registry.interceptor;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.opensaber.pojos.ValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.Gson;

import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.impl.RDFValidator;
import io.opensaber.registry.middleware.util.Constants;

@Component
public class RDFValidationInterceptor extends BaseRequestHandler implements HandlerInterceptor{

	private static Logger logger = LoggerFactory.getLogger(RDFValidationInterceptor.class);
	private RDFValidator rdfValidator;
	private Gson gson;
	
	@Autowired
	public RDFValidationInterceptor(RDFValidator rdfValidator, Gson gson){
		this.rdfValidator = rdfValidator;
		this.gson = gson;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object arg2) throws IOException, MiddlewareHaltException {
		try {
			setRequest(request);
			Map<String, Object> attributeMap = rdfValidator.execute(getRequestAttributeMap());
			mergeRequestAttributes(attributeMap);
			request = getRequest();
			ValidationResponse validationResponse = (ValidationResponse) request.getAttribute(Constants.RDF_VALIDATION_OBJECT);
			if (validationResponse != null && validationResponse.isValid()) {
				logger.info("RDF Validated successfully !");
				return true;
			} else {
				logger.info("RDF Validation failed!");
				setResponse(response);
				writeResponseObj(validationResponse.getError(), validationResponse);
				response = getResponse();
			}
		} catch (MiddlewareHaltException e) {
			logger.error("MiddlewareHaltException from RDFValidationInterceptor: ", e);
			setResponse(response);
			writeResponseObj(gson, e.getMessage());
			response = getResponse();
		} catch (Exception e) {
			logger.error("Exception from RDFValidationInterceptor: ", e);
			setResponse(response);
			writeResponseObj(gson, Constants.RDF_VALIDATION_ERROR);
			response = getResponse();
		}
		return false;
	}
	
	@Override
	public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3)
			throws Exception {
	}

	@Override
	public void postHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, ModelAndView arg3)
			throws Exception {
		// TODO Auto-generated method stub

	} 
	
}
