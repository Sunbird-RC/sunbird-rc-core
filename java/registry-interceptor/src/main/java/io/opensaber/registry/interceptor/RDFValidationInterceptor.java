package io.opensaber.registry.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.interceptor.handler.RequestWrapper;
import io.opensaber.registry.middleware.impl.RDFValidator;
import io.opensaber.registry.middleware.util.Constants;

@Component
public class RDFValidationInterceptor extends BaseRequestHandler implements HandlerInterceptor {
	
	private RDFValidator rdfValidator;

	@Autowired
	public RDFValidationInterceptor(RDFValidator rdfValidator){
		this.rdfValidator=rdfValidator;
	}

	@Override
	public boolean preHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2) throws Exception {
		setRequest(request);
		Map<String,Object> attributeMap = rdfValidator.execute(getRequestBodyMap());
		setRequestAttributes(attributeMap);
		request = getRequest();
		requestWrapper = new RequestWrapper(request);
		System.out.println("Request body after interceptor:"+requestWrapper.getBody());
		if(request.getAttribute(Constants.REQUEST_ATTRIBUTE_NAME)!=null){
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
