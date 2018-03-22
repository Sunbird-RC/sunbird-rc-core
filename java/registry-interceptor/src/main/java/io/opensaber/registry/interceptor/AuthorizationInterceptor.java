package io.opensaber.registry.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.Gson;

import io.opensaber.registry.authorization.AuthorizationFilter;
import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;

@Component
public class AuthorizationInterceptor extends BaseRequestHandler implements HandlerInterceptor{
	
	private AuthorizationFilter authorizationFilter;
	
	private Gson gson;
	
	@Autowired
	public AuthorizationInterceptor(AuthorizationFilter authorizationFilter, Gson gson){
		this.authorizationFilter = authorizationFilter;
		this.gson = gson;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		try{
			setRequest(request);
			authorizationFilter.execute(getRequestHeaderMap());
			return true;
		}catch(MiddlewareHaltException e){
			setResponse(response);
			writeResponseObj(gson, e.getMessage());
			response = getResponse();
		}catch(Exception e){
			setResponse(response);
			writeResponseObj(gson, Constants.TOKEN_EXTRACTION_ERROR);
			response = getResponse();
		}
		return false;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub
		HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// TODO Auto-generated method stub
		HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
	}



}
