package io.opensaber.registry.interceptor.handler;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.google.gson.Gson;

import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;

/**
 * 
 * @author jyotsna
 *
 */
public class BaseResponseHandler {
	
	protected HttpServletResponse response;
	protected ResponseWrapper responseWrapper;
	
	public void setResponse(HttpServletResponse response) throws IOException{
		this.response = response;
		setResponseWrapper();
	}
	
	public void setResponseWrapper() throws IOException{
		if(responseWrapper==null){
			responseWrapper = new ResponseWrapper(response);
		}
	}
	
	public void writeResponseBody(String content) throws IOException{
		setResponseWrapper();
		responseWrapper.writeResponseBody(content);
		response = (HttpServletResponse)responseWrapper.getResponse();
	}

	public HttpServletResponse getResponse(){
		return response;
	}
	
	public String getResponseContent() throws IOException{
		setResponseWrapper();
		return responseWrapper.getResponseContent();
	}
	
	public Map<String,Object> getResponseHeaderMap() throws IOException{
		setResponseWrapper();
		Map<String,Object> responseHeaderMap = new HashMap<String,Object>();
		Collection<String> headerNames = responseWrapper.getHeaderNames();
		if(headerNames!=null){
			for(String header: headerNames){
				responseHeaderMap.put(header, responseWrapper.getHeader(header));
			}
		}
		return responseHeaderMap;
	}
	
	public void writeResponseObj(Gson gson, String errMessage) throws IOException{
		response.setStatus(HttpStatus.OK.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		writeResponseBody(gson.toJson(setErrorResponse(errMessage)));
	}
	
	public Response setErrorResponse(String message){
		Response response = new Response();
		ResponseParams responseParams = new ResponseParams();
		response.setId("");
		response.setEts(System.currentTimeMillis() / 1000L);
		response.setVer("1.0");
		response.setParams(responseParams);
		responseParams.setMsgid(UUID.randomUUID().toString());
		responseParams.setErr("");
		responseParams.setResmsgid("");
		response.setResponseCode("OK");
		Map<String, Object> result = new HashMap<>();
		response.setResult(result);
		responseParams.setStatus(Response.Status.UNSUCCESSFUL);
		responseParams.setErrmsg(message);
		return response;
	}
}
