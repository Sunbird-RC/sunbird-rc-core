package io.opensaber.registry.controller;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import io.opensaber.pojos.Entity;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.pojos.Request;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import io.opensaber.registry.service.SignatureService;
import io.opensaber.registry.util.JSONUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class RegistryUtilsController {
	
	private static final String ID_REGEX = "\"@id\"\\s*:\\s*\"[a-z]+:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\",";
	//private static final String ID_REGEX2 = "\"@id\"\\s*:\\s*\".*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\",";
	private static final String ID_REGEX2 = "\"@id\"\\s*:\\s*\"_:[a-z][0-9]+\",";

    private static Logger logger = LoggerFactory.getLogger(RegistryUtilsController.class);

    private Gson gson = new Gson();
    private Type mapType = new TypeToken<Map<String, Object>>() {
    }.getType();

    @Autowired
    private SignatureService signatureService;

	@Autowired
	private OpenSaberInstrumentation watch;
	
	@Value("${registry.context.base}")
    private String registryContext;
	
	@Value("${frame.file}")
    private String frameFile;


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
    public ResponseEntity<Response> verifySignature(HttpServletRequest request) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.VERIFY, "OK", responseParams);

        try {
            BaseRequestHandler baseRequestHandler = new BaseRequestHandler();
            baseRequestHandler.setRequest(request);
            Map<String,Object> map = baseRequestHandler.getRequestBodyMap();
            if(map.containsKey(Constants.REQUEST_ATTRIBUTE) && map.containsKey(Constants.ATTRIBUTE_NAME)){
            	String payload  = (String)map.get(Constants.ATTRIBUTE_NAME);
            	JsonObject obj = gson.fromJson(payload, JsonObject.class);
            	Entity entity = gson.fromJson(obj.get("entity"), Entity.class);
            	String jsonldToExpand = gson.toJson(entity.getClaim());
            	//jsonldToExpand = JSONUtil.getStringWithReplacedText(jsonldToExpand, ID_REGEX, StringUtils.EMPTY);
            	InputStream is = this.getClass().getClassLoader().getResourceAsStream(frameFile);
				String fileString = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
            	Map<String,Object> framedJsonLD = JSONUtil.frameJsonAndRemoveIds(ID_REGEX, jsonldToExpand, gson, fileString);
            	entity.setClaim(framedJsonLD);
            	Map verifyReq  = new HashMap<String, Object>();
            	verifyReq.put("entity", gson.fromJson(gson.toJson(entity),mapType));
                Object result = signatureService.verify(verifyReq);
                response.setResult(result);
                responseParams.setErrmsg("");
                responseParams.setStatus(Response.Status.SUCCESSFUL);
            }else{
            	responseParams.setStatus(Response.Status.UNSUCCESSFUL);
                responseParams.setErrmsg("");
            }
            
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
