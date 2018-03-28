package io.opensaber.registry.service.impl;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.schema.config.SchemaConfigurator;
import io.opensaber.registry.service.EncryptionService;

@Component
public class EncryptionServiceImpl implements EncryptionService {
	
	@Value("${encryption.uri}")
	private String encryptionUri;

	@Value("${decryption.uri}")
	private String decryptionUri;

	@Value("${encryption.decryption.base}")
	private String encryptionServiceHealthCheckUri;
	
	@Autowired
	SchemaConfigurator schemaConfigurator;
	
	@Autowired
	RestTemplate restTemplate;
	
	private static Logger logger = LoggerFactory.getLogger(EncryptionServiceImpl.class);
	
	@Override
	public String encrypt(Object propertyValue) throws EncryptionException {
		
		MultiValueMap<String, Object> map= new LinkedMultiValueMap<String, Object>();
		map.add("value", propertyValue);
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map);
		ResponseEntity<String> response=null;
		try {
		    response = restTemplate.postForEntity(encryptionUri, request, String.class);
		   	return response.getBody();
		}catch(ResourceAccessException e) {
			logger.error("Exception while connecting enryption service : ", e);
			throw new EncryptionException("Exception while connecting enryption service! ");
		}catch(ServiceUnavailableException e) {
	    	logger.error("Service not available exception !: ", e);
	  		throw new EncryptionException("Encryption service is not available !");
		}catch(Exception e) {
	    	logger.error("Exception in encryption servie !: ", e);
	    	throw new EncryptionException("Exception in encryption service ! ");
	    }	    
	}
	
	@Override
     public String decrypt(Object propertyValue) throws EncryptionException {		
						
		MultiValueMap<String, Object> map= new LinkedMultiValueMap<String, Object>();
		map.add("value", propertyValue);
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map);
		ResponseEntity<String> response=null;
		try {
			response = restTemplate.postForEntity(decryptionUri, request, String.class);
			return response.getBody();
		}catch(ResourceAccessException e) {
	    	logger.error("Exception while connecting dcryption service : ", e);
	    	throw new EncryptionException("Exception while connecting enryption service ! ");
		}catch(ServiceUnavailableException e) {
	    	logger.error("Service not available exception ! ", e);
	    	throw new EncryptionException("Encryption service is not available !");
		}catch(Exception e) {
	    	logger.error("Exception in decryption service !: ", e);
	       	throw new EncryptionException("Exception in encryption service ! ");
		}   
	}
	
    public boolean isEncryptable(String propertyKey) throws EncryptionException {    
    	if(propertyKey!=null) {
    		return schemaConfigurator.isPrivate(propertyKey);
    	}else
    		return false;
    }
    
    public boolean isDecryptable(String tailPropertyKey) throws EncryptionException {    
    	if(tailPropertyKey!=null) {
    		return tailPropertyKey.substring(0, Math.min(tailPropertyKey.length(), 9)).equalsIgnoreCase("encrypted");    
    	}else
    		return false;
    }

	/**
	 * This method is used to check if the sunbird encryption service is up
	 * @return
	 */
	@Override
	public boolean isEncryptionServiceUp() {
		boolean isEncryptionServiceUp = false;
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(encryptionServiceHealthCheckUri, String.class);
			if (response.getBody().equalsIgnoreCase("UP")) {
				isEncryptionServiceUp = true;
			}
		} catch (RestClientException ex) {
			logger.error("RestClientException when checking the health of the Sunbird encryption service: ", ex);
		}
		return isEncryptionServiceUp;
	}

}