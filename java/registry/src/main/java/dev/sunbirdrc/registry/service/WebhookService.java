package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.registry.model.dto.WebhookEvent;
import dev.sunbirdrc.registry.service.impl.RetryRestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    @Value("${webhook.enabled}")
    private Boolean webhookEnabled;
    @Autowired
    private RetryRestTemplate retryRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    public void postEvent(WebhookEvent event) {
        if (webhookEnabled) {
            logger.debug("Post event {}", event);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            try {
                HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(event), headers);
                ResponseEntity<String> response = retryRestTemplate.postForEntity(event.getWebhookUrl(), entity);

            } catch (JsonProcessingException e) {
                logger.error("Failed calling webhook event, {}", e.getMessage(), e);
            }
        } else {
            logger.info("Webhook service is disabled");
        }
    }
}
