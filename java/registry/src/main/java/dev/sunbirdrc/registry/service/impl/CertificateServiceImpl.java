package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.registry.dao.Learner;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.dto.BarCode;
import dev.sunbirdrc.registry.model.dto.FileDto;
import dev.sunbirdrc.registry.model.dto.MailDto;
import dev.sunbirdrc.registry.service.ICertificateService;
import dev.sunbirdrc.registry.util.ClaimRequestClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_CERTIFICATE_SERVICE_NAME;

@Component
public class CertificateServiceImpl implements ICertificateService {
    private final String templateBaseUrl;
    private final String certificateUrl;
    private final String certificateHealthCheckURL;
    private final RestTemplate restTemplate;

    @Value("${claims.url}")
    private String claimRequestUrl;

    @Value("${claims.download-path}")
    private String claimDownloadPath;

    static String static_download_parameter = "?fileName=";

    private static String URL_APPENDER = "/";

    private boolean signatureEnabled;
    private static Logger logger = LoggerFactory.getLogger(CertificateServiceImpl.class);

    private final ClaimRequestClient claimRequestClient;
    public CertificateServiceImpl(@Value("${certificate.templateBaseUrl}") String templateBaseUrl,
                                  @Value("${certificate.apiUrl}") String certificateUrl,
                                  @Value("${signature.enabled}") boolean signatureEnabled,
                                  @Value("${certificate.healthCheckURL}") String certificateHealthCheckURL,
                                  RestTemplate restTemplate,ClaimRequestClient claimRequestClient) {
        this.templateBaseUrl = templateBaseUrl;
        this.certificateUrl = certificateUrl;
        this.restTemplate = restTemplate;
        this.certificateHealthCheckURL = certificateHealthCheckURL;
        this.signatureEnabled = signatureEnabled;
        this.claimRequestClient = claimRequestClient;
    }

    @Override
    public Object getCertificate(JsonNode certificateData, String entityName, String entityId, String mediaType, String templateUrl, JsonNode entity, String fileName, boolean wc) {
        try {
            String finalTemplateUrl = inferTemplateUrl(entityName, mediaType, templateUrl, wc);

            Map<String, Object> requestBody = new HashMap<String, Object>(){{
                put("templateUrl", finalTemplateUrl);
                put("certificate", certificateData.toString());
                put("entityId", entityId);
                put("entityName", entityName);
                put("entity", entity);
                put("credentialsFileName",fileName);
            }};
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", mediaType);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
            return restTemplate.postForObject(certificateUrl, httpEntity, byte[].class);
        } catch (Exception e) {
            logger.error("Get certificate failed", e);
        }
        return null;
    }

    public Object getCertificateForDGL(JsonNode certificateData, String entityName, String entityId, String mediaType, String templateUrl, JsonNode entity, String fileName) {
        try {
            String finalTemplateUrl = inferTemplateUrl(entityName, mediaType, templateUrl, false);

            Map<String, Object> requestBody = new HashMap<String, Object>(){{
                put("templateUrl", finalTemplateUrl);
                put("certificate", certificateData.toString());
                put("entityId", entityId);
                put("entityName", entityName);
                put("entity", entity);
                put("credentialsFileName",fileName);
            }};
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", mediaType);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
            return restTemplate.postForObject(certificateUrl, httpEntity, byte[].class);
        } catch (Exception e) {
            logger.error("Get certificate failed", e);
        }
        return null;
    }

    @NotNull
    private String inferTemplateUrl(String entityName, String mediaType, String templateUrl, boolean wc) {
        if (templateUrl == null) {
            if(wc)
            templateUrl = templateBaseUrl + entityName+"-wc.html";
            else{
                templateUrl = templateBaseUrl + entityName+".html";
            }
        }
        return templateUrl;
    }

    @NotNull
    private String getFileExtension(String mediaType) {
        if (Constants.SVG_MEDIA_TYPE.equals(mediaType)) {
            return ".svg";
        }
        return ".html";
    }

    @Override
    public String getServiceName() {
        return SUNBIRD_CERTIFICATE_SERVICE_NAME;
    }

    @Override
    public ComponentHealthInfo getHealthInfo() {
        if (signatureEnabled) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(URI.create(certificateHealthCheckURL), String.class);
                if (!StringUtils.isEmpty(response.getBody()) && Arrays.asList("UP", "OK").contains(response.getBody().toUpperCase())) {
                    logger.debug("Certificate service running !");
                    return new ComponentHealthInfo(getServiceName(), true);
                } else {
                    return new ComponentHealthInfo(getServiceName(), false);
                }
            } catch (RestClientException ex) {
                logger.error("RestClientException when checking the health of the Sunbird certificate service: ", ex);
                return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, ex.getMessage());
            }
        } else {
            return new ComponentHealthInfo(getServiceName(), true, "SIGNATURE_ENABLED", "false");
        }

    }

    public String saveToGCS(Object certificate, String entityId) {
        String url = null;
        logger.info("Uploading File GCP.");
        url = claimRequestClient.saveFileToGCS(certificate, entityId);
        logger.info("Uploading File GCP complete");
        return url;
    }
//saveFileToGCSForDGL
public String saveforDGL(Object certificate, String entityId) {
    String url = null;
    logger.info("Uploading File GCP.");
    url = claimRequestClient.saveFileToGCSForDGL(certificate, entityId);
    logger.info("Uploading File GCP complete");
    return url;
}
    public BarCode getBarCode(BarCode barCode) {
        BarCode node = null;
        try {
            logger.debug("BarCode start");
            node = claimRequestClient.getBarCode(barCode);
            logger.debug("BarCode end");
            return node;
        } catch (Exception e) {
            logger.error("Get BarCode failed", e);
        }
        return node;
    }


    public byte[]  getCred(String fileName) {
        byte[] bytes = null;
        try {
            logger.info("Track Certificate start");
            bytes = claimRequestClient.getCredentials(fileName);
            logger.info("Track Certificate end");
        } catch (Exception e) {
            logger.error("Track certificate failed", e);
        }

        return bytes;

    }

    public List<String> uploadMultiEntityDocFiles(MultipartFile[] files, String entityName, String entityId) throws Exception {
        if (files == null || files.length == 0) {
            logger.error("Missing files to upload document");
            throw new Exception("Missing files to upload document");
        }

        List<FileDto> fileDtoList = claimRequestClient.uploadCLaimMultipleFiles(files, entityName, entityId);

        if (fileDtoList == null || fileDtoList.isEmpty()) {
            throw new Exception("Unable to file file details while uploading file in claim service");
        }

        List<String> fileUrlList = fileDtoList.stream()
                .map(fileDto -> claimRequestUrl  + claimDownloadPath + static_download_parameter + fileDto.getFileName())
                .collect(Collectors.toList());

        return fileUrlList;
    }

    /**
     * @param file
     * @param entityName
     * @param entityId
     * @return
     * @throws Exception
     */
    public String uploadSingleEntityDocFiles(MultipartFile file, String entityName, String entityId) throws Exception {
        if (file == null) {
            logger.error("Missing file in single file upload document");
            throw new Exception("Missing file in single file upload document");
        }

        List<FileDto> fileDtoList = claimRequestClient.uploadCLaimMultipleFiles(new MultipartFile[]{file}, entityName, entityId);

        if (fileDtoList == null || fileDtoList.isEmpty()) {
            throw new Exception("Unable to file file details while uploading file in claim service");
        }

        Optional<String> fileUrl = fileDtoList.stream()
                .map(fileDto -> claimRequestUrl  + claimDownloadPath + static_download_parameter + fileDto.getFileName())
                .findFirst();

        if (!fileUrl.isPresent()) {
            logger.error("Missing file url after uploading file");
            throw new Exception("Missing file url after uploading file");
        }

        return fileUrl.get();
    }

    public void trackCredentials(Learner learner) {
        try {
            logger.info("Track Certificate start");
            claimRequestClient.saveCredentials(learner);
            logger.info("Track Certificate end");
        } catch (Exception e) {
            logger.error("Track certificate failed", e);
        }

    }

    public void shareCertificateMail(MailDto mail) {
        try {
            logger.info("Sharing Certificate start");
            claimRequestClient.sendMail(mail);
            logger.info("Sharing Certificate end");
        } catch (Exception e) {
            logger.error("Get certificate failed", e);
        }

    }
}
