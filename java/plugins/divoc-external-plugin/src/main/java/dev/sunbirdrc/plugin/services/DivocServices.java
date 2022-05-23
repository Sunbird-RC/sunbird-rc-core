package dev.sunbirdrc.plugin.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DivocServices {

	private static final String GRANT_TYPE = "grant_type";
	private static final String CLIENT_ID = "client_id";
	private static final String CLIENT_CREDENTIALS = "client_credentials";
	private static final String CLIENT_SECRET = "client_secret";
	private static final String ACCESS_TOKEN = "access_token";
	@Autowired
	private RestTemplate restTemplateProvider;

	private Logger LOGGER = LoggerFactory.getLogger(DivocServices.class);

	@Value("${divoc.keycloak.url:#{environment.DIVOC_KEYCLOAK_URL}}")
	private String divocKeycloakUrl;

	@Value("${divoc.url:#{environment.DIVOC_URL}}")
	private String divocUrl;

	@Value("${divoc.keycloak.clientId:#{environment.DIVOC_KEYCLOAK_CLIENT_ID}}")
	private String divocClientId;

	@Value("${divoc.keycloak.clientSecret:#{environment.DIVOC_KEYCLOAK_CLIENT_SECRET}}")
	private String divocClientSecret;

	private static final int RETRY_MILLI_SECONDS = 1 * 60 * 10 * 1000;

	public void init() {
		LOGGER.info("Fetching divoc token");
		fetchClientToken();
	}

    @CacheEvict("divocToken")
    @Scheduled(fixedDelay = RETRY_MILLI_SECONDS)
    public void cacheEvict() {
    }

	@Cacheable(value = "divocToken", unless = "#result==null")
	public String fetchClientToken() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add(GRANT_TYPE, CLIENT_CREDENTIALS);
		map.add(CLIENT_ID, divocClientId);
		map.add(CLIENT_SECRET, divocClientSecret);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

		ResponseEntity<Map> response = null;
		try {
			response = restTemplateProvider.exchange(divocKeycloakUrl, HttpMethod.POST, entity, Map.class);
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			LOGGER.error("Fetching divoc token failed: ");
			exception.printStackTrace();
		}
		if (response != null && response.getBody() != null && response.getBody().containsKey(ACCESS_TOKEN)) {
			LOGGER.info("Divoc token fetch successfully.");
			return response.getBody().get(ACCESS_TOKEN).toString();
		}
		return null;
	}

	public byte[] fetchDivocPdf(String clientToken, String preEnrollmentCode, String mobile) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + clientToken);
		Map<String, String> map = new HashMap<>();
		map.put("mobile", mobile);
		map.put("beneficiaryId", preEnrollmentCode);
		HttpEntity<Map<String, String>> entity = new HttpEntity<>(map, headers);

		ResponseEntity<byte[]> response = null;
		try {
			response = restTemplateProvider.exchange(divocUrl, HttpMethod.POST, entity, byte[].class);
			return response.getBody();
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			LOGGER.error("Fetching divoc PDF failed: ", exception);
			exception.printStackTrace();
		}
		return null;
	}
}
