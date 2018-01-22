package io.opensaber.registry.middleware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * 
 * @author jyotsna
 *
 */
public class RdfToJsonldConverter implements HandlerInterceptor{

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object object) throws Exception {
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request,HttpServletResponse response,
	  Object handler, ModelAndView modelAndView) throws Exception {
		String rdfStr = convertToJsonld(JsonldToRdfConverter.rdfVar);
		ResponseWrapper responseWrapper = new ResponseWrapper(response);
		responseWrapper = (ResponseWrapper)responseWrapper.writeResponseBody(responseWrapper, rdfStr);
		response = (HttpServletResponse)responseWrapper.getResponse();
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
	  Object handler, Exception ex) {

	}

	public String convertToJsonld(String str){
		String rdfStr = "";
		try{
			Object compact = JsonLdProcessor.fromRDF(str);
			rdfStr = JsonUtils.toPrettyString(compact);
		}catch(JsonGenerationException e){
			e.printStackTrace();
		}
	    catch(Exception e){
			e.printStackTrace();
		}
		return rdfStr;
	}
	
}
