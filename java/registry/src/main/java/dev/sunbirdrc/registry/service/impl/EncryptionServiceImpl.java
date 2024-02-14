package dev.sunbirdrc.registry.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.registry.exception.EncryptionException;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.service.EncryptionService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.*;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_ENCRYPTION_SERVICE_NAME;

@Component
@ConditionalOnProperty(name = "encryption.enabled", havingValue = "true")
public class EncryptionServiceImpl implements EncryptionService {

	private static Logger logger = LoggerFactory.getLogger(EncryptionServiceImpl.class);
	@Value("${encryption.enabled}")
	private boolean encryptionEnabled;
	@Value("${encryption.tenant.id}")
	private String encryptionTenantId;
	@Value("${encryption.method}")
	private String encryptionMethod;
	@Value("${encryption.uri}")
	private String encryptionUri;
	@Value("${decryption.uri}")
	private String decryptionUri;
	@Value("${encryption.batch.uri}")
	private String encryptionBatchUri;
	@Value("${decryption.batch.uri}")
	private String decryptionBatchUri;
	@Value("${encryption.healthCheckURL}")
	private String encryptionServiceHealthCheckUri;
	@Autowired
	private RetryRestTemplate retryRestTemplate;

	@Autowired
	private Gson gson;
	@Autowired
	private SunbirdRCInstrumentation watch;


	/** encrypts the input
	 * @param propertyValue - single value or object as input for encryption
	 * @return - encrypted value
	 * @throws EncryptionException
	 */
	@Override
	public String encrypt(Object propertyValue) throws EncryptionException {
		return this.doEncrypt(propertyValue, encryptionUri).toString();
	}

	/** decrypts the input
	 * @param propertyValue - single value or object as input for decryption
	 * @return - decrypted value
	 * @throws EncryptionException
	 */
	@Override
	public String decrypt(Object propertyValue) throws EncryptionException {
		return this.doDecrypt(propertyValue, decryptionUri).toString();
	}

	/** encrypts the input which is in Map format
	 * @param propertyValue - input is in format Map<String, Object>
	 * @return Map<String, Object>
	 * @throws EncryptionException
	 */
	@Override
	public Map<String, Object> encrypt(Map<String, Object> propertyValue) throws EncryptionException {
		return this.doEncrypt(propertyValue, encryptionBatchUri);
	}

	/** decrypts the input which is in Map format
	 * @param propertyValue - input is in format Map<String, Object>
	 * @return Map<String, Object>
	 * @throws EncryptionException
	 */
	@Override
	public Map<String, Object> decrypt(Map<String, Object> propertyValue) throws EncryptionException {
		return this.doDecrypt(propertyValue, decryptionBatchUri);
	}

	private <T> T doEncrypt(T propertyValue, String uri) throws EncryptionException {
		logger.debug("encrypt starts with value {}", propertyValue);
		Map<String, Object> map = new HashMap<>();
		Map<String, Object> encReqObj = new HashMap<>();
		encReqObj.put("tenantId", encryptionTenantId);
		encReqObj.put("value", propertyValue);
		encReqObj.put("type", encryptionMethod);
		map.put("encryptionRequests", Collections.singletonList(encReqObj));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(gson.toJson(map), headers);
		try {
			watch.start("EncryptionServiceImpl.encryptBatch");
			ResponseEntity<String> response = retryRestTemplate.postForEntity(uri, entity);
			watch.stop("EncryptionServiceImpl.encryptBatch");
			List<T> results = gson.fromJson(response.getBody(), new TypeToken<List<T>>() {
			}.getType());
			assert results != null;
			return results.get(0);
		} catch (ResourceAccessException e) {
			logger.error("Exception while connecting encryption service : {}", ExceptionUtils.getStackTrace(e));
			throw new EncryptionException("Exception while connecting encryption service! ");
		} catch (Exception e) {
			logger.error("Exception in encryption service !: {}", ExceptionUtils.getStackTrace(e));
			throw new EncryptionException("Exception in encryption service.");
		}
	}

	private  <T> T  doDecrypt(T propertyValue, String uri) throws EncryptionException {
		logger.debug("decrypt starts with value {}", propertyValue);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(gson.toJson(propertyValue), headers);

		try {
			watch.start("EncryptionServiceImpl.decryptBatch");
			ResponseEntity<String> response = retryRestTemplate.postForEntity(uri, entity);
			watch.stop("EncryptionServiceImpl.decryptBatch");
			return gson.fromJson(response.getBody(), new TypeToken<T>() {
			}.getType());
		} catch (ResourceAccessException e) {
			logger.error("Exception while connecting decryption service : {}", ExceptionUtils.getStackTrace(e));
			throw new EncryptionException("Exception while connecting encryption service ! ");
		} catch (Exception e) {
			logger.error("Exception in decryption service !: {}", ExceptionUtils.getStackTrace(e));
			throw new EncryptionException("Exception in encryption service ! ");
		}
	}

	@Override
	public String getServiceName() {
		return SUNBIRD_ENCRYPTION_SERVICE_NAME;
	}

	/**
	 * This method is used to check if the sunbird encryption service is up
	 * 
	 * @return boolean true/false
	 */

	@Override
	public ComponentHealthInfo getHealthInfo() {
		if (encryptionEnabled) {
			try {
				ResponseEntity<String> response = retryRestTemplate.getForEntity(encryptionServiceHealthCheckUri);
				if (!StringUtils.isEmpty(response.getBody()) && JSONUtil.convertStringJsonNode(response.getBody()).get("status").asText().equalsIgnoreCase("UP")) {
					logger.debug("Encryption service running !");
					return new ComponentHealthInfo(getServiceName(), true);
				} else {
					return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, response.getBody());
				}
			} catch (RestClientException | IOException ex) {
				logger.error("RestClientException when checking the health of the encryption service: {}", ExceptionUtils.getStackTrace(ex));
				return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, ex.getMessage());
			}
		} else {
			return new ComponentHealthInfo(getServiceName(), true, "ENCRYPTION_ENABLED", "false");
		}
	}

	public boolean isEncryptionServiceUp() {
		return getHealthInfo().isHealthy();
	}
}