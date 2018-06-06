package io.opensaber.registry.service.impl;

import java.util.HashMap;
import java.util.Map;

import io.opensaber.pojos.OpenSaberInstrumentation;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.schema.config.SchemaConfigurator;
import io.opensaber.registry.service.EncryptionService;

@Component
public class EncryptionServiceImpl implements EncryptionService {
	
	@Value("${encryption.uri}")
	private String encryptionUri;

	@Value("${decryption.uri}")
	private String decryptionUri;
	
	@Value("${encryption.batch.uri}")
	private String encryptionBatchUri;

	@Value("${decryption.batch.uri}")
	private String decryptionBatchUri;

	@Value("${encryption.base}")
	private String encryptionServiceHealthCheckUri;

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private Gson gson;
	
	@Autowired
	SchemaConfigurator schemaConfigurator;

	@Autowired
	private OpenSaberInstrumentation watch;
		
	private static Logger logger = LoggerFactory.getLogger(EncryptionServiceImpl.class);
	
	@Override
	public String encrypt(Object propertyValue) throws EncryptionException {
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("value", propertyValue);
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(map);
		try {
			ResponseEntity<String> response = new RestTemplate().postForEntity(encryptionUri, request, String.class);
		   	return response.getBody();
		}catch(ResourceAccessException e) {
			logger.error("ResourceAccessException while connecting enryption service : ", e);
			throw new EncryptionException("Exception while connecting enryption service! ");
		}catch(ServiceUnavailableException e) {
	    	logger.error("ServiceUnavailableException while connecting enryption service!: ", e);
	  		throw new EncryptionException("Encryption service is not available !");
		}catch(Exception e) {
	    	logger.error("Exception in encryption servie !: ", e);
	    	throw new EncryptionException("Exception in encryption service ! ");
	    }	    
	}

	@Override
	public String decrypt(Object propertyValue) throws EncryptionException {

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("value", propertyValue);
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(map);
		try {
			ResponseEntity<String> response = restTemplate.postForEntity(decryptionUri, request, String.class);
			logger.info("Property decrypted successfully !");
			return response.getBody();
		} catch (ResourceAccessException e) {
			logger.error("ResourceAccessException while connecting dcryption service : ", e);
			throw new EncryptionException("Exception while connecting enryption service ! ");
		} catch (ServiceUnavailableException e) {
			logger.error("ServiceUnavailableException while connecting enryption service!", e);
			throw new EncryptionException("Encryption service is not available !");
		} catch (Exception e) {
			logger.error("Exception in decryption service !: ", e);
			throw new EncryptionException("Exception in encryption service ! ");
		}
	}

	@Override
	public Map<String, Object> encrypt(Map<String, Object> propertyValue) throws EncryptionException {
		Map<String, Object> map = new HashMap<>();
		map.put("value", propertyValue);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(gson.toJson(map), headers);
		try {
			watch.start("EncryptionServiceImpl.encryptBatch");
			ResponseEntity<String> response = restTemplate.postForEntity(encryptionBatchUri, entity, String.class);
			watch.stop("EncryptionServiceImpl.encryptBatch");
			return gson.fromJson(response.getBody(), new TypeToken<HashMap<String, Object>>() {}.getType());
		} catch (ResourceAccessException e) {
			logger.error("Exception while connecting enryption service : ", e);
			throw new EncryptionException("Exception while connecting enryption service! ");
		} catch (ServiceUnavailableException e) {
			logger.error("Service not available exception !: ", e);
			throw new EncryptionException("Encryption service is not available !");
		} catch (Exception e) {
			logger.error("Exception in encryption servie !: ", e);
			throw new EncryptionException("Exception in encryption service ! ");
		}
	}

	@Override
	public Map<String, Object> decrypt(Map<String, Object> propertyValue) throws EncryptionException {
		Map<String, Object> map = new HashMap<>();
		map.put("value", propertyValue);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(gson.toJson(map), headers);

		try {
			watch.start("EncryptionServiceImpl.decryptBatch");
			ResponseEntity<String> response = restTemplate.postForEntity(decryptionBatchUri, entity, String.class);
			watch.stop("EncryptionServiceImpl.decryptBatch");
			return gson.fromJson(response.getBody(), new TypeToken<HashMap<String, Object>>() {}.getType());
		} catch (ResourceAccessException e) {
			logger.error("Exception while connecting dcryption service : ", e);
			throw new EncryptionException("Exception while connecting enryption service ! ");
		} catch (ServiceUnavailableException e) {
			logger.error("Service not available exception ! ", e);
			throw new EncryptionException("Encryption service is not available !");
		} catch (Exception e) {
			logger.error("Exception in decryption service !: ", e);
			throw new EncryptionException("Exception in encryption service ! ");
		}
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
				logger.debug("Encryption service running !");
			}
		} catch (RestClientException ex) {
			logger.error("RestClientException when checking the health of the Sunbird encryption service: ", ex);
		}
		return isEncryptionServiceUp;
	}
}