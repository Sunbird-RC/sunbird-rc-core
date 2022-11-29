package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.registry.exception.SignatureException;
import dev.sunbirdrc.registry.service.FileStorageService;
import dev.sunbirdrc.registry.service.SignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_SIGNATURE_SERVICE_NAME;

import java.util.Map;

import static dev.sunbirdrc.registry.Constants.CREDENTIAL_TEMPLATE;
import static dev.sunbirdrc.registry.Constants.MINIO_URI_PREFIX;

@Component
public class SignatureServiceImpl implements SignatureService {

	private static Logger logger = LoggerFactory.getLogger(SignatureService.class);
	@Value("${signature.enabled}")
	private boolean signatureEnabled;
	@Value("${signature.healthCheckURL}")
	private String healthCheckURL;
	@Value("${signature.signURL}")
	private String signURL;
	@Value("${signature.verifyURL}")
	private String verifyURL;
	@Value("${signature.keysURL}")
	private String keysURL;
	@Autowired
	private RetryRestTemplate retryRestTemplate;
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private FileStorageService fileStorageService;

	/** This method checks signature service is available or not
	 * @return - true or false
	 */
	@Override
	public ComponentHealthInfo getHealthInfo() {
		if (signatureEnabled) {
			try {
				ResponseEntity<String> response = retryRestTemplate.getForEntity(healthCheckURL);
				if (!StringUtils.isEmpty(response.getBody()) && Arrays.asList("UP", "OK").contains(response.getBody().toUpperCase())) {
					logger.debug("Signature service running !");
					return new ComponentHealthInfo(getServiceName(), true);
				} else {
					return new ComponentHealthInfo(getServiceName(), false);
				}
			} catch (RestClientException ex) {
				logger.error("RestClientException when checking the health of the Sunbird signature service: ", ex);
				return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, ex.getMessage());
			}
		} else {
			return new ComponentHealthInfo(getServiceName(), true, "SIGNATURE_ENABLED", "false");
		}
	}
	private void replaceMinioURIWithSignedURL(Map<String, Object> signRequestObject) throws Exception {
		if (signRequestObject.containsKey(CREDENTIAL_TEMPLATE) &&  signRequestObject.get(CREDENTIAL_TEMPLATE) instanceof String
				&& ((String) signRequestObject.get(CREDENTIAL_TEMPLATE)).startsWith(MINIO_URI_PREFIX)) {
			signRequestObject.put(CREDENTIAL_TEMPLATE, fileStorageService.getSignedUrl(((String) signRequestObject.get(CREDENTIAL_TEMPLATE)).substring(MINIO_URI_PREFIX.length())));
		}
	}
	/** This method calls signature service for signing the object
	 * @param propertyValue - contains input need to be signed
	 * @return - signed data with key
	 * @throws SignatureException.UnreachableException
	 * @throws SignatureException.CreationException
	 */
	@Override
	public Object sign(Map<String, Object> propertyValue)
			throws SignatureException.UnreachableException, SignatureException.CreationException {
		ResponseEntity<String> response = null;
		Object result = null;
		try {
			replaceMinioURIWithSignedURL(propertyValue);
			response = retryRestTemplate.postForEntity(signURL, propertyValue);
			result = objectMapper.readTree(response.getBody());
			logger.info("Successfully generated signed credentials");
		} catch (RestClientException ex) {
			logger.error("RestClientException when signing: ", ex);
			throw new SignatureException().new UnreachableException(ex.getMessage());
		} catch (Exception e) {
			logger.error("RestClientException when signing: ", e);
			throw new SignatureException().new CreationException(e.getMessage());
		}
		return result;
	}

	/** This method verifies the sign value with request input object
	 * @param propertyValue - contains input along with signed value
	 * @return true/false
	 * @throws SignatureException.UnreachableException
	 * @throws SignatureException.VerificationException
	 */
	@Override
	public boolean verify(Object propertyValue)
			throws SignatureException.UnreachableException, SignatureException.VerificationException {
		logger.debug("verify method starts with value {}",propertyValue);
		ResponseEntity<String> response = null;
		boolean result = false;
		try {
			response = retryRestTemplate.postForEntity(verifyURL, propertyValue);
			JsonNode resultNode = objectMapper.readTree(response.getBody());
			result = resultNode.get("verified").asBoolean();
		} catch (RestClientException ex) {
			logger.error("RestClientException when verifying: ", ex);
			throw new SignatureException().new UnreachableException(ex.getMessage());
		} catch (Exception e) {
			logger.error("RestClientException when verifying: ", e);
			throw new SignatureException().new VerificationException(e.getMessage());
		}
		logger.debug("verify method ends with value {}",result);
		return result;
	}

	/** This medhod gives public key based on keyId
	 * @param keyId
	 * @return public key
	 * @throws SignatureException.UnreachableException
	 * @throws SignatureException.KeyNotFoundException
	 */
	@Override
	public String getKey(String keyId)
			throws SignatureException.UnreachableException, SignatureException.KeyNotFoundException {
		logger.debug("getKey method starts with value {}",keyId);
		ResponseEntity<String> response = null;
		String result = null;
		try {
			response = retryRestTemplate.getForEntity(keysURL + "/" + keyId);
			result = response.getBody();
		} catch (RestClientException ex) {
			logger.error("RestClientException when verifying: ", ex);
			throw new SignatureException().new UnreachableException(ex.getMessage());
		} catch (Exception e) {
			logger.error("RestClientException when verifying: ", e);
			throw new SignatureException().new KeyNotFoundException(keyId);
		}
		logger.debug("getKey method ends with value {}",result);
		return result;
	}

	@Override
	public String getServiceName() {
		return SUNBIRD_SIGNATURE_SERVICE_NAME;
	}

	public boolean isServiceUp() {
		return getHealthInfo().isHealthy();
	}
}
