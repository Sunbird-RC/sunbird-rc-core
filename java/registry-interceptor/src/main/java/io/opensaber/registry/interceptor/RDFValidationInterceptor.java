package io.opensaber.registry.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.interceptor.handler.RequestWrapper;
import io.opensaber.registry.middleware.impl.RDFConverter;
import io.opensaber.registry.middleware.impl.RDFValidator;
import io.opensaber.registry.middleware.util.Constants;

@Order(2)
@Component
public class RDFValidationInterceptor extends BaseRequestHandler implements HandlerInterceptor {
	
	private RDFValidator rdfValidator;
	
	@Autowired
	public RDFValidationInterceptor(RDFValidator rdfValidator){
		this.rdfValidator = rdfValidator;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object arg2) throws Exception {
		setRequest(request);
		Map<String,Object> attributeMap = rdfValidator.execute(getRequestAttributeMap());
		mergeRequestAttributes(attributeMap);
		request = getRequest();
		if(request.getAttribute(Constants.RDF_VALIDATION_OBJECT)!=null){
			return true;
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
