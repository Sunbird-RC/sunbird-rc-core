package dev.sunbirdrc.registry.interceptor;

import dev.sunbirdrc.pojos.APIMessage;
import dev.sunbirdrc.pojos.RequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public class RequestIdValidationInterceptor implements HandlerInterceptor {
	private Map<String, String> requestIdMap;
	public static final Logger logger = LoggerFactory.getLogger(RequestIdValidationInterceptor.class);

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

		boolean validRequest = !expectedAPI.isEmpty() && (apiMessage.getRequest().getId().compareTo(expectedAPI) == 0);
		if (!validRequest) {
			logger.error("Request id doesnt match the expected format: {}", apiMessage.getRequest().getId());
		}
		return validRequest;
	}

}
