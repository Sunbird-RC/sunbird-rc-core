package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.Gson;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.pojos.UniqueIdentifierField;
import dev.sunbirdrc.registry.exception.CustomException;
import dev.sunbirdrc.registry.exception.UniqueIdentifierException.*;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.service.IIdGenService;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_ID_GEN_SERVICE_NAME;

@Service
@ConditionalOnProperty(name = "idgen.enabled", havingValue = "true")
public class IdGenService implements IIdGenService {
    private static final Logger logger = LoggerFactory.getLogger(IdGenService.class);

    @Value("${idgen.generateURL}")
    private String generateUrl;
    @Value("${idgen.idFormatURL}")
    private String idFormatUrl;
    @Value("${idgen.healthCheckURL}")
    private String healthCheckUrl;
    @Value("${idgen.tenantId}")
    private String tenantId;
    @Value("${idgen.enabled:false}")
    private boolean enabled;

    @Autowired
    private Gson gson;
    @Autowired
    private SunbirdRCInstrumentation watch;
    @Autowired
    private RetryRestTemplate retryRestTemplate;

    @Override
    public Map<String, String> generateId(List<UniqueIdentifierField> uniqueIdentifierFields) throws CustomException {
        if(!enabled) throw new UnreachableException("IDGEN service not enabled");
        HttpEntity<String> entity = getIdgenRequest(uniqueIdentifierFields);

        try {
            watch.start("IdGenServiceImpl.generateId");
            ResponseEntity<String> response = retryRestTemplate.postForEntity(generateUrl, entity);
            watch.stop("IdGenServiceImpl.generateId");
            JsonNode results = JSONUtil.convertStringJsonNode(response.getBody());
            if("SUCCESSFUL".equals(results.at("/responseInfo/status").asText())) {
                logger.info("Generated value successfully");
                Map<String, String> resultMap = new HashMap<>();
                Iterator<JsonNode> iterator = ((ArrayNode) results.at("/idResponses")).elements();
                int i = 0;
                while(iterator.hasNext()) {
                    resultMap.put(uniqueIdentifierFields.get(i).getField(), iterator.next().at("/id").asText());
                    i++;
                }
                return resultMap;
            }
            throw new GenerateException(results.at("/idResponses").asText());
        } catch (ResourceAccessException e) {
            logger.error("Exception while connecting {} : {}", getServiceName(), ExceptionUtils.getStackTrace(e));
            throw new UnreachableException("Exception while connecting idgen service ! ");
        } catch (Exception e) {
            logger.error("Exception in {}: {}", getServiceName(), ExceptionUtils.getStackTrace(e));
            throw new GenerateException("Exception occurred while generating id");
        }
    }

    @Override
    public void saveIdFormat(List<UniqueIdentifierField> uniqueIdentifierFields) throws CustomException {
        if(!enabled) throw new UnreachableException("IDGEN service not enabled");
        HttpEntity<String> entity = getIdgenRequest(uniqueIdentifierFields);

        try {
            watch.start("IdGenServiceImpl.saveFormat");
            ResponseEntity<String> response = retryRestTemplate.postForEntity(idFormatUrl, entity);
            watch.stop("IdGenServiceImpl.saveFormat");
            JsonNode results = JSONUtil.convertStringJsonNode(response.getBody());
            if(!"SUCCESSFUL".equals(results.at("/responseInfo/status").asText())) {
                Iterator<JsonNode> iterator = ((ArrayNode) results.at("/errorMsgs")).elements();
                while(iterator.hasNext()) {
                    JsonNode node = iterator.next();
                    if (node.isNull()) continue;
                    if (!node.asText().contains("already exists")) {
                        throw new IdFormatException(node.asText());
                    }
                }
            }
        } catch (ResourceAccessException e) {
            logger.error("Exception while connecting {} : {}", getServiceName(), ExceptionUtils.getStackTrace(e));
            throw new UnreachableException("Exception while connecting idgen service ! ");
        } catch (Exception e) {
            logger.error("Exception in {}: {}", getServiceName(), ExceptionUtils.getStackTrace(e));
            throw new GenerateException("Exception occurred while generating id");
        }
    }

    private HttpEntity<String> getIdgenRequest(List<UniqueIdentifierField> uniqueIdentifierFields) {
        Map<String, Object> map = new HashMap<>();
        map.put("RequestInfo", new HashMap<>());
        List<Map<String, String>> idRequests = uniqueIdentifierFields.stream().map(field -> {
            Map<String, String> idRequest = new HashMap<>();
            idRequest.put("idName", field.getIdName());
            idRequest.put("tenantId", tenantId);
            idRequest.put("format", field.getFormat());
            return idRequest;
        }).collect(Collectors.toList());

        map.put("idRequests", idRequests);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(gson.toJson(map), headers);
    }

    @Override
    public String getServiceName() {
        return SUNBIRD_ID_GEN_SERVICE_NAME;
    }

    @Override
    public ComponentHealthInfo getHealthInfo() {
        if (enabled) {
            try {
                ResponseEntity<String> response = retryRestTemplate.getForEntity(healthCheckUrl);
                if (!StringUtils.isEmpty(response.getBody()) && JSONUtil.convertStringJsonNode(response.getBody()).get("status").asText().equalsIgnoreCase("UP")) {
                    logger.debug(" running !");
                    return new ComponentHealthInfo(getServiceName(), true);
                } else {
                    return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, response.getBody());
                }
            } catch (RestClientException | IOException ex) {
                logger.error("RestClientException when checking the health of the idgen service: {}", ExceptionUtils.getStackTrace(ex));
                return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, ex.getMessage());
            }
        } else {
            return new ComponentHealthInfo(getServiceName(), true, "IDGEN_ENABLED", "false");
        }
    }
}
