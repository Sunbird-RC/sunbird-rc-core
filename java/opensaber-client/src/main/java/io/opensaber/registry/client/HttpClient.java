package io.opensaber.registry.client;

import io.opensaber.pojos.Response;
import io.opensaber.registry.config.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

public class HttpClient {
    private static HttpClient ourInstance = new HttpClient();

    public static HttpClient getInstance() {
        return ourInstance;
    }

    private RestTemplate restTemplate;

    private HttpClient() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Configuration.HTTP_CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(Configuration.HTTP_READ_TIMEOUT);
        requestFactory.setConnectionRequestTimeout(Configuration.HTTP_CONNECTION_REQUEST_TIMEOUT);
        restTemplate = new RestTemplate(requestFactory);
    }

    public ResponseEntity<Response> post(String url, HttpHeaders headers, String payload) {
        return post(url, headers, new HashMap<>(), payload);
    }

    public ResponseEntity<Response> delete(String url, HttpHeaders headers, String payload) {
        return delete(url, headers, new HashMap<>(), payload);
    }

    public ResponseEntity<Response> delete(String url,HttpHeaders headers, Map<String, String> queryParams, String payload){
        HttpEntity<String> entity =  new HttpEntity<>(payload, headers);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
        queryParams.forEach((param, paramValue) -> uriBuilder.queryParam(param, paramValue));
        ResponseEntity<Response> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.DELETE, entity, Response.class);
        return response;
    }

    public ResponseEntity<Response> post(String url, HttpHeaders headers, Map<String, String> queryParams, String payload) {
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
        queryParams.forEach((param, paramValue) -> uriBuilder.queryParam(param, paramValue));
        ResponseEntity<Response> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.POST, entity, Response.class);
        return response;
    }

    public ResponseEntity<Response> get(String url, HttpHeaders headers) {
        return get(url, headers, new HashMap<>());
    }

    public ResponseEntity<Response> get(String url, HttpHeaders headers, Map<String, String> queryParams) {
        HttpEntity<String> entity = new HttpEntity<>(headers);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
        queryParams.forEach((param, paramValue) -> uriBuilder.queryParam(param, paramValue));
        ResponseEntity<Response> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, entity, Response.class);
        return response;
    }
}
