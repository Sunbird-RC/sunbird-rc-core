package io.opensaber.registry.test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.opensaber.pojos.Response;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RegistryTestBase {

	public static final String FORMAT = "JSON-LD";
	private static final String REPLACING_SUBJECT_LABEL = "<@id>";
	private static final String EMPTY_STRING = "";
	private static final String CONTEXT_CONSTANT = "teacher:";
	protected static Type mapType = new TypeToken<Map<String, String>>() {
	}.getType();
	private static String ssoUrl = System.getenv("sunbird_sso_url");
	private static String ssoClientId = System.getenv("sunbird_sso_client_id");
	private static String ssoUsername = System.getenv("sunbird_sso_username");
	private static String ssoPassword = System.getenv("sunbird_sso_password");
	private static String ssoRealm = System.getenv("sunbird_sso_realm");
	public static String accessToken = generateAuthToken();
	public String jsonld;
	protected RestTemplate restTemplate;

	public static String extractIdWithoutContext(String label) {
		String extractedId = label;
		Pattern pattern = Pattern.compile("^" + Pattern.quote(Constants.INTEGRATION_TEST_BASE_URL) + "(.*?)$");
		Matcher matcher = pattern.matcher(label);
		if (matcher.find()) {
			extractedId = matcher.group(1);
		}
		return extractedId;
	}

	public static String generateRandomId() {
		return UUID.randomUUID().toString();
	}

	private static String generateAuthToken() {
		String ssoAuthBody = new StringBuilder().append("client_id=").append(ssoClientId).append("&username=")
				.append(ssoUsername).append("&password=").append(ssoPassword).append("&grant_type=password").toString();
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("no-cache");
		headers.set("content-type", "application/x-www-form-urlencoded");
		HttpEntity<String> request = new HttpEntity<>(ssoAuthBody, headers);
		String url = ssoUrl + "realms/" + ssoRealm + "/protocol/openid-connect/token ";
		ResponseEntity<String> response = new RestTemplate().postForEntity(url, request, String.class);
		Map<String, String> myMap = new Gson().fromJson(response.getBody(), mapType);
		return myMap.getOrDefault("access_token", "");
	}

	public void setJsonld(String filename) {
		try {
			String file = Paths.get(getPath(filename)).toString();
			jsonld = readFromFile(file);
		} catch (Exception e) {
			jsonld = EMPTY_STRING;
		}
	}

	public String readFromFile(String file) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader reader = Files.newBufferedReader(Paths.get(file));
			Stream<String> lines = reader.lines();
			lines.forEach(sb::append);
			lines.close();
		} catch (IOException e) {
			return EMPTY_STRING;
		}
		return sb.toString();
	}

	public URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

	public String generateBaseUrl() {
		return Constants.INTEGRATION_TEST_BASE_URL;
	}

	public String setJsonldWithNewRootLabel() {
		String id = null;
		String replacingId = null;
		while (jsonld.contains(REPLACING_SUBJECT_LABEL)) {
			if (id == null) {
				id = generateRandomId();
				replacingId = CONTEXT_CONSTANT + id;
			} else {
				replacingId = CONTEXT_CONSTANT + generateRandomId();
			}
			jsonld = jsonld.replaceFirst(REPLACING_SUBJECT_LABEL, replacingId);

		}
		return id;
	}

	public void setJsonldWithNewRootLabel(String id) {
		while (jsonld.contains(REPLACING_SUBJECT_LABEL)) {
			jsonld = jsonld.replaceFirst(REPLACING_SUBJECT_LABEL, CONTEXT_CONSTANT + id);
		}
	}

	public ResponseEntity<Response> createEntity(String jsonldData, String url, HttpHeaders headers) {
		HttpEntity<String> entity = new HttpEntity<>(jsonldData, headers);
		ResponseEntity<Response> response = restTemplate.postForEntity(url, entity, Response.class);
		return response;
	}

	public ResponseEntity<Response> updateEntity(String jsonldData, String url, HttpHeaders headers) {
		HttpEntity<String> entity = new HttpEntity<>(jsonldData, headers);
		Response response = restTemplate.patchForObject(url, entity, Response.class);
		return new ResponseEntity(response, HttpStatus.OK);
	}

	public ResponseEntity<Response> fetchEntity(String url, HttpHeaders headers) {
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<Response> response = restTemplate.exchange(url, HttpMethod.GET, entity, Response.class);
		return response;
	}

	public ResponseEntity<Response> addEntity(String jsonldData, String url, HttpHeaders headers) {
		HttpEntity<String> entity = new HttpEntity<>(jsonldData, headers);
		ResponseEntity<Response> response = restTemplate.postForEntity(url, entity, Response.class);
		return response;
	}

	public ResponseEntity<Response> update(String jsonldData, String url, HttpHeaders headers) {
		HttpEntity<String> entity = new HttpEntity<>(jsonldData, headers);
		ResponseEntity<Response> response = restTemplate.postForEntity(url, entity, Response.class);
		return response;
	}

	public ResponseEntity<Response> delete(String url, HttpHeaders headers) {
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<Response> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Response.class);
		return response;
	}

	public ResponseEntity<Response> readEntity(String url, HttpHeaders headers, String id) {
		HttpEntity<String> entity = new HttpEntity<>(headers);
		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("id", id);
		ResponseEntity<Response> response = restTemplate.exchange(url, HttpMethod.GET, entity, Response.class,
				queryParams);
		return response;
	}

	public ResponseEntity<Response> search(String jsonldData, String url, HttpHeaders headers) {
		HttpEntity<String> entity = new HttpEntity<>(jsonldData, headers);
		ResponseEntity<Response> response = restTemplate.postForEntity(url, entity, Response.class);
		return response;
	}

}
