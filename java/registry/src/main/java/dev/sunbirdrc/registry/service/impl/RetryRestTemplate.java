package dev.sunbirdrc.registry.service.impl;

import dev.sunbirdrc.registry.exception.SignatureException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.Collections;

@Component
public class RetryRestTemplate {

    @Autowired
    private RestTemplate restTemplate;

    @Retryable(value = {SignatureException.UnreachableException.class, ResourceAccessException.class, ServiceUnavailableException.class}, maxAttemptsExpression = "#{${service.retry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${service.retry.backoff.delay}}"))
    public ResponseEntity<String> postForEntity(String url, Object propertyValue) {
        return restTemplate.postForEntity(url, propertyValue, String.class);
    }

    @Retryable(value = {SignatureException.UnreachableException.class, ResourceAccessException.class, ServiceUnavailableException.class}, maxAttemptsExpression = "#{${service.retry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${service.retry.backoff.delay}}"))
    public ResponseEntity<String> putForEntity(String url, HttpEntity<?> propertyValue, Object... uriVariables) {
        return restTemplate.exchange(url, HttpMethod.PUT, propertyValue, String.class, uriVariables);
    }

    @Retryable(value = {SignatureException.UnreachableException.class, ResourceAccessException.class, ServiceUnavailableException.class}, maxAttemptsExpression = "#{${service.retry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${service.retry.backoff.delay}}"))
    public ResponseEntity<String> getForEntity(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
    }

    @Retryable(value = {SignatureException.UnreachableException.class, ResourceAccessException.class, ServiceUnavailableException.class}, maxAttemptsExpression = "#{${service.retry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${service.retry.backoff.delay}}"))
    public ResponseEntity<String> getForEntity(String url, Object... uriVariables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory());
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), String.class, uriVariables);
    }

    @Retryable(value = {SignatureException.UnreachableException.class, ResourceAccessException.class, ServiceUnavailableException.class}, maxAttemptsExpression = "#{${service.retry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${service.retry.backoff.delay}}"))
    public void deleteForEntity(String url, Object... uriVariables) {
        restTemplate.delete(url, uriVariables);
    }

    @Retryable(value = {SignatureException.UnreachableException.class, ResourceAccessException.class, ServiceUnavailableException.class}, maxAttemptsExpression = "#{${service.retry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${service.retry.backoff.delay}}"))
    public <T> ResponseEntity<T> getForObject(String url, HttpHeaders headers, Class<T> tClass, Object... uriVariables) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), tClass, uriVariables);
    }

}
