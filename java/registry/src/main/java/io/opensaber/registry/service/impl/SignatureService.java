package io.opensaber.registry.service.impl;

import com.google.gson.Gson;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.exception.SignatureException;
import io.opensaber.registry.service.ISignatureService;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
public class SignatureService implements ISignatureService {

    @Value("signature.healthCheckURL")
    private String healthCheckURI;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Gson gson;

    @Autowired
    private OpenSaberInstrumentation watch;

    private static Logger logger = LoggerFactory.getLogger(SignatureService.class);

    @Override
    public Map<String, Object> sign(Object propertyValue) throws SignatureException {
        return null;
    }

    @Override
    public Map<String, Object> verify(Map<String, Object> propertyValue) throws SignatureException {

        return null;
    }

    @Override
    @Retryable(label = "signatureHealthCheck", maxAttempts = 3)
    public boolean isServiceUp() {
        boolean isEncryptionServiceUp = false;
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(healthCheckURI, String.class);
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
