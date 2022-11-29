package in.divoc.api.authenticator;

import okhttp3.*;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NotifyService {
	private static final Logger logger = Logger.getLogger(NotifyService.class);
	private static final String notificationURL = System.getenv().getOrDefault("NOTIFICATION_SERVICE_URL", "http://localhost:8765/notification-service/v1/notification");
	private static final String messageTemplate = System.getenv().getOrDefault("MESSAGE_TEMPLATE", "Your OTP to access SunbirdRC portal is %s. Please dont share this with anyone.");

	public void notify(String to, String otp) throws IOException {

		try {
			URL url = new URL(notificationURL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("content-type", "application/json");
			con.setRequestProperty("accept", "application/json");
			con.setDoOutput(true);
			String jsonInputString = "{\n" +
					"    \"recipient\": \"" + to + "\",\n" +
					"    \"message\": \"" + getMessage(otp) + "\",\n" +
					"    \"subject\": \"" + "Sunbird-RC OTP" + "\"\n" +
					"}";
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

	private String getMessage(String... args) {
		return String.format(messageTemplate, args);
	}

}