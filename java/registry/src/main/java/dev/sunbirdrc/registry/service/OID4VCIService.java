package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.service.impl.RetryRestTemplate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Optional, flag-gated hook that asks oid4vc-service to create a wallet
 * credential offer after a credential is issued. It is deliberately
 * fail-open: any error here is logged and swallowed so that entity creation
 * and claim-grant flows never fail because the wallet offer could not be made.
 *
 * Enabled via oid4vc.enabled=true (default false).
 */
@Service
public class OID4VCIService {
    private static final Logger logger = LoggerFactory.getLogger(OID4VCIService.class);

    @Value("${oid4vc.enabled:false}")
    private boolean oid4vcEnabled;

    @Value("${oid4vc.offerUrl:}")
    private String offerUrl;

    @Autowired
    private RetryRestTemplate retryRestTemplate;

    public boolean isEnabled() {
        return oid4vcEnabled && offerUrl != null && !offerUrl.isEmpty();
    }

    /**
     * Fire-and-forget offer creation. Runs async and never throws to the caller.
     *
     * @param credentialConfigurationId the credential type/config id (schema name)
     * @param claims                    the credential subject claims
     */
    @Async
    public void createOfferSafely(String credentialConfigurationId, JsonNode claims) {
        if (!isEnabled()) {
            return;
        }
        try {
            ObjectNode body = JsonNodeFactory.instance.objectNode();
            body.put("credential_configuration_id", credentialConfigurationId);
            body.set("claims", claims != null ? claims : JsonNodeFactory.instance.objectNode());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = retryRestTemplate.postForEntity(offerUrl, request);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("OID4VCI offer created for {}", credentialConfigurationId);
            } else {
                logger.warn("OID4VCI offer creation returned {}", response.getStatusCode());
            }
        } catch (Exception e) {
            // fail-open: never propagate
            logger.warn("OID4VCI offer creation failed (ignored): {}", ExceptionUtils.getStackTrace(e));
        }
    }
}
