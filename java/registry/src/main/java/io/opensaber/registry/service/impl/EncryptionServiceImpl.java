package io.opensaber.registry.service.impl;

import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.fields.configuration.FieldConfiguration;
import io.opensaber.registry.service.EncryptionService;

@Component
public class EncryptionServiceImpl implements EncryptionService {
	
	@Value("${encryption.uri}")
	private String encryptionUri;

	@Value("${decryption.uri}")
	private String decryptionUri;
	
	@Autowired
	FieldConfiguration fieldConfiguration;
	
	private static Logger logger = LoggerFactory.getLogger(EncryptionServiceImpl.class);
	
	@Override
	public ResponseEntity<String> encrypt(Object propertyValue) throws EncryptionException {
		
		RestTemplate restTemplate = new RestTemplate();
	    ResponseEntity<String> response = restTemplate.postForEntity(encryptionUri, propertyValue, String.class);
	    return response;
	    
	}
	
	@Override
     public ResponseEntity<String> decrypt(Object propertyValue) throws EncryptionException {
		
	    RestTemplate restTemplate = new RestTemplate();
	    ResponseEntity<String> response = restTemplate.postForEntity(decryptionUri, propertyValue, String.class);
	  	    
	    return response;
	    
	}
	
    public boolean encryptionRequired(VertexProperty<Object> property) throws EncryptionException {
    
    	System.out.println("----Return from fieldConfiguration : ----- "+fieldConfiguration.getPrivacyForField(property.key()));
     	return fieldConfiguration.getPrivacyForField(property.key());
    }


}