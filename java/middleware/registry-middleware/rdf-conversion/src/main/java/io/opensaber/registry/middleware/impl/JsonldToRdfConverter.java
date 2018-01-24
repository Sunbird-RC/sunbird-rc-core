package io.opensaber.registry.middleware.impl;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.util.Constants;

/**
 * 
 * @author jyotsna
 *
 */
public class JsonldToRdfConverter implements BaseMiddleware{

	public static String rdfVar = "";

	@Override
	public Map<String,Object> execute(Map<String,Object> mapData) throws IOException{
		return getRdf(mapData.get(Constants.ATTRIBUTE_NAME));
	}
	
	@Override
	public Map<String,Object> next(Map<String,Object> mapData) throws IOException{
		return  new HashMap<String,Object>();
		
	}

	public Map<String,Object> getRdf(Object obj) throws IOException{
		Map<String,Object> attributeMap = new HashMap<String,Object>();
		if(!StringUtils.isEmpty(obj.toString())){
			rdfVar = convertToRdf(obj.toString());
			System.out.println(rdfVar);
			attributeMap.put(Constants.ATTRIBUTE_NAME, rdfVar);
		}
		return attributeMap;
		
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
