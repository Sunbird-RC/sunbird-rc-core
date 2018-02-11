/*package io.opensaber.registry.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.interceptor.handler.RequestWrapper;
import io.opensaber.registry.middleware.impl.JsonldToRdfConverter;
import io.opensaber.registry.middleware.util.Constants;


*//**
 * 
 * @author jyotsna
 *
 *//*
@Component
public class JsonldToRdfInterceptor extends BaseRequestHandler implements HandlerInterceptor{
	
	private JsonldToRdfConverter jsonldToRdfConverter;
	
	@Autowired
	public JsonldToRdfInterceptor(JsonldToRdfConverter jsonldToRdfConverter){
		this.jsonldToRdfConverter=jsonldToRdfConverter;
	}
	
	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object object) throws Exception {
		setRequest(request);
		Map<String,Object> attributeMap = jsonldToRdfConverter.execute(getRequestBodyMap());
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
	public void postHandle(HttpServletRequest request,HttpServletResponse response,
	  Object handler, ModelAndView modelAndView) throws Exception {
		
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
	  Object handler, Exception ex) {

	}

	
}
*/