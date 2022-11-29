package dev.sunbirdrc.registry.controller;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.registry.entities.VerificationRequest;
import dev.sunbirdrc.registry.helper.RegistryHelper;
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


    private static final String VERIFIED = "verified";
    private static final String RESULTS = "results";
    private final RestTemplate restTemplate;
    private final String verifyURL;

    private final RegistryHelper registryHelper;

    public RegistryCertificateController(RestTemplate restTemplate, @Value("${signature.verifyURL}") String verifyURL,
                                         RegistryHelper registryHelper) {
        this.restTemplate = restTemplate;
        this.verifyURL = verifyURL;
        this.registryHelper = registryHelper;
    }

    @RequestMapping(value = "/api/v1/verify", method = RequestMethod.POST)
    public ResponseEntity<Object> verifyCertificate(@RequestBody VerificationRequest verificationRequest) {
        try {
            if (registryHelper.checkIfCredentialIsRevoked(verificationRequest.getSignedCredentials().toString())) {
                return new ResponseEntity<>(JsonNodeFactory.instance.objectNode().put(VERIFIED, false)
                        .put(RESULTS, "Credential is revoked"), HttpStatus.BAD_REQUEST);
            } else {
                Object response = restTemplate.postForObject(verifyURL, verificationRequest, Object.class);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
