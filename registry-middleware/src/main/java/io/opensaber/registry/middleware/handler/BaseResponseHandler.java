package io.opensaber.registry.middleware.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * 
 * @author jyotsna
 *
 */
public class BaseResponseHandler {
	
	protected HttpServletResponse response;
	protected ResponseWrapper responseWrapper;
	
	public void setResponse(HttpServletResponse response){
		this.response = response;
		responseWrapper = new ResponseWrapper(response);
	}
	
	public ResponseWrapper getResponseWrapper() throws IOException{
		responseWrapper = new ResponseWrapper(response);
		return responseWrapper;
	}
	
	public void writeResponseBody(String content) throws IOException{
		if(responseWrapper==null){
			responseWrapper = getResponseWrapper();
		}
		responseWrapper.writeResponseBody(content);
		response = (HttpServletResponse)responseWrapper.getResponse();
	}

	public HttpServletResponse getResponse(){
		return response;
	}
	
	public String getResponseContent() throws IOException{
		if(responseWrapper==null){
			responseWrapper = getResponseWrapper();
		}
		return responseWrapper.getResponseContent();
	}

}
