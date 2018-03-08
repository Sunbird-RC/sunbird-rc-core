package io.opensaber.registry.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.UUID;



public class RegistryTestBase {

	
	public String jsonld;
	public static final String FORMAT = "JSON-LD";
	private static final String REPLACING_SUBJECT_LABEL = "<@id>";
	private static final String EMPTY_STRING = "";
	
	public void setJsonld(String filename){

		try {
			String file = Paths.get(getPath(filename)).toString();
			jsonld = readFromFile(file);			
		} catch (Exception e) {
			jsonld = EMPTY_STRING;
		}

	}

	public String readFromFile(String file) throws IOException,FileNotFoundException{
		BufferedReader reader = new BufferedReader(new FileReader (file));
		StringBuilder sb = new StringBuilder();
		try{
			String line = null;
			while((line = reader.readLine()) !=null){
				sb.append(line);
			}
		}catch(Exception e){
			return EMPTY_STRING;
		}finally{
			if(reader!=null){
				reader.close();
			}
		}
		return sb.toString();
	}

	public URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

	public String generateBaseUrl(){
		return Constants.INTEGRATION_TEST_BASE_URL;
	}

	public void setJsonldWithNewRootLabel(String id){
		jsonld.replace(REPLACING_SUBJECT_LABEL, id);
	}
	
	
	
	public static String generateRandomId(){
		return UUID.randomUUID().toString();
	}

}
