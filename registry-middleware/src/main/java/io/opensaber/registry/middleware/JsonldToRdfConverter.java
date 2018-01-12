package io.opensaber.registry.middleware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * 
 * @author jyotsna
 *
 */
public class JsonldToRdfConverter implements HandlerInterceptor{
	
	public static String rdfVar = "";

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object object) throws Exception {
		RequestWrapper request1 = new RequestWrapper((HttpServletRequest) request);
		String requestBody = request1.getBody();
		if(!StringUtils.isEmpty(requestBody)){
			String rdfStr = convertToRdf(requestBody);
			request.setAttribute("entity", rdfStr);
			rdfVar=rdfStr;
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request,HttpServletResponse response,
	  Object handler, ModelAndView modelAndView) throws Exception {
		
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
	  Object handler, Exception ex) {

	}

	public String convertToRdf(String body){
		String rdfStr = "";
		try{
			JsonLdOptions options = new JsonLdOptions();
			options.format = JsonLdConsts.APPLICATION_NQUADS;
			Object compact = JsonLdProcessor.toRDF(JsonUtils.fromString(body),options);
			rdfStr = compact.toString();
		}catch(JsonGenerationException e){
			e.printStackTrace();
		}
	    catch(Exception e){
			e.printStackTrace();
		}
		return rdfStr;
	}
	
}
