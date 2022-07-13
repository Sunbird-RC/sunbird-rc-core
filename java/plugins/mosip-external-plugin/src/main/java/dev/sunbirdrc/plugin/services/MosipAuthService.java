package dev.sunbirdrc.plugin.services;

import dev.sunbirdrc.plugin.constant.Constants;
import dev.sunbirdrc.plugin.dto.AuthRequestDto;
import dev.sunbirdrc.plugin.dto.RequestCredentialsDto;
import dev.sunbirdrc.plugin.dto.RequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class MosipAuthService {

    private Logger LOGGER = LoggerFactory.getLogger(MosipAuthService.class);

    @Autowired
    private RestTemplate restTemplateProvider;

    @Value("${mosip.event.hubURL:#{environment.HUB_URL}}")
    private String webSubHubUrl;

    @Value("${mosip.event.secret:#{environment.EVENT_SECRET}}")
    private String webSubSecret;
    @Value("${mosip.appId:resident}")
    private String appId;

    @Value("${mosip.clientId:sunbird}")
    private String clientId;

    @Cacheable(value = "authToken", unless = "#result==null")
    public HttpHeaders getAuthTokenHeader() {
        HttpHeaders headers = new HttpHeaders();

        AuthRequestDto requestCredentials = AuthRequestDto.builder()
                .request(RequestDto.builder()
                        .appId(appId)
                        .clientId(clientId)
                        .secretKey(webSubSecret)
                        .build()).build();
        HttpEntity<AuthRequestDto> entity = new HttpEntity<>(requestCredentials, headers);

        ResponseEntity<String> response = null;
        try {
            response = restTemplateProvider.exchange(webSubHubUrl + Constants.AUTH_URL, HttpMethod.POST, entity, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            LOGGER.error("Get auth token failed: ");
            exception.printStackTrace();
        }
        if (response != null && response.getStatusCode() == HttpStatus.OK) {
            LOGGER.info("Successfully authenticated with mosip");
            return response.getHeaders();
        } else {
            LOGGER.error("Failed getting auth token");
        }
        return null;
    }

    @CacheEvict("authToken")
    @Scheduled(fixedDelay = Constants.CACHE_TOKEN_SECONDS)
    public void cacheEvict() {
    }
}
