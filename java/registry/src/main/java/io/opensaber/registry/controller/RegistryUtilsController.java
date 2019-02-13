package io.opensaber.registry.controller;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.Entity;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.service.SignatureService;
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
	private APIMessage apiMessage;

	@Autowired
	private OpenSaberInstrumentation watch;

	@Value("${frame.file}")
	private String frameFile;

	@RequestMapping(value = "/utils/sign", method = RequestMethod.POST)
	public ResponseEntity<Response> generateSignature(HttpServletRequest requestModel) {
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.SIGN, "OK", responseParams);

		try {
			watch.start("RegistryUtilsController.generateSignature");
			Map<String, Object> requestBodyMap = apiMessage.getRequest().getRequestMap();
			if (null !=requestBodyMap && (requestBodyMap.containsKey(Constants.SIGN_ENTITY) || requestBodyMap.containsKey(Constants.SIGN_VALUE))){
				Object result = signatureService
						.sign(requestBodyMap);
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
		finally {
			watch.stop("RegistryUtilsController.generateSignature");
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/utils/verify", method = RequestMethod.POST)
	public ResponseEntity<Response> verifySignature(HttpServletRequest request) {
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.VERIFY, "OK", responseParams);

		try {
			watch.start("RegistryUtilsController.verifySignature");
			Map<String, Object> map = apiMessage.getRequest().getRequestMap();
			String inputEntity = apiMessage.getRequest().getRequestMapAsString();
			if (null != inputEntity && null != map && map.containsKey(Constants.SIGN_ENTITY)) {
				JsonObject obj = gson.fromJson(inputEntity, JsonObject.class);
				JsonArray arr = new JsonArray();
				JsonElement entityElement = obj.get(Constants.SIGN_ENTITY);
				if (!entityElement.isJsonArray()) {
					arr.add(entityElement);
				} else {
					arr = entityElement.getAsJsonArray();
				}

				Map verifyReq = new HashMap<String, Object>();
				List<Entity> entityList = new ArrayList<Entity>();

				for (int i = 0; i < arr.size(); i++) {
					JsonObject element = arr.get(i).getAsJsonObject();
					Entity entity = gson.fromJson(element, Entity.class);
					JsonElement claimObj = gson.fromJson(element.get("claim"), JsonElement.class);

					if (claimObj != null && claimObj.isJsonObject()) {
						String claimJson = claimObj.toString();
						if (claimJson.contains(Constants.CONTEXT_KEYWORD)) {
							InputStream is = this.getClass().getClassLoader().getResourceAsStream(frameFile);
							String fileString = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
							Map<String, Object> framedJsonLD = JSONUtil.frameJsonAndRemoveIds(ID_REGEX, claimJson, gson,
									fileString);
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
			} else {
				responseParams.setStatus(Response.Status.UNSUCCESSFUL);
				responseParams.setErrmsg("");
			}
		} catch (Exception e) {
			logger.error("Error in verifying signature", e);
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(Constants.VERIFY_SIGN_ERROR_MESSAGE);
		}
		finally {
			watch.stop("RegistryUtilsController.verifySignature");
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/utils/keys/{id}", method = RequestMethod.GET)
	public ResponseEntity<Response> getKey( @PathVariable("id") String keyId) {
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.KEYS, "OK", responseParams);

		try {
			watch.start("RegistryUtilsController.getKey");
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
		finally {
			watch.stop("RegistryUtilsController.getKey");
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/utils/sign/health", method = RequestMethod.GET)
	public ResponseEntity<Response> health() {
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.HEALTH, "OK", responseParams);

		try {
			boolean healthCheckResult = signatureService.isServiceUp();
			HealthCheckResponse healthCheck = new HealthCheckResponse(Constants.SUNBIRD_SIGNATURE_SERVICE_NAME,
					healthCheckResult, null);
			response.setResult(JSONUtil.convertObjectJsonMap(healthCheck));
			responseParams.setErrmsg("");
			responseParams.setStatus(Response.Status.SUCCESSFUL);
			logger.debug("Application heath checked : ", healthCheckResult);
		} catch (Exception e) {
			logger.error("Error in health checking!", e);
			HealthCheckResponse healthCheckResult = new HealthCheckResponse(Constants.SUNBIRD_SIGNATURE_SERVICE_NAME,
					false, null);
			response.setResult(JSONUtil.convertObjectJsonMap(healthCheckResult));
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Error during health check");
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
