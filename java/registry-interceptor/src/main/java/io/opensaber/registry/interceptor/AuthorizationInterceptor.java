package io.opensaber.registry.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.opensaber.pojos.OpenSaberInstrumentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import com.google.gson.Gson;
import io.opensaber.registry.authorization.AuthorizationFilter;
import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.Middleware;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

	private static Logger logger = LoggerFactory.getLogger(AuthorizationInterceptor.class);
	private Middleware authorizationFilter;
	
	private Gson gson;

	@Autowired
	private OpenSaberInstrumentation watch;
	
	public AuthorizationInterceptor(Middleware authorizationFilter, Gson gson){
		this.authorizationFilter = authorizationFilter;
		this.gson = gson;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		BaseRequestHandler baseRequestHandler = new BaseRequestHandler();
		try{
			baseRequestHandler.setRequest(request);
			watch.start("AuthorizationInterceptor.execute");
			authorizationFilter.execute(baseRequestHandler.getRequestHeaderMap());
			watch.stop("AuthorizationInterceptor.execute");
			logger.debug(" Authentication successfull !");

			return true;
		}catch(MiddlewareHaltException e){
			logger.error(" Authentication Failed !", e);
			baseRequestHandler.setResponse(response);
			baseRequestHandler.writeResponseObj(gson, e.getMessage());
			response = baseRequestHandler.getResponse();
		}catch(Exception e){
			logger.error(" Authentication Failed !", e);
			baseRequestHandler.setResponse(response);
			baseRequestHandler.writeResponseObj(gson, Constants.TOKEN_EXTRACTION_ERROR);
			response = baseRequestHandler.getResponse();
		}
		return false;
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
