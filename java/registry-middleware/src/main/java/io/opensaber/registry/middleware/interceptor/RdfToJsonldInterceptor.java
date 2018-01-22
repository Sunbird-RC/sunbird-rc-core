package io.opensaber.registry.middleware.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import io.opensaber.registry.middleware.handler.ResponseWrapper;
import io.opensaber.registry.middleware.impl.JsonldToRdfConverter;
import io.opensaber.registry.middleware.impl.RdfToJsonldConverter;

/**
 * 
 * @author jyotsna
 *
 */
@Component
public class RdfToJsonldInterceptor implements HandlerInterceptor{
	
	private RdfToJsonldConverter rdfToJsonldConverter;
	
	@Autowired
	public RdfToJsonldInterceptor(RdfToJsonldConverter rdfToJsonldConverter){
		this.rdfToJsonldConverter = rdfToJsonldConverter;
	}

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object object) throws Exception {
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request,HttpServletResponse response,
	  Object handler, ModelAndView modelAndView) throws Exception {
		rdfToJsonldConverter.execute(request, response);
		response = rdfToJsonldConverter.getResponse();
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
	  Object handler, Exception ex) {

	}

}
