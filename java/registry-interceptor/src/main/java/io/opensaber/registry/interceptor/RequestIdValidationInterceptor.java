package io.opensaber.registry.interceptor;

import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.RequestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class RequestIdValidationInterceptor implements HandlerInterceptor {
	private Map<String, String> requestIdMap;

	@Autowired
	private APIMessage apiMessage;

	public RequestIdValidationInterceptor(Map requestIdMap) {
		this.requestIdMap = requestIdMap;
	}

	/**
	 * This method checks for each request it contains a valid request id for
	 * accessing the api
	 * 
	 * @param request
	 * @param response
	 * @param handler
	 * @return true or false
	 * @throws Exception
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		RequestWrapper wrapper = apiMessage.getRequestWrapper();
		String expectedAPI = requestIdMap.getOrDefault(wrapper.getRequestURI(), "");

		return !expectedAPI.isEmpty() && (apiMessage.getRequest().getId().compareTo(expectedAPI) == 0);
	}

}
