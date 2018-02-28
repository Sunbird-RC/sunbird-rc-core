package io.opensaber.registry.interceptor;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.opensaber.registry.interceptor.handler.BaseResponseHandler;
import io.opensaber.registry.middleware.impl.RdfToJsonldConverter;
import io.opensaber.registry.middleware.util.Constants;

@Component
public class RdfToJsonldInterceptor extends BaseResponseHandler implements HandlerInterceptor{
	
	private RdfToJsonldConverter rdfToJsonldConverter;
	
	@Autowired
	public RdfToJsonldInterceptor(RdfToJsonldConverter rdfToJsonldConverter){
		this.rdfToJsonldConverter = rdfToJsonldConverter;
	}

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object object) throws Exception {
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request,HttpServletResponse response,
	  Object handler, ModelAndView modelAndView) throws Exception {
		setResponse(response);
		String responseBody = getResponseContent();
		Map<String,Object> responseMap = new HashMap<String,Object>();
		responseMap.put(Constants.RESPONSE_ATTRIBUTE_NAME, responseBody);
		responseMap = rdfToJsonldConverter.execute(responseMap);
		if(responseMap.get(Constants.RESPONSE_ATTRIBUTE_NAME)!=null){
			writeResponseBody(responseMap.get(Constants.RESPONSE_ATTRIBUTE_NAME).toString());
		}
		response = getResponse();		
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
	  Object handler, Exception ex) {

	}

}
