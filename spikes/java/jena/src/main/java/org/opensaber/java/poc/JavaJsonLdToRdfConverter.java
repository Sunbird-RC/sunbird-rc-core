package org.opensaber.java.poc;

import java.io.FileInputStream;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * 
 * @author jyotsna
 * This class uses com.github.jsonld-java to convert json-ld to RDF
 *
 */
public class JavaJsonLdToRdfConverter {

	public static void main(String args[]){
		try{
			InputStream inputStream = new FileInputStream("testing/teacher.json");
			Object compact = JsonLdProcessor.toRDF(JsonUtils.fromInputStream(inputStream));
			System.out.println(JsonUtils.toPrettyString(compact));
		}catch(JsonGenerationException e){
			e.printStackTrace();
		}
	    catch(Exception e){
			e.printStackTrace();
		}
	
	}

}
