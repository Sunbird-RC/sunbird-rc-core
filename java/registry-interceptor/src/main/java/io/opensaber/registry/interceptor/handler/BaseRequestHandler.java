package io.opensaber.registry.interceptor.handler;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.JsonParser;
import org.apache.jena.rdf.model.Model;

import com.google.gson.Gson;
import io.opensaber.pojos.Request;
import io.opensaber.pojos.RequestParams;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.middleware.util.Constants;
import static org.apache.commons.lang3.StringUtils.*;

public class BaseRequestHandler extends BaseResponseHandler{

	protected RequestWrapper requestWrapper;
	protected HttpServletRequest request;

	public void setRequest(HttpServletRequest request) throws IOException {
		this.request = request;
		setRequestWrapper();
	}


	public void setRequestWrapper() throws IOException {
		requestWrapper = new RequestWrapper(request);
	}

	public String getRequestBody() throws IOException{
		return requestWrapper.getBody();
	}

	public String getRequestHeaderByName(String name) throws IOException {
		return requestWrapper.getHeader(name);
	}

	public void mergeRequestAttributes(Map<String, Object> attributeMap) {
		if (attributeMap != null) {
			for (Map.Entry<String, Object> entry : attributeMap.entrySet()) {
				if (null == request.getAttribute(entry.getKey())) {
					request.setAttribute(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	public HttpServletRequest getRequest(){
		return request;
	}

	public Map<String, Object> getRequestBodyMap() throws IOException {
		Map<String, Object> requestBodyMap = new HashMap<>();
		Gson gson = new Gson();
		String requestBody = getRequestBody();
		Request requestModel = gson.fromJson(requestBody, Request.class);
		requestBodyMap.put(Constants.REQUEST_ATTRIBUTE, requestModel);
		String requestObject = new JsonParser().parse(requestBody)
				.getAsJsonObject().getAsJsonObject("request").toString();
		requestBodyMap.put(Constants.ATTRIBUTE_NAME, requestObject);
		requestModel.setRequestMap(requestBodyMap);
		return requestBodyMap;
	}

	public Map<String, Object> getRequestHeaderMap() throws IOException {
		Map<String, Object> requestHeaderMap = new HashMap<>();
		Enumeration<String> headerNames = request.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String header = headerNames.nextElement();
				requestHeaderMap.put(header, request.getHeader(header));
			}
		}
		return requestHeaderMap;
	}


	public Map<String,Object> getRequestAttributeMap() throws IOException{
		boolean isMethodOriginAdded = false;
		Map<String,Object> requestAttributeMap = new HashMap<String,Object>();
		Enumeration<String> attributeNames = request.getAttributeNames();
		if (attributeNames != null) {
			while (attributeNames.hasMoreElements()) {
				String attribute = attributeNames.nextElement();
				if(attribute.equalsIgnoreCase(Constants.METHOD_ORIGIN)){
					isMethodOriginAdded = true;
				}
				requestAttributeMap.put(attribute, request.getAttribute(attribute));
			}
			if(!isMethodOriginAdded){
				requestAttributeMap.put(Constants.METHOD_ORIGIN, getRequestPath());
			}
			
		}
		return requestAttributeMap;
	}

	public Map<String, Object> getRequestParameterMap() throws IOException {
		Map<String, Object> requestParameterMap = new HashMap<>();
		requestParameterMap.putAll(request.getParameterMap());
		return requestParameterMap;
	}
	
	public String getRequestPath() throws IOException{
		return request.getServletPath();
	}

}
