package io.opensaber.registry.interceptor;

import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

	private static Logger logger = LoggerFactory.getLogger(AuthorizationInterceptor.class);
	private Middleware authorizationFilter;

	@Autowired
	private APIMessage apiMessage;

	@Autowired
	private OpenSaberInstrumentation watch;

	public AuthorizationInterceptor(Middleware authorizationFilter) {
		this.authorizationFilter = authorizationFilter;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		watch.start("AuthorizationInterceptor.execute");
		authorizationFilter.execute(apiMessage);
		watch.stop("AuthorizationInterceptor.execute");
		logger.debug(" Authentication successful !");

		return true;
	}
}
