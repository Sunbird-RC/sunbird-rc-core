package io.opensaber.registry.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseUtil {

	private static ObjectMapper objectMapper;

	@Autowired
	public ResponseUtil(ObjectMapper objectMapper) {
		ResponseUtil.objectMapper = objectMapper;
	}

	/**
	 * 
	 * @param obj
	 * @return
	 * @throws JsonProcessingException
	 */
	public static ResponseEntity successResponse(Object obj) throws JsonProcessingException {
		ObjectNode response = objectMapper.createObjectNode();
		if (obj != null) {
			response.put(JsonKeys.RESPONSE, obj.toString());
		}
		ResponseEntity responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
		return responseEntity;
	}

	public static ResponseEntity successResponse() throws JsonProcessingException {
		ObjectNode response = objectMapper.createObjectNode();
		response.put(JsonKeys.RESPONSE, JsonKeys.SUCCESS);
		ResponseEntity responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
		return responseEntity;
	}

	public static ResponseEntity failureResponse(String message) throws JsonProcessingException {
		ObjectNode response = objectMapper.createObjectNode();
		if (message != null) {
			response.put(JsonKeys.RESPONSE, message);
		}
		ResponseEntity responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
		return responseEntity;
	}
}
