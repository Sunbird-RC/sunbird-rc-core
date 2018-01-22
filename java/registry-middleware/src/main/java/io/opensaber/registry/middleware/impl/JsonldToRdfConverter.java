package io.opensaber.registry.middleware.impl;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.handler.BaseRequestHandler;
import io.opensaber.registry.middleware.util.Constants;

/**
 * 
 * @author jyotsna
 *
 */
public class JsonldToRdfConverter extends BaseRequestHandler implements BaseMiddleware{

	public static String rdfVar = "";

	@Override
	public void execute(HttpServletRequest request,HttpServletResponse response) throws IOException{
		setRequest(request);
		getRdf();
	}
	
	@Override
	public void next(HttpServletRequest request, HttpServletResponse response) throws IOException{
		
	}

	public void getRdf() throws IOException{
		String requestBody = getRequestBody();
		if(!StringUtils.isEmpty(requestBody)){
			rdfVar = convertToRdf(requestBody);
			Map<String,Object> attributeMap = new HashMap<String,Object>();
			attributeMap.put(Constants.REQUEST_ATTRIBUTE_NAME, rdfVar);
			setRequestAttributes(attributeMap);
		}
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
