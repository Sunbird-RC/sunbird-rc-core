package io.opensaber.registry.service.impl;

import io.opensaber.registry.exception.SignatureException;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class RetryRestTemplate {

    @Autowired
    private RestTemplate restTemplate;

    @Retryable(value ={SignatureException.UnreachableException.class,ResourceAccessException.class,ServiceUnavailableException.class }, maxAttemptsExpression = "#{${service.retry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${service.retry.backoff.delay}}"))
    public ResponseEntity<String> postForEntity(String url, Object propertyValue){
        return restTemplate.postForEntity(url, propertyValue, String.class);
    }
    
    @Retryable(value ={SignatureException.UnreachableException.class,ResourceAccessException.class,ServiceUnavailableException.class }, maxAttemptsExpression = "#{${service.retry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${service.retry.backoff.delay}}"))
    public ResponseEntity<String> getForEntity(String url){
        return restTemplate.getForEntity(url, String.class);
    }

}
