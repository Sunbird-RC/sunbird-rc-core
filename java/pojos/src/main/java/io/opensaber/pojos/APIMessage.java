package io.opensaber.pojos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component("apiMessage")
@Scope(value = WebApplicationContext.SCOPE_REQUEST,
					proxyMode = ScopedProxyMode.TARGET_CLASS)
public class APIMessage {
	private static Logger logger = LoggerFactory.getLogger(APIMessage.class);

	/* HTTP wrapper */
	private RequestWrapper requestWrapper;

	/* Custom pojo specific to org */
	private Request request;

	/* A temporary map to pass data cooked up in the interceptors, modules */
	private Map<String, Object> localMap = new HashMap<>();

	private String userID;

	public APIMessage() {}

	@Autowired
	public APIMessage(HttpServletRequest servletRequest) {
		request = new Request();
		requestWrapper = new RequestWrapper(servletRequest);
		String body = requestWrapper.getBody();
		try {
			request = new ObjectMapper().readValue(body, Request.class);
		} catch (IOException jpe) {
			logger.error("Can't read request body");
			request = null;
		}
	}

	/**
	 * Get the message body
	 * @return
	 */
	public String getBody() {
		return requestWrapper.getBody();
	}

	/**
	 * Provides access to HTTPServletRequest operations
	 * @return
	 */
	public RequestWrapper getRequestWrapper() {
		return requestWrapper;
	}

	public Request getRequest() {
		return request;
	}

	/**
	 * Add some temporary request-specific data, say massaged data
	 * @param key
	 * @param data
	 */
	public void addLocalMap(String key, Object data) {
	    localMap.put(key, data);
    }

	/**
	 * Read back from local
	 * @param key
	 * @return
	 */
	public Object getLocalMap(String key) {
	    return localMap.get(key);
    }

	/**
	 * Get a map of all temporary data
	 * @return
	 */
	public Map<String, Object> getLocalMap() {
		return localMap;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}
}
