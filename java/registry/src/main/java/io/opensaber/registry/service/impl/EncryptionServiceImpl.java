package io.opensaber.registry.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Component
public class EncryptionServiceImpl implements EncryptionService {

	private static Logger logger = LoggerFactory.getLogger(EncryptionServiceImpl.class);
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
	private RetryRestTemplate retryRestTemplate;

	@Autowired
	private Gson gson;
	@Autowired
	private OpenSaberInstrumentation watch;


	/** encrypts the input
	 * @param propertyValue - single value or object as input for encryption
	 * @return - encrypted value
	 * @throws EncryptionException
	 */
	@Override
	public String encrypt(Object propertyValue) throws EncryptionException {
		logger.debug("encrypt starts with value");
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("value", propertyValue);
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(map);
		try {
			ResponseEntity<String> response = retryRestTemplate.postForEntity(encryptionUri, request);
			return response.getBody();
		} catch (ResourceAccessException e) {
			logger.error("ResourceAccessException while connecting enryption service : ", e);
			throw new EncryptionException("Exception while connecting enryption service! ");
		} catch (Exception e) {
			logger.error("Exception in encryption service !: ", e);
			throw new EncryptionException("Exception in encryption service");
		}
	}

	/** decrypts the input
	 * @param propertyValue - single value or object as input for decryption
	 * @return - decrypted value
	 * @throws EncryptionException
	 */
	@Override
	public String decrypt(Object propertyValue) throws EncryptionException {
		logger.debug("decrypt starts with value {}", propertyValue);
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("value", propertyValue);
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(map);
		try {
			ResponseEntity<String> response = retryRestTemplate.postForEntity(decryptionUri,request);
			logger.info("Property decrypted successfully !");
			return response.getBody();
		} catch (ResourceAccessException e) {
			logger.error("ResourceAccessException while connecting dcryption service : ", e);
			throw new EncryptionException("Exception while connecting enryption service ! ");
		} catch (Exception e) {
			logger.error("Exception in decryption service !: ", e);
			throw new EncryptionException("Exception in encryption service ! ");
		}
	}

	/** encrypts the input which is in Map format
	 * @param propertyValue - input is in format Map<String, Object>
	 * @return Map<String, Object>
	 * @throws EncryptionException
	 */
	@Override
	public Map<String, Object> encrypt(Map<String, Object> propertyValue) throws EncryptionException {
		logger.debug("encrypt starts with value {}", propertyValue);
		Map<String, Object> map = new HashMap<>();
		map.put("value", propertyValue);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(gson.toJson(map), headers);
		try {
			watch.start("EncryptionServiceImpl.encryptBatch");
			ResponseEntity<String> response = retryRestTemplate.postForEntity(encryptionBatchUri,entity);
			watch.stop("EncryptionServiceImpl.encryptBatch");
			return gson.fromJson(response.getBody(), new TypeToken<HashMap<String, Object>>() {
			}.getType());
		} catch (ResourceAccessException e) {
			logger.error("Exception while connecting enryption service : ", e);
			throw new EncryptionException("Exception while connecting enryption service! ");
		} catch (Exception e) {
			logger.error("Exception in encryption servie !: ", e);
			throw new EncryptionException("Exception in encryption service.");
		}
	}

	/** decrypts the input which is in Map format
	 * @param propertyValue - input is in format Map<String, Object>
	 * @return Map<String, Object>
	 * @throws EncryptionException
	 */
	@Override
	public Map<String, Object> decrypt(Map<String, Object> propertyValue) throws EncryptionException {
		logger.debug("decrypt starts with value {}", propertyValue);
		Map<String, Object> map = new HashMap<>();
		map.put("value", propertyValue);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(gson.toJson(map), headers);

		try {
			watch.start("EncryptionServiceImpl.decryptBatch");
			ResponseEntity<String> response = retryRestTemplate.postForEntity(decryptionBatchUri,entity);
			watch.stop("EncryptionServiceImpl.decryptBatch");
			return gson.fromJson(response.getBody(), new TypeToken<HashMap<String, Object>>() {
			}.getType());
		} catch (ResourceAccessException e) {
			logger.error("Exception while connecting dcryption service : ", e);
			throw new EncryptionException("Exception while connecting enryption service ! ");
		} catch (Exception e) {
			logger.error("Exception in decryption service !: ", e);
			throw new EncryptionException("Exception in encryption service ! ");
		}
	}

	/**
	 * This method is used to check if the sunbird encryption service is up
	 * 
	 * @return boolean true/false
	 */
	@Override
	public boolean isEncryptionServiceUp() {
		boolean isEncryptionServiceUp = false;
		try {
			ResponseEntity<String> response = retryRestTemplate.getForEntity(encryptionServiceHealthCheckUri);
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