package dev.sunbirdrc.actors.services;

import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.pojos.NotificationMessage;
import okhttp3.*;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_NOTIFICATION_SERVICE_NAME;

public class NotificationService implements HealthIndicator {
    private static String connectionInfo;
    private static String healthUrl;
    private static Boolean enabled;
    public NotificationService(String connection, String healthInfo, Boolean notificationEnabled) {
        connectionInfo = connection;
        healthUrl = healthInfo;
        enabled = notificationEnabled;
    }

    public NotificationService() {
    }

    public Map<String, String> notify(NotificationMessage notificationMessage) throws IOException {
        Map<String, String> map = new HashMap<>();
        map.put("recipient", notificationMessage.getTo());
        map.put("message", notificationMessage.getMessage());
        map.put("subject", notificationMessage.getSubject());
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForObject(connectionInfo, map, HashMap.class);
    }

    @Override
    public String getServiceName() {
        return SUNBIRD_NOTIFICATION_SERVICE_NAME;
    }

    @Override
    public ComponentHealthInfo getHealthInfo() {
        if (enabled) {
            try {
                OkHttpClient client = new OkHttpClient();
                Request httpRequest = new Request.Builder()
                        .url(healthUrl)
                        .get()
                        .header("Content-Type", "application/json")
                        .build();
                Response response = client.newCall(httpRequest).execute();
                return new ComponentHealthInfo(getServiceName(), response.isSuccessful());
            } catch (Exception ex) {
                return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, ex.getMessage());
            }
        } else {
            return new ComponentHealthInfo(getServiceName(), true, "NOTIFICATION_ENABLED", "false");
        }
    }
}
