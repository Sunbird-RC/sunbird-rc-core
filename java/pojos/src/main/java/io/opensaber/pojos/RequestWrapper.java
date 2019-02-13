package io.opensaber.pojos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author jyotsna
 *
 */
public class RequestWrapper extends HttpServletRequestWrapper {
	private static Logger logger = LoggerFactory.getLogger(RequestWrapper.class);

	private String body;

	public RequestWrapper(HttpServletRequest request) {
		super(request);
	}

	public String getBody() {
		if (this.body == null || this.body.isEmpty()) {
			StringBuilder buffer = new StringBuilder();
			BufferedReader reader = null;
			try {
				reader = getReader();
				String line;
				while ((line = reader.readLine()) != null) {
					buffer.append(line);
				}
			} catch (IOException e) {
				logger.error("Can't read from http stream. Set body empty");
			}

			body = buffer.toString();
		}

		return this.body;
	}

	public Map<String, Object> getRequestHeaderMap() throws IOException {
		Map<String, Object> requestHeaderMap = new HashMap<>();
		Enumeration<String> headerNames = getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String header = headerNames.nextElement();
				requestHeaderMap.put(header, getHeader(header));
			}
		}
		return requestHeaderMap;
	}
}