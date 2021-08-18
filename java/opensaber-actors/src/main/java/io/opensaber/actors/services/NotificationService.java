package io.opensaber.actors.services;

import io.opensaber.pojos.NotificationMessage;
import okhttp3.*;

import java.io.IOException;

public class NotificationService {
    private static String connectionInfo;
    public void setConnectionInfo(String connection) {
        connectionInfo = connection;
    }
    public Response callNotificationService(NotificationMessage notificationMessage) throws IOException {
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
}
