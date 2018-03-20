package io.opensaber.registry.service.impl;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
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
	
	@Autowired
	SchemaConfigurator schemaConfigurator;
	
	private static Logger logger = LoggerFactory.getLogger(EncryptionServiceImpl.class);
	
	@Override
	public ResponseEntity<String> encrypt(Object propertyValue) throws EncryptionException {
		
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		requestFactory.setConnectTimeout(5000);
		requestFactory.setConnectionRequestTimeout(5000);
		requestFactory.setReadTimeout(5000);
		
		MultiValueMap<String, Object> map= new LinkedMultiValueMap<String, Object>();
		map.add("value", propertyValue);
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map);
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		try {
		    ResponseEntity<String> response = restTemplate.postForEntity(encryptionUri, request, String.class);
		    return response;
		}catch(ResourceAccessException e) {
			logger.error("Exception while connecting enryption service : ", e);
			return null;
		}catch(ServiceUnavailableException e) {
	    	logger.error("Service not available exception !: ", e);
	    	return null;
		}catch(Exception e) {
	    	logger.error("Exception in encryption service !: ", e);
	    	return null;
	    }	    
	}
	
	@Override
     public ResponseEntity<String> decrypt(Object propertyValue) throws EncryptionException {		
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		requestFactory.setConnectTimeout(5000);
		requestFactory.setConnectionRequestTimeout(5000);
		requestFactory.setReadTimeout(5000);
	
		MultiValueMap<String, Object> map= new LinkedMultiValueMap<String, Object>();
		map.add("value", propertyValue);
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map);
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		try {
			ResponseEntity<String> response = restTemplate.postForEntity(decryptionUri, request, String.class);
			return response;
		}catch(ResourceAccessException e) {
	    	logger.error("Exception while connecting dcryption service : ", e);
			return null;
		}catch(ServiceUnavailableException e) {
	    	logger.error("Service not available exception !: ", e);
	    	return null;
		}catch(Exception e) {
	    	logger.error("Exception in decryption service !: ", e);
	    	return null;
		}   
	}
	
    public boolean encryptionRequired(VertexProperty<Object> property) throws EncryptionException {    
    	logger.info("----Return from fieldConfiguration : ----- "+schemaConfigurator.isPrivate(property.key()));
     	return schemaConfigurator.isPrivate(property.key());
    }
}