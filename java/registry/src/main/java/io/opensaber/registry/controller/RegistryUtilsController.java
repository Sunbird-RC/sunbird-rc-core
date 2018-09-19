package io.opensaber.registry.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.SignatureService;
import io.opensaber.registry.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.util.Map;

@RestController
public class RegistryUtilsController {

    private static Logger logger = LoggerFactory.getLogger(RegistryUtilsController.class);

    private Gson gson = new Gson();
    private Type mapType = new TypeToken<Map<String, Object>>() {
    }.getType();

    @Autowired
    private SignatureService signatureService;

    @Autowired
    private OpenSaberInstrumentation watch;

    @RequestMapping(value = "/utils/sign", method = RequestMethod.POST)
    public ResponseEntity<Response> generateSignature(HttpServletRequest requestModel) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SIGN, "OK", responseParams);

        try {
            Gson gson = new Gson();
            Object payload = gson.fromJson(requestModel.getReader(), Object.class);
            Object result = signatureService.sign(payload);
            response.setResult(JSONUtil.convertObjectJsonMap(result));
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
        } catch (Exception e) {
            logger.error("Error in generating signature", e);
            HealthCheckResponse healthCheckResult =
                    new HealthCheckResponse(Constants.SUNBIRD_SIGNATURE_SERVICE_NAME, false, null);
            response.setResult(JSONUtil.convertObjectJsonMap(healthCheckResult));
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg("");
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/utils/verify", method = RequestMethod.POST)
    public ResponseEntity<Response> verifySignature(HttpServletRequest requestModel) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.VERIFY, "OK", responseParams);

        try {
            Gson gson = new Gson();
            Object payload = gson.fromJson(requestModel.getReader(), Object.class);
            Object result = signatureService.verify(payload);
            response.setResult(JSONUtil.convertObjectJsonMap(result));
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
        } catch (Exception e) {
            logger.error("Error in verifying signature", e);
            HealthCheckResponse healthCheckResult =
                    new HealthCheckResponse(Constants.SUNBIRD_SIGNATURE_SERVICE_NAME, false, null);
            response.setResult(JSONUtil.convertObjectJsonMap(healthCheckResult));
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg("");
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/utils/keys/{id}", method = RequestMethod.GET)
    public ResponseEntity<Response> getKey(@PathVariable String keyId) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.KEYS, "OK", responseParams);

        try {
            Gson gson = new Gson();
            String result = signatureService.getKey(keyId);
            response.setResult(result);
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
        } catch (Exception e) {
            logger.error("Error in getting key ", e);
            HealthCheckResponse healthCheckResult =
                    new HealthCheckResponse(Constants.SUNBIRD_SIGNATURE_SERVICE_NAME, false, null);
            response.setResult(JSONUtil.convertObjectJsonMap(healthCheckResult));
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg("");
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/utils/sign/health", method = RequestMethod.GET)
    public ResponseEntity<Response> health() {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.HEALTH, "OK", responseParams);

        try {
            boolean healthCheckResult = signatureService.isServiceUp();
            HealthCheckResponse healthCheck = new HealthCheckResponse(Constants.SUNBIRD_SIGNATURE_SERVICE_NAME, healthCheckResult, null);
            response.setResult(JSONUtil.convertObjectJsonMap(healthCheck));
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            logger.debug("Application heath checked : ", healthCheckResult);
        } catch (Exception e) {
            logger.error("Error in health checking!", e);
            HealthCheckResponse healthCheckResult =
                    new HealthCheckResponse(Constants.SUNBIRD_SIGNATURE_SERVICE_NAME, false, null);
            response.setResult(JSONUtil.convertObjectJsonMap(healthCheckResult));
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg("Error during health check");
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
