package io.opensaber.registry.middleware.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.handler.BaseResponseHandler;

/**
 * 
 * @author jyotsna
 *
 */
public class RdfToJsonldConverter extends BaseResponseHandler implements BaseMiddleware{

	@Override
	public void execute(HttpServletRequest request, HttpServletResponse response) throws IOException {
		setResponse(response);
		String responseData = getResponseContent();
		if(!StringUtils.isEmpty(responseData)){
			String rdfStr = convertToJsonld(responseData);
			if(!StringUtils.isEmpty(rdfStr)){
				writeResponseBody(rdfStr);
			}
		}
	}

	@Override
	public void next(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
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
