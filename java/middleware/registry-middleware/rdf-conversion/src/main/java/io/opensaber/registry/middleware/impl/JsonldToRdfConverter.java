/*package io.opensaber.registry.middleware.impl;


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

*//**
 * 
 * @author jyotsna
 *
 *//*
public class JsonldToRdfConverter implements BaseMiddleware{

	//public static String rdfVar = "";

	public Map<String,Object> execute(Map<String,Object> mapData) throws IOException{
		return getRdf(mapData.get(Constants.ATTRIBUTE_NAME));
	}
	
	public Map<String,Object> next(Map<String,Object> mapData) throws IOException{
		return  new HashMap<String,Object>();
		
	}

	public Map<String,Object> getRdf(Object obj) throws IOException{
		Map<String,Object> attributeMap = new HashMap<String,Object>();
		if(!StringUtils.isEmpty(obj.toString())){
			String rdf = convertToRdf(obj.toString());
			
			//attributeMap.put(Constants.ATTRIBUTE_NAME, rdfModel);
		}
		return attributeMap;
		
	}


	public String convertToRdf(String body){
		String rdf = null;
		try{
			JsonLdOptions options = new JsonLdOptions();
			options.format = JsonLdConsts.APPLICATION_NQUADS;
			//final JsonLdTripleCallback callback = new RDF4JJSONLDTripleCallback(Rio.createWriter(sesameOutputFormat, System.out));
			//final JenaTripleCallback callback = new JenaTripleCallback();
			Object rdfObj = JsonLdProcessor.toRDF(JsonUtils.fromString(body), options);
			System.out.println(rdfObj.toString());
			return rdfObj.toString();
		}catch(JsonGenerationException e){
			e.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return rdf;
	}
	
	public static void main(String args[]){
		JsonldToRdfConverter jsonldToRdfConverter= new JsonldToRdfConverter();
		jsonldToRdfConverter.convertToRdf(getJsonString());
	}
	
	public static String getJsonString(){
		return "{\"@context\": {\"schema\": \"http:schema.org/\",\"opensaber\": \"http:open-saber.org/vocab/core/#\"},\"@type\": "
				+ "[\"schema:Person\",\"opensabre:Teacher\"],\"schema:identifier\": \"b6ad2941-fac3-4c72-94b7-eb638538f55f\",\"schema:image\": null,"
				+ "\"schema:nationality\": \"Indian\",\"schema:birthDate\": \"2011-12-06\",\"schema:name\": \"Marvin\",\"schema:gender\": \"male\","
				+ "\"schema:familyName\":\"Pande\",\"opensaber:languagesKnownISO\": [\"en\",\"hi\"]}";
	}

}
*/