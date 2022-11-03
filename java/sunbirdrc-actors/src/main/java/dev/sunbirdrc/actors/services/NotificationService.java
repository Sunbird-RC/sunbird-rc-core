package dev.sunbirdrc.actors.services;

import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.pojos.NotificationMessage;
import okhttp3.*;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

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

    public Response notify(NotificationMessage notificationMessage) throws IOException {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create("{\n" +
                "    \"recipient\": \"" + notificationMessage.getTo() + "\",\n" +
                "    \"message\": \"" + notificationMessage.getMessage() + "\",\n" +
                "    \"subject\": \"" + notificationMessage.getSubject() + "\"\n" +
                "}", JSON);
        Request httpRequest = new Request.Builder()
                .url(connectionInfo)
                .method("POST", requestBody)
                .header("Content-Type", "application/json")
                .build();
        Response response = client.newCall(httpRequest).execute();
        return response;
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
