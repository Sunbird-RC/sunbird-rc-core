package io.opensaber.registry.middleware.handler;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * 
 * @author jyotsna
 *
 */
public class BaseRequestHandler{
	
	protected RequestWrapper requestWrapper;
	protected HttpServletRequest request;
	
	public void setRequest(HttpServletRequest request) throws IOException{
		this.request = request;
		requestWrapper = new RequestWrapper(request);
	}
	
	public RequestWrapper getRequestWrapper() throws IOException{
		requestWrapper = new RequestWrapper(request);
		return requestWrapper;
	}
	
	public String getRequestBody() throws IOException{
		if(requestWrapper==null){
			requestWrapper = getRequestWrapper();
		}
		return requestWrapper.getBody();
	}
	
	public String getRequestHeaderByName(String name) throws IOException{
		if(requestWrapper==null){
			requestWrapper = getRequestWrapper();
		}
		return requestWrapper.getHeader(name);
	}
	
	public void setRequestAttributes(Map<String,Object> attributeMap){
		for(Map.Entry<String, Object> entry: attributeMap.entrySet()){
			request.setAttribute(entry.getKey(), entry.getValue());
		}
	}
	
	public HttpServletRequest getRequest(){
		return request;
	}

}
