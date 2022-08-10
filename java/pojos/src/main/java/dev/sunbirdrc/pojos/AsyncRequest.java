package dev.sunbirdrc.pojos;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.servlet.http.HttpServletRequest;

@Component("asyncRequest")
@RequestScope
public class AsyncRequest {
	private static final String MODE = "mode";
	private static final String ASYNC = "async";
	private static final Logger logger = LoggerFactory.getLogger(AsyncRequest.class);
	private static final String CALLBACK = "callback";
	private Boolean enabled = Boolean.FALSE;
	private String webhookUrl;

	public AsyncRequest() {
	}

	@Autowired
	public AsyncRequest(HttpServletRequest servletRequest) {
		if (!StringUtils.isEmpty(servletRequest.getParameter(MODE)) && servletRequest.getParameter(MODE).equalsIgnoreCase(ASYNC)) {
			logger.info("Async request received: {}", servletRequest.getParameter(MODE));
			enabled = Boolean.TRUE;
			if (!StringUtils.isEmpty(servletRequest.getParameter(CALLBACK))) {
				logger.info("Async request received with callback: {}", servletRequest.getParameter(CALLBACK));
				webhookUrl = servletRequest.getParameter(CALLBACK);
			}
		}

	}

	public Boolean isEnabled() {
		return enabled;
	}

	public String getWebhookUrl() {
		return webhookUrl;
	}
}
