package dev.sunbirdrc.registry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.pojos.Response;
import dev.sunbirdrc.pojos.ResponseParams;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.transform.ConfigurationHelper;
import dev.sunbirdrc.registry.transform.Transformer;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public abstract class AbstractController {
    private static Logger logger = LoggerFactory.getLogger(AbstractController.class);

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    SunbirdRCInstrumentation watch;

    @Autowired
    RegistryHelper registryHelper;

    @Autowired
    DBConnectionInfoMgr dbConnectionInfoMgr;

    @Autowired
    Transformer transformer;

    @Autowired
    ConfigurationHelper configurationHelper;

    @Autowired
    IDefinitionsManager definitionsManager;

    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    ResponseEntity<Object> badRequestException(ResponseParams responseParams, Response response, String errorMessage) {
        logger.info("Error in handling the invite {}", errorMessage);
        responseParams.setStatus(Response.Status.UNSUCCESSFUL);
        responseParams.setErrmsg(errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    ResponseEntity<Object> internalErrorResponse(ResponseParams responseParams, Response response, Exception ex) {
        logger.info("Error in handling the invite", ex);
        responseParams.setStatus(Response.Status.UNSUCCESSFUL);
        responseParams.setErrmsg("Error occurred");
        if (!StringUtils.isEmpty(ex.getMessage())) {
            responseParams.setErrmsg(ex.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    ResponseEntity<Object> createUnauthorizedExceptionResponse(Exception e) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        responseParams.setErrmsg(e.getMessage());
        responseParams.setStatus(Response.Status.UNSUCCESSFUL);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
}
