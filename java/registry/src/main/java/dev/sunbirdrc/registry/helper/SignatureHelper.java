package dev.sunbirdrc.registry.helper;

import dev.sunbirdrc.registry.exception.SignatureException;
import dev.sunbirdrc.registry.service.FileStorageService;
import dev.sunbirdrc.registry.service.SignatureService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Map;

import static dev.sunbirdrc.registry.Constants.CREDENTIAL_TEMPLATE;
import static dev.sunbirdrc.registry.Constants.MINIO_URI_PREFIX;

@Component
@ConditionalOnProperty(name = "signature.enabled", havingValue = "true")
public class SignatureHelper {

	private static Logger logger = LoggerFactory.getLogger(SignatureHelper.class);
	@Autowired
	private SignatureService signatureService;

	@Value("${file-storage.enabled}")
	private boolean fileStorageEnabled;
	@Autowired(required = false)
	private FileStorageService fileStorageService;

	private void replaceMinioURIWithSignedURL(Map<String, Object> signRequestObject) throws Exception {
		if (signRequestObject.containsKey(CREDENTIAL_TEMPLATE) &&  signRequestObject.get(CREDENTIAL_TEMPLATE) instanceof String
				&& ((String) signRequestObject.get(CREDENTIAL_TEMPLATE)).startsWith(MINIO_URI_PREFIX)) {
			if(!fileStorageEnabled) {
				throw new SignatureException().new UnreachableException("File Storage is not enabled! Enable file storage to load credential template from Minio");
			}
			signRequestObject.put(CREDENTIAL_TEMPLATE, fileStorageService.getSignedUrl(((String) signRequestObject.get(CREDENTIAL_TEMPLATE)).substring(MINIO_URI_PREFIX.length())));
		}
	}
	/** This method calls signature service for signing the object
	 * @param propertyValue - contains input need to be signed
	 * @return - signed data with key
	 * @throws SignatureException.UnreachableException
	 * @throws SignatureException.CreationException
	 */

	public Object sign(Map<String, Object> propertyValue)
			throws SignatureException.UnreachableException, SignatureException.CreationException {
		ResponseEntity<String> response = null;
		Object result = null;
		try {
			replaceMinioURIWithSignedURL(propertyValue);
			result = signatureService.sign(propertyValue);
			logger.info("Successfully generated signed credentials");
		} catch (SignatureException.UnreachableException e) {
			logger.error("SignatureException when signing: {}", ExceptionUtils.getStackTrace(e));
			throw e;
		} catch (RestClientException e) {
			logger.error("RestClientException when signing: {}", ExceptionUtils.getStackTrace(e));
			throw new SignatureException().new UnreachableException(e.getMessage());
		} catch (Exception e) {
			logger.error("SignatureException when signing: {}", ExceptionUtils.getStackTrace(e));
			throw new SignatureException.CreationException(e.getMessage());
		}
		return result;
	}

	/** This method verifies the sign value with request input object
	 * @param propertyValue - contains input along with signed value
	 * @return true/false
	 * @throws SignatureException.UnreachableException
	 * @throws SignatureException.VerificationException
	 */
	public boolean verify(Object propertyValue)
			throws SignatureException.UnreachableException, SignatureException.VerificationException {
		logger.debug("verify method starts with value {}",propertyValue);
		ResponseEntity<String> response = null;
		boolean result = false;
		try {
			result = signatureService.verify(propertyValue);
		} catch (RestClientException e) {
			logger.error("RestClientException when verifying: {}", ExceptionUtils.getStackTrace(e));
			throw new SignatureException().new UnreachableException(e.getMessage());
		} catch (Exception e) {
			logger.error("RestClientException when verifying: {}", ExceptionUtils.getStackTrace(e));
			throw new SignatureException.VerificationException(e.getMessage());
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
	public String getKey(String keyId)
			throws SignatureException.UnreachableException, SignatureException.KeyNotFoundException {
		logger.debug("getKey method starts with value {}",keyId);
		ResponseEntity<String> response = null;
		String result = null;
		try {
			result = signatureService.getKey(keyId);
		} catch (RestClientException e) {
			logger.error("RestClientException when verifying: {}", ExceptionUtils.getStackTrace(e));
			throw new SignatureException().new UnreachableException(e.getMessage());
		} catch (Exception e) {
			logger.error("RestClientException when verifying: {}", ExceptionUtils.getStackTrace(e));
			throw new SignatureException().new KeyNotFoundException(keyId);
		}
		logger.debug("getKey method ends with value {}",result);
		return result;
	}

	public void revoke(String entityName, String entityId, String signed) {
		signatureService.revoke(entityName, entityId, signed);
	}

	public boolean isHealthy() {
		return this.signatureService.getHealthInfo().isHealthy();
	}
}
