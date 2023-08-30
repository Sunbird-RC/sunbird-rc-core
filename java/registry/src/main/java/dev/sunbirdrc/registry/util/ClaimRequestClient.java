package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.dto.ClaimDTO;
import dev.sunbirdrc.registry.dao.Learner;
import dev.sunbirdrc.registry.model.dto.*;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;

@Component
public class ClaimRequestClient {
    private static final String GET_CERTIFICATE_NUMBER = "/api/v1/generate-certNumber";
    private static Logger logger = LoggerFactory.getLogger(ClaimRequestClient.class);
    private final String claimRequestUrl;
    private final RestTemplate restTemplate;
    private static final String CLAIMS_PATH = "/api/v1/claims";
    private static final String FETCH_CLAIMS_PATH = "/api/v1/getClaims";
    private static final String TEMPLATE_KEY = "/course-template-key/";
    private static final String FETCH_CLAIMS_STUDENT_PATH = "/api/v2/getClaims";
    private static final String MAIL_SEND_URL = "/api/v1/sendMail";
    private static final String BAR_CODE_API = "/api/v1/barcode";
    private static final String SAVE_CRED_API = "/api/v1/credentials/save";
    private static final String GET_CRED_URL = "/api/v1/files/download?";

    private static final String GET_COURSE_CATEGORY = "/api/v1/courses/diploma";

    private static final String GET_ALL_COURSES = "/api/v1/courses/";
    private static final String PDF = ".pdf";
    private static final String GCS_CODE_API = "/api/v1/files/upload";
    private static final String CLAIM_MULTI_FILE_UPLOAD = "/api/v1/files/upload/multiple";
    private static String URL_APPENDER = "/";

    private static final String MAIL_SEND_PENDING_FOREIGN_ITEM_URL = "/api/v1/sendPendingForeignItemMail/";
    private static final String MAIL_SEND_EC_PENDING_ITEM_URL = "/api/v1/sendEcPendingItemMail/";

    ClaimRequestClient(@Value("${claims.url}") String claimRequestUrl, RestTemplate restTemplate) {
        this.claimRequestUrl = claimRequestUrl;
        this.restTemplate = restTemplate;
    }

    public HashMap<String, Object> riseClaimRequest(ClaimDTO claimDTO) {
        HashMap<String, Object> hashMap = restTemplate.postForObject(claimRequestUrl + CLAIMS_PATH, claimDTO, HashMap.class);
        logger.info("Claim has successfully risen {}", hashMap.toString());
        return hashMap;
    }

    public JsonNode getClaims(JsonNode jsonNode, Pageable pageable, String entityName) {
        final String QUERY_PARAMS = "?size=" + pageable.getPageSize() + "&page="+pageable.getPageNumber();
        ObjectNode requestBody = JsonNodeFactory.instance.objectNode();
        requestBody.set("attestorInfo", jsonNode);
        requestBody.put("entity", entityName);
        return restTemplate.postForObject(claimRequestUrl + FETCH_CLAIMS_PATH + QUERY_PARAMS, requestBody, JsonNode.class);
    }

    public JsonNode getStudentsClaims(JsonNode jsonNode, Pageable pageable, String entityName) {
        final String QUERY_PARAMS = "?size=" + pageable.getPageSize() + "&page="+pageable.getPageNumber();
        ObjectNode requestBody = JsonNodeFactory.instance.objectNode();
        requestBody.set("attestorInfo", jsonNode);
        requestBody.put("entity", entityName);
        return restTemplate.postForObject(claimRequestUrl + FETCH_CLAIMS_STUDENT_PATH + QUERY_PARAMS, requestBody, JsonNode.class);
    }

    public JsonNode getClaim(JsonNode jsonNode, String entityName, String claimId) {
        ObjectNode requestBody = JsonNodeFactory.instance.objectNode();
        requestBody.set("attestorInfo", jsonNode);
        requestBody.put("entity", entityName);
        return restTemplate.postForObject(claimRequestUrl + FETCH_CLAIMS_PATH + "/" + claimId, requestBody, JsonNode.class);
    }

    public ResponseEntity<Object> attestClaim(JsonNode attestationRequest, String claimId) {
        return restTemplate.exchange(
                claimRequestUrl + CLAIMS_PATH + "/" + claimId,
                HttpMethod.POST,
                new HttpEntity<>(attestationRequest),
                Object.class
        );
    }

    public void sendMail(MailDto mail) {
        restTemplate.postForObject(claimRequestUrl + MAIL_SEND_URL, mail, HashMap.class);
        logger.info("Mail has successfully sent ...");
    }

    public String saveFileToGCS(Object certificate, String entityId) {
        String fileName = entityId + PDF;
        logger.info("Credentials File Name."+fileName);
        String url = null;
        byte[] bytes = convertObtToByte(certificate);
        HttpHeaders headers = new HttpHeaders();
        if(bytes!=null){
            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            ResponseEntity<String> response = uploadFileToGCS(headers, resource);
            switch (response.getStatusCode()){
                case OK:
                    url=response.getBody(); // TODO - handle http status
                    break;
                default:
                    break;

            }
        }
        logger.debug("Save to GCS successfully ..."+url);
        return url;
    }

    public String saveFileToGCSForDGL(Object certificate, String entityId) {
        String fileName = entityId;
        logger.info("Credentials File Name."+fileName);
        String url = null;
        byte[] bytes = convertObtToByte(certificate);
        HttpHeaders headers = new HttpHeaders();
        if(bytes!=null){
            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            ResponseEntity<String> response = uploadFileToGCS(headers, resource);
            switch (response.getStatusCode()){
                case OK:
                    url=response.getBody();
                    break;
                default:
                    break;

            }
        }
        logger.debug("Save to GCS successfully ..."+url);
        return url;
    }

    @Nullable
    private ResponseEntity<String> uploadFileToGCS(HttpHeaders headers, ByteArrayResource resource) {
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("accept", MediaType.MULTIPART_FORM_DATA_VALUE);
        String serviceUrl = claimRequestUrl + GCS_CODE_API;
        HttpMethod method = HttpMethod.POST;

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        // Create the HTTP entity with headers and body
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Make the POST request to the service
        RestTemplate restTemplate = new RestTemplate();
        logger.debug("Claim Service url for GCS upload:"+ serviceUrl);
        ResponseEntity<String> response = restTemplate.postForEntity(serviceUrl, requestEntity, String.class);

        return response;
    }

    private byte[] convertObtToByte(Object certificate) {
        byte[] bytes = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream objOutStream = new ObjectOutputStream(bos)) {
                objOutStream.writeObject(certificate);
                objOutStream.flush();
                bytes = bos.toByteArray();
            }
        } catch (Exception e){
            logger.error("Converting certificate file to stream failed.",e);
        }

        return bytes;
    }

    public BarCode getBarCode(BarCode barCode) {
        logger.debug("in getBarCode text::"+barCode.getBarCodeText());
        BarCode node = restTemplate.postForObject(claimRequestUrl + BAR_CODE_API, barCode, BarCode.class);
        logger.debug("BarCode generated ...");
        return node;
    }

    public void saveCredentials(Learner learner) {
        logger.info("in Client::"+"Track certificate");
        String node = restTemplate.postForObject(claimRequestUrl + SAVE_CRED_API, learner, String.class);
        logger.info("in Client certificate saved ...");
    }

    public byte[] getCredentials(String fileName) {
        String requestUrl = claimRequestUrl + GET_CRED_URL;
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("fileName", fileName);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(requestUrl)
                .queryParam("fileName", fileName);
        // Set request headers if needed
        HttpHeaders headers = new HttpHeaders();
        // Add any required headers here
        headers.set("accept", "*/*");
        ResponseEntity<byte[]> response = restTemplate.exchange(
                builder.toUriString(), HttpMethod.POST, null, byte[].class, queryParams, headers
        );        logger.info("end getCredentials ...");
        return response.getBody();
    }

    /**
     * @param files
     * @return
     */
    public List<FileDto> uploadCLaimMultipleFiles(@NonNull MultipartFile[] files, String entityName, String entityId) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (MultipartFile file : files) {
            body.add("files", file.getResource());
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String url = claimRequestUrl + CLAIM_MULTI_FILE_UPLOAD + URL_APPENDER + entityName + URL_APPENDER + entityId;

        logger.debug("Claim Service url for multiple file upload:"+ url);
        ResponseEntity<List<FileDto>> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
                new ParameterizedTypeReference<List<FileDto>>() {});

        List<FileDto> fileDtoList = response.getBody();
        return fileDtoList;
    }

    public String sendPendingForeignItemMail(String claimId) {
        if (!StringUtils.isEmpty(claimId)) {

            ResponseEntity<String> mailStatusResponse = restTemplate.getForEntity(
                    claimRequestUrl + MAIL_SEND_PENDING_FOREIGN_ITEM_URL + claimId, String.class);

            logger.info(">>>>>>>> Pending foreign item mail status: " + mailStatusResponse.getBody());

            return mailStatusResponse.getBody();
        } else {
            logger.error(">>>>>>> Invalid claim id - while calling claim service for foreign pending item (manually)");
        }

        return "Failed to send mail to foreign pending item - invalid claim id";
    }

    public String sendEcPendingItemMail(ManualPendingMailDTO pendingMailDTO) {
        String mailStatus = restTemplate.postForObject(
                claimRequestUrl + MAIL_SEND_EC_PENDING_ITEM_URL, pendingMailDTO, String.class);

        logger.info("Pending item mail status: " + mailStatus);
        return mailStatus;
    }

    public List getCourseCategory(String category) {
        String requestUrl = claimRequestUrl + GET_COURSE_CATEGORY;
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("category", category);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(requestUrl)
                .queryParam("category", category);
        // Set request headers if needed
        HttpHeaders headers = new HttpHeaders();
        // Add any required headers here
        headers.set("accept", "*/*");
        ResponseEntity<List> response = restTemplate.exchange(
                builder.toUriString(), HttpMethod.GET, null, List.class, queryParams, headers
        );        logger.info("end getCredentials ...");
        return response.getBody();
    }


    public List getAllCourses() {
        String requestUrl = claimRequestUrl + GET_ALL_COURSES;
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        // Set request headers if needed
        HttpHeaders headers = new HttpHeaders();
        // Add any required headers here
        headers.set("accept", "*/*");
        ResponseEntity<List> response = restTemplate.exchange(
                requestUrl, HttpMethod.GET, null, List.class, queryParams, headers
        );        logger.info("end getCredentials ...");
        return response.getBody();
    }


    ///api/v1/generate-certNumber

    public Long getCertificateNumber() {
        String requestUrl = claimRequestUrl + GET_CERTIFICATE_NUMBER;
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(requestUrl);
        // Set request headers if needed
        HttpHeaders headers = new HttpHeaders();
        // Add any required headers here
        headers.set("accept", "*/*");
        ResponseEntity<Long> response = restTemplate.exchange(
                builder.toUriString(), HttpMethod.GET, null, Long.class, headers
        );        logger.info("end getCredentials ...");
        return response.getBody();
    }

}
