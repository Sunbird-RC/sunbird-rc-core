package io.opensaber.registry.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.opensaber.pojos.*;
import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.SignatureService;
import io.opensaber.registry.util.JSONUtil;
import io.opensaber.registry.util.ResponseUtil;
import org.apache.jena.ext.com.google.common.io.ByteStreams;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class RegistryUtilsController {

    private static final String ID_REGEX = "\"@id\"\\s*:\\s*\"[a-z]+:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\",";

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
            BaseRequestHandler baseRequestHandler = new BaseRequestHandler();
            baseRequestHandler.setRequest(requestModel);
            Map<String,Object> requestBodyMap = baseRequestHandler.getRequestBodyMap();
            if(requestBodyMap.containsKey(Constants.REQUEST_ATTRIBUTE) && requestBodyMap.containsKey(Constants.ATTRIBUTE_NAME)
                    && ResponseUtil.checkApiId((Request)requestBodyMap.get(Constants.REQUEST_ATTRIBUTE),Response.API_ID.SIGN.getId())){
                Object result = signatureService.sign(gson.fromJson(requestBodyMap.get(Constants.ATTRIBUTE_NAME).toString(),mapType));
                response.setResult(result);
                responseParams.setErrmsg("");
                responseParams.setStatus(Response.Status.SUCCESSFUL);
            } else {
                responseParams.setStatus(Response.Status.UNSUCCESSFUL);
                responseParams.setErrmsg("");
            }
        } catch (Exception e) {
            logger.error("Error in generating signature", e);
            response.setResult(null);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(Constants.SIGN_ERROR_MESSAGE);
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
            	JsonObject obj = gson.fromJson(map.get(Constants.ATTRIBUTE_NAME).toString(),JsonObject.class);

            	JsonArray arr = new JsonArray();
            	JsonElement entityElement = obj.get("entity");
            	if (!entityElement.isJsonArray()) {
                    arr.add(entityElement);
                } else {
            	    arr = entityElement.getAsJsonArray();
                }

                Map verifyReq = new HashMap<String, Object>();
                List<Entity> entityList = new ArrayList<Entity>();

                for(int i = 0; i < arr.size(); i++) {
                    JsonObject element = arr.get(i).getAsJsonObject();
                    Entity entity = gson.fromJson(element, Entity.class);
                    JsonElement claimObj = gson.fromJson(element.get("claim"), JsonElement.class);

                    if (claimObj != null && claimObj.isJsonObject()) {
                        String claimJson = claimObj.toString();
                        if (claimJson.contains(Constants.CONTEXT_KEYWORD)) {
                            InputStream is = this.getClass().getClassLoader().getResourceAsStream(frameFile);
                            String fileString = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
                            Map<String, Object> framedJsonLD = JSONUtil.frameJsonAndRemoveIds(ID_REGEX, claimJson, gson, fileString);
                            entity.setClaim(framedJsonLD);
                        } else {
                            entity.setClaim(claimObj);
                        }
                    } else {
                        entity.setClaim(claimObj.getAsString());
                    }
                    entityList.add(entity);
                }
                // We dont want the callers to be aware of the internal arr
                if (entityList.size() == 1) {
                    verifyReq.put("entity", gson.fromJson(gson.toJson(entityList.get(0)), mapType));
                } else {
                    verifyReq.put("entity", gson.fromJson(gson.toJson(entityList), ArrayList.class));
                }

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
            response.setResult(null);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(Constants.VERIFY_SIGN_ERROR_MESSAGE);
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
            response.setResult(null);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(Constants.KEY_RETRIEVE_ERROR_MESSAGE);
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
