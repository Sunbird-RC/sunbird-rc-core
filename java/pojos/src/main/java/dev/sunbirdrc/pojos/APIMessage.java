package dev.sunbirdrc.pojos;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Getter
@Component("apiMessage")
@Scope(value = WebApplicationContext.SCOPE_REQUEST,
					proxyMode = ScopedProxyMode.TARGET_CLASS)
public class APIMessage {
	private static Logger logger = LoggerFactory.getLogger(APIMessage.class);

    /**
     * -- GETTER --
     *  Provides access to HTTPServletRequest operations
     *
     */
    /* HTTP wrapper */
	private RequestWrapper requestWrapper;

	/* Custom pojo specific to org */
	private Request request;

    /**
     * -- GETTER --
     *  Get a map of all temporary data
     *
     */
    /* A temporary map to pass data cooked up in the interceptors, modules */
	private final Map<String, Object> localMap = new HashMap<>();

	@Setter
    private String userID;

	public APIMessage() {}

	@Autowired
	public APIMessage(HttpServletRequest servletRequest) {
		request = new Request();
		requestWrapper = new RequestWrapper(servletRequest);
		String body = requestWrapper.getBody();
		if(body != null && !body.isEmpty()) {
			try {
				request = new ObjectMapper()
						.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
						.readValue(body, Request.class);
			} catch (IOException e) {
				logger.error("Can't read request body: {}", ExceptionUtils.getStackTrace(e));
				request = null;
			}
		}
	}

	/**
	 * Get the message body
	 * @return body
	 */
	public String getBody() {
		return requestWrapper.getBody();
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
	 * @param  key
	 * @return data
	 */
	public Object getLocalMap(String key) {
	    return localMap.get(key);
    }

}
