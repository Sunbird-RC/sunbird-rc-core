package io.opensaber.registry.client;

import io.opensaber.pojos.Response;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class HttpClient {
    private static HttpClient ourInstance = new HttpClient();

    public static HttpClient getInstance() {
        return ourInstance;
    }

    private RestTemplate restTemplate;

    private HttpClient() {
        restTemplate = new RestTemplate();
    }

    public ResponseEntity<Response> post(String url, HttpHeaders headers, String payload) {
        System.out.println("JSON-LD payload");
        return post(url, headers, new HashMap<>(), payload);
    }

    public ResponseEntity<Response> post(String url, HttpHeaders headers, Map<String, String> queryParams, String payload) {
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Response> response = restTemplate.exchange(url, HttpMethod.POST, entity, Response.class, queryParams);
        return response;
    }

    public ResponseEntity<Response> get(String url, HttpHeaders headers) {
        return get(url, headers, new HashMap<>());
    }

    public ResponseEntity<Response> get(String url, HttpHeaders headers, Map<String, String> queryParams) {
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Response> response = restTemplate.exchange(url, HttpMethod.GET, entity, Response.class, queryParams);
        return response;
    }
}
