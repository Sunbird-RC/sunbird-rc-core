package io.opensaber.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

@Controller
public class RegistryCertificateController {

    private final RestTemplate restTemplate;
    private final String verifyURL;

    public RegistryCertificateController(RestTemplate restTemplate, @Value("${signature.verifyURL}") String verifyURL) {
        this.restTemplate = restTemplate;
        this.verifyURL = verifyURL;
    }

    @RequestMapping(value = "/api/v1/verify", method = RequestMethod.POST)
    public ResponseEntity<Object> verifyCertificate(@RequestBody JsonNode payload) {
        try {
            Object response = restTemplate.postForObject(verifyURL, payload, Object.class);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
