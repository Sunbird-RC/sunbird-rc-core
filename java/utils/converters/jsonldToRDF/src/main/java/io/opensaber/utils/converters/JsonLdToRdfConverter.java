package io.opensaber.utils.converters;

import java.io.InputStream;
import java.io.Reader;

import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;


public class JsonLdToRdfConverter {

	public static String jsonldToRDFConvertor(InputStream inputStream){
		String rdfStr = "";
		try{
			Object compact = JsonLdProcessor.toRDF(JsonUtils.fromInputStream(inputStream));
			rdfStr = JsonUtils.toPrettyString(compact);
		}
	    catch(Exception e){
			e.printStackTrace();
		}
		return rdfStr;
	}
	
	public static String jsonldToRDFConvertor(Reader reader){
		String rdfStr = "";
		try{
			Object compact = JsonLdProcessor.toRDF(JsonUtils.fromReader(reader));
			rdfStr = JsonUtils.toPrettyString(compact);
		}
	    catch(Exception e){
			e.printStackTrace();
		}
		return rdfStr;
	}
	
	public static String jsonldToRDFConvertor(String jsonString){
		String rdfStr = "";
		try{
			Object compact = JsonLdProcessor.toRDF(JsonUtils.fromString(jsonString));
			rdfStr = JsonUtils.toPrettyString(compact);
		}
	    catch(Exception e){
			e.printStackTrace();
		}
		return rdfStr;
	}
}
