package io.opensaber.registry.interceptor;

import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ValidationInterceptor implements HandlerInterceptor {

	private static Logger logger = LoggerFactory.getLogger(ValidationInterceptor.class);

	private Middleware validationFilter;

	@Autowired
	private APIMessage apiMessage;

	@Autowired
	private OpenSaberInstrumentation watch;

	public ValidationInterceptor(Middleware validationFilter) {
		this.validationFilter = validationFilter;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		boolean result = true;
		watch.start("ValidationInterceptor.execute");
		result = validationFilter.execute(apiMessage);
		watch.stop("ValidationInterceptor.execute");
		return result;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
	}

}
