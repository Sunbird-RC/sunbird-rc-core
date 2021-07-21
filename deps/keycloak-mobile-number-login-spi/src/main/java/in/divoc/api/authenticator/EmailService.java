package in.divoc.api.authenticator;// File Name SendEmail.java

import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class EmailService {
    private static final Logger logger = Logger.getLogger(EmailService.class);

    void sendEmail(String email, String otp) {
        try {
            URL url = new URL("https://api.sendinblue.com/v3/smtp/email");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("content-type", "application/json");
            con.setRequestProperty("accept", "application/json");
            con.setRequestProperty("api-key", "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");
            con.setRequestProperty("Host", "api.sendinblue.com");

            con.setDoOutput(true);
            String jsonInputString = "{  \n   \"sender\":{  \n      \"name\":\"NDEAR\",\n      \"email\":\"no-reply@ndear.xiv.in\"\n   },\n   \"to\":[  \n      {  \n         \"email\": \""  + email + "\",\n         \"name\":\""+email+"\"\n      }\n   ],\n   \"subject\":\"NDEAR - LOGIN OTP\",\n   \"htmlContent\":\"<html><head></head><body><p>Your One Time Password(OTP) is :</p><h1> " + otp + "</h1></p></body></html>\"\n}";
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            try(BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println(response.toString());
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }
}