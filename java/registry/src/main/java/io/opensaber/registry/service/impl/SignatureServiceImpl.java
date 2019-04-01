package io.opensaber.registry.service.impl;

import com.google.gson.Gson;
import io.opensaber.registry.exception.SignatureException;
import io.opensaber.registry.service.SignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class SignatureServiceImpl implements SignatureService {

	private static Logger logger = LoggerFactory.getLogger(SignatureService.class);
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

	/** This method checks signature service is available or not
	 * @return - true or false
	 * @throws SignatureException.UnreachableException
	 */
	@Override
	public boolean isServiceUp() throws SignatureException.UnreachableException {
		boolean isSignServiceUp = false;
		try {
			ResponseEntity<String> response = retryRestTemplate.getForEntity(healthCheckURL);
			if (response.getBody().equalsIgnoreCase("UP")) {
				isSignServiceUp = true;
				logger.debug("Signature service running !");
			}
		} catch (RestClientException ex) {
			logger.error("RestClientException when checking the health of the Sunbird signature service: ", ex);
			throw new SignatureException().new UnreachableException(ex.getMessage());
		}
		return isSignServiceUp;
	}

	/** This method calls signature service for signing the object
	 * @param propertyValue - contains input need to be signed
	 * @return - signed data with key
	 * @throws SignatureException.UnreachableException
	 * @throws SignatureException.CreationException
	 */
	@Override
	public Object sign(Object propertyValue)
			throws SignatureException.UnreachableException, SignatureException.CreationException {
		ResponseEntity<String> response = null;
		Object result = null;
		try {
			response = retryRestTemplate.postForEntity(signURL, propertyValue);
			result = new Gson().fromJson(response.getBody(), Object.class);
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
	public Object verify(Object propertyValue)
			throws SignatureException.UnreachableException, SignatureException.VerificationException {
		logger.debug("verify method starts with value {}",propertyValue);
		ResponseEntity<String> response = null;
		Object result = null;
		try {
			response = retryRestTemplate.postForEntity(verifyURL, propertyValue);
			result = new Gson().fromJson(response.getBody(), Object.class);
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
}
