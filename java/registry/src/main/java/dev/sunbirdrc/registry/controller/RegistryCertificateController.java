package dev.sunbirdrc.registry.controller;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.registry.entities.VerificationRequest;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.helper.SignatureHelper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final Logger logger = LoggerFactory.getLogger(RegistryCertificateController.class);

    private static final String VERIFIED = "verified";
    private static final String RESULTS = "results";
    @Autowired
    private SignatureHelper signatureHelper;
    @Autowired
    private RegistryHelper registryHelper;

    @RequestMapping(value = "/api/v1/verify", method = RequestMethod.POST)
    public ResponseEntity<Object> verifyCertificate(@RequestBody VerificationRequest verificationRequest) {
        try {
            if (registryHelper.checkIfCredentialIsRevoked(verificationRequest.getSignedCredentials().toString())) {
                return new ResponseEntity<>(JsonNodeFactory.instance.objectNode().put(VERIFIED, false)
                        .put(RESULTS, "Credential is revoked"), HttpStatus.BAD_REQUEST);
            } else {
                Object response = signatureHelper.verify(verificationRequest);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.error("Exception occurred while verifying certificate: {}", ExceptionUtils.getStackTrace(e));
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
