package io.opensaber.registry.exception;

import com.google.gson.Gson;
import io.opensaber.registry.interceptor.handler.BaseResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CustomExceptionHandler extends BaseResponseHandler implements HandlerExceptionResolver {

	private static Logger logger = LoggerFactory.getLogger(CustomExceptionHandler.class);

	private Gson gson;

	public CustomExceptionHandler(Gson gson) {
		this.gson = gson;
	}

	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		try {
			logger.info("Exception thrown-" + ex.getMessage());
			setResponse(response);
			writeResponseObj(gson, ex.getMessage());
		} catch (Exception e) {
			logger.error("Error in sending response");
		}
		return null;
	}

}
