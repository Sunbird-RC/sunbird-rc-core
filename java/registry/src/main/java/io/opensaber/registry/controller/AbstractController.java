package io.opensaber.registry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.helper.RegistryHelper;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.transform.ConfigurationHelper;
import io.opensaber.registry.transform.Transformer;
import io.opensaber.registry.util.DefinitionsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public abstract class AbstractController {
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    OpenSaberInstrumentation watch;

    @Autowired
    RegistryHelper registryHelper;

    @Autowired
    DBConnectionInfoMgr dbConnectionInfoMgr;

    @Autowired
    Transformer transformer;

    @Autowired
    ConfigurationHelper configurationHelper;

    @Autowired
    DefinitionsManager definitionsManager;

    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    ResponseEntity<Object> badRequestException(ResponseParams responseParams, Response response, String errorMessage) {
        responseParams.setStatus(Response.Status.UNSUCCESSFUL);
        responseParams.setErrmsg(errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    ResponseEntity<Object> createUnauthorizedExceptionResponse(Exception e) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        responseParams.setErrmsg(e.getMessage());
        responseParams.setStatus(Response.Status.UNSUCCESSFUL);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
}
