package dev.sunbirdrc.plugin.services;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.plugin.constant.Constants;
import dev.sunbirdrc.plugin.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@ConditionalOnExpression("#{(environment.MOSIP_ENABLED?:'false').equals('true')}")
@Service
public class MosipServices {
    
    @Autowired
    private RestTemplate restTemplateProvider;

    private Logger LOGGER = LoggerFactory.getLogger(MosipServices.class);

    @Value("${mosip.event.hubURL:#{environment.HUB_URL}}")
    private String webSubHubUrl;

    @Value("${mosip.issuer:SunbirdRC}")
    private String issuer;

    @Value("${mosip.event.secret:#{environment.EVENT_SECRET}}")
    private String webSubSecret;

    @Value("${mosip.event.callBackUrl:#{environment.CALLBACK_URL}}")
    private String callBackUrl;

    @Value("${mosip.event.topic:SunbirdRC/CREDENTIAL_ISSUED}")
    private String topic;

    @Value("${mosip.print.url:#{environment.PRINT_URL}}")
    private String printUrl;

    @Value("${mosip.enabled:#{environment.MOSIP_ENABLED}}")
    private Boolean mosipEnabled = Boolean.FALSE;

    @Autowired
    private MosipAuthService mosipAuthService;

    public void initSubscriptions() {
        if (mosipEnabled) {
            LOGGER.info("Initializing subscriptions..");
            registerTopic();
            subscribeForPrintServiceEvents();
        }
    }

    @Scheduled(fixedDelay = Constants.SUBSCRIBE_RETRY_SECONDS, initialDelay = Constants.SUBSCRIBE_RETRY_SECONDS)
    void subscribeForPrintServiceEvents() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(Constants.HUB_MODE, Constants.SUBSCRIBE);
        map.add(Constants.HUB_TOPIC, topic);
        map.add(Constants.HUB_CALLBACK, callBackUrl.concat("?intentMode=").concat(Constants.SUBSCRIBE));
        map.add(Constants.HUB_SECRET, webSubSecret);
        map.add(Constants.HUB_LEASE_SECONDS, Constants.LEASE_SECONDS);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        ResponseEntity<String> response = null;
        try {
            response = restTemplateProvider.exchange(webSubHubUrl + Constants.SUBSCRIBE_URL, HttpMethod.POST, entity, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            LOGGER.error("Subscription to topic failed: ");
            exception.printStackTrace();
        }
        if (response != null && response.getStatusCode() == HttpStatus.ACCEPTED) {
            LOGGER.info("subscribed for topic {} at hub", topic);
        } else {
            LOGGER.error("Subscription to topic");
        }
    }

    private void registerTopic() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(Constants.HUB_MODE, Constants.REGISTER);
        map.add(Constants.HUB_TOPIC, topic);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        ResponseEntity<String> response = null;
        try {
            response = restTemplateProvider.exchange(webSubHubUrl + Constants.REGISTER_URL, HttpMethod.POST, entity, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            LOGGER.error("Register topic failed: ");
            exception.printStackTrace();
        }
        if (response != null && response.getStatusCode() == HttpStatus.ACCEPTED) {
            LOGGER.info("topic {} registered at hub", topic);
        } else {
            LOGGER.error("Failed registering topic");
        }
    }

    public Object generateOTP(SendOTPDto otpDto) {
        long transactionId = getRandomTransactionId();
        RequestOTPDto requestOTPDto = RequestOTPDto.builder()
                .id(Constants.MOSIP_IDENTITY_OTP_INTERNAL)
                .individualId(otpDto.getUid())
                .individualIdType(Constants.UIN)
                .metadata(new HashMap<>())
                .otpChannel(Collections.singletonList(Constants.EMAIL))
                .requestTime(getISODate())
                .version(Constants.VERSION)
                .transactionID(Long.toString(transactionId))
                .build();
        HttpEntity<RequestOTPDto> entity = new HttpEntity<>(requestOTPDto, mosipAuthService.getAuthTokenHeader());

        ResponseEntity<Object> response = null;
        try {
            response = restTemplateProvider.exchange(webSubHubUrl + Constants.OTP_URL, HttpMethod.POST, entity, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            LOGGER.error("Generate otp failed failed: ");
            exception.printStackTrace();
        }
        if (response != null && response.getStatusCode() == HttpStatus.OK) {
            LOGGER.info("Successfully generated otp");
            return response.getBody();
        } else {
            LOGGER.error("Failed generating otp");
        }
        return null;
    }

    private long getRandomTransactionId() {
        return (long) Math.floor(Math.random() * 9_000_000_000L) + 1_000_000_000L;
    }

    private String getISODate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());
    }

    public Object fetchCredentials(FetchCredentialsDto fetchCredentialsDto) {
        RequestCredentialsDto requestCredentialsDto = RequestCredentialsDto.builder()
                .id(Constants.MOSIP_IDENTITY_OTP_INTERNAL)
                .request(CredentialsRequestDto.builder()
                        .additionalData(new HashMap(){{
                            put("osid", fetchCredentialsDto.getOsid());
                            put("attestationOsid", fetchCredentialsDto.getAttestationOsid());
                        }})
                        .individualId(fetchCredentialsDto.getUid())
                        .issuer(issuer)
                        .otp(fetchCredentialsDto.getOtp())
                        .transactionID(fetchCredentialsDto.getTransactionId())
                        .build())
                .version(Constants.VERSION)
                .requesttime(getISODate())
                .build();
        HttpEntity<RequestCredentialsDto> entity = new HttpEntity<>(requestCredentialsDto, mosipAuthService.getAuthTokenHeader());

        ResponseEntity<Object> response = null;
        try {
            response = restTemplateProvider.exchange(webSubHubUrl + Constants.CREDENTIALS_URL, HttpMethod.POST, entity, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            LOGGER.error("Generate credentials failed failed: ");
            exception.printStackTrace();
        }
        if (response != null && response.getStatusCode() == HttpStatus.OK) {
            LOGGER.info("Successfully generated credentials");
            return response.getBody();
        } else {
            LOGGER.error("Failed generating credentials");
        }
        return null;
    }

    public byte[] fetchMosipPdf(Map<String, String> requestHeaders, String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        for (Map.Entry<String, String> requestHeader: requestHeaders.entrySet()) {
            headers.add(requestHeader.getKey(), requestHeader.getValue());
        }
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<byte[]> response = null;
        try {
            response = restTemplateProvider.exchange(printUrl + Constants.PRINT_PDF_URL, HttpMethod.POST, entity, byte[].class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            LOGGER.error("Failed fetching pdf failed: ");
            exception.printStackTrace();
        }
        LOGGER.error("Failed fetching pdf failed: ");
        return null;
    }
}
