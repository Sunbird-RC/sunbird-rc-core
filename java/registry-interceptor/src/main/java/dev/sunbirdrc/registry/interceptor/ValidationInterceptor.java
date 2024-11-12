package dev.sunbirdrc.registry.interceptor;

import dev.sunbirdrc.pojos.APIMessage;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.registry.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ValidationInterceptor implements HandlerInterceptor {

	private static Logger logger = LoggerFactory.getLogger(ValidationInterceptor.class);

	private Middleware validationFilter;

	@Autowired
	private APIMessage apiMessage;

	@Autowired
	private SunbirdRCInstrumentation watch;

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
