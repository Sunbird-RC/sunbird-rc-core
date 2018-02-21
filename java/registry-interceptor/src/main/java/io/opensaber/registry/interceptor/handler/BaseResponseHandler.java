package io.opensaber.registry.interceptor.handler;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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
	
}
