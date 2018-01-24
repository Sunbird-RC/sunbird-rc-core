package io.opensaber.registry.middleware.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.util.Constants;

/**
 * 
 * @author jyotsna
 *
 */
public class RdfToJsonldConverter implements BaseMiddleware{

	@Override
	public Map<String,Object> execute(Map<String,Object> mapData) throws IOException {
		String responseData = mapData.get(Constants.ATTRIBUTE_NAME).toString();
		if(!StringUtils.isEmpty(responseData)){
			String rdfStr = convertToJsonld(responseData);
			if(!StringUtils.isEmpty(rdfStr)){
				mapData.put(Constants.ATTRIBUTE_NAME, rdfStr);
			}else{
				mapData.put(Constants.ATTRIBUTE_NAME, null);
			}
		}
		return mapData;
	}

	@Override
	public Map<String,Object> next(Map<String,Object> mapData) throws IOException {
		return new HashMap<String,Object>();
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
