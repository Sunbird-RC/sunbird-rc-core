package io.opensaber.registry.service.impl;

import com.google.gson.Gson;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.exception.SignatureException;
import io.opensaber.registry.service.SignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class SignatureServiceImpl implements SignatureService {

    @Value("${signature.healthCheckURL}")
    private String healthCheckURL;

    @Value("${signature.signURL}")
    private String signURL;

    @Value("${signature.verifyURL}")
    private String verifyURL;

    @Value("${signature.keysURL}")
    private String keysURL;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Gson gson;

    @Autowired
    private OpenSaberInstrumentation watch;

    private static Logger logger = LoggerFactory.getLogger(SignatureService.class);

    @Override
    public boolean isServiceUp() throws SignatureException.UnreachableException {
        boolean isSignServiceUp = false;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(healthCheckURL, String.class);
            if (response.getBody().equalsIgnoreCase("UP")) {
                isSignServiceUp = true;
                logger.debug("Signature service running !");
            }
        } catch (RestClientException ex) {
            logger.error("RestClientException when checking the health of the Sunbird encryption service: ", ex);
            throw new SignatureException().new UnreachableException(ex.getMessage());
        }
        return isSignServiceUp;
    }

    @Override
    public Object sign(Object propertyValue) throws SignatureException.UnreachableException,
            SignatureException.CreationException {
        ResponseEntity<String> response = null;
        Object result = null;
        try {
            response = restTemplate.postForEntity(signURL, propertyValue, String.class);
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

    @Override
    public Object verify(Object propertyValue) throws SignatureException.UnreachableException,
            SignatureException.VerificationException {
        ResponseEntity<String> response = null;
        Object result = null;
        try {
            response = restTemplate.postForEntity(verifyURL, propertyValue, String.class);
            result = new Gson().fromJson(response.getBody(), Object.class);
        } catch (RestClientException ex) {
            logger.error("RestClientException when verifying: ", ex);
            throw new SignatureException().new UnreachableException(ex.getMessage());
        } catch (Exception e) {
            logger.error("RestClientException when verifying: ", e);
            throw new SignatureException().new VerificationException(e.getMessage());
        }
        return result;
    }

    @Override
    public String getKey(String keyId) throws SignatureException.UnreachableException,
            SignatureException.KeyNotFoundException {
        ResponseEntity<String> response = null;
        String result = null;
        try {
            response = restTemplate.getForEntity(keysURL + "/" + keyId, String.class);
            result = new Gson().fromJson(response.getBody(), String.class);
        } catch (RestClientException ex) {
            logger.error("RestClientException when verifying: ", ex);
            throw new SignatureException().new UnreachableException(ex.getMessage());
        } catch (Exception e) {
            logger.error("RestClientException when verifying: ", e);
            throw new SignatureException().new KeyNotFoundException(e.getMessage());
        }
        return result;
    }
}
