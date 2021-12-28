package dev.sunbirdrc.registry.interceptor.handler;

import dev.sunbirdrc.pojos.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.servlet.http.HttpServletResponse;

@Component
@RequestScope
public class APIResponseMessage {
	@Autowired
	private HttpServletResponse httpServletResponse;

	private Response response;

	public APIResponseMessage() {
		if (httpServletResponse != null && response == null) {
			response = new Response();
		}
	}

}
