package io.opensaber.registry.middleware.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;


import io.opensaber.registry.middleware.impl.JsonldToRdfConverter;

/**
 * 
 * @author jyotsna
 *
 */
@Component
public class JsonldToRdfInterceptor implements HandlerInterceptor{
	
	private JsonldToRdfConverter jsonldToRdfConverter;
	
	@Autowired
	public JsonldToRdfInterceptor(JsonldToRdfConverter jsonldToRdfConverter){
		this.jsonldToRdfConverter=jsonldToRdfConverter;
	}
	
	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object object) throws Exception {
		jsonldToRdfConverter.execute(request,response);
		request = jsonldToRdfConverter.getRequest();
		if(request.getAttribute("actualRequestData")!=null){
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
