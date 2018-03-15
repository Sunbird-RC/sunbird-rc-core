package io.opensaber.registry.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.Gson;

import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.impl.RDFConverter;
import io.opensaber.registry.middleware.util.Constants;

@Order(1)
@Component
public class RDFConversionInterceptor extends BaseRequestHandler implements HandlerInterceptor{


	private RDFConverter rdfConverter;
	
	private Gson gson;

	@Autowired
	public RDFConversionInterceptor(RDFConverter rdfConverter, Gson gson){
		this.rdfConverter = rdfConverter;
		this.gson = gson;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		try{
		setRequest(request);
		Map<String, Object> attributeMap = rdfConverter.execute(getRequestBodyMap());
		mergeRequestAttributes(attributeMap);
		request = getRequest();
		if (request.getAttribute(Constants.RDF_OBJECT) != null) {
			return true;
		}
		}catch(MiddlewareHaltException e){
			setResponse(response);
			writeResponseObj(gson, e.getMessage());
			response = getResponse();
		}catch(Exception e){
			setResponse(response);
			writeResponseObj(gson, Constants.JSONLD_PARSE_ERROR);
			response = getResponse();
		}
		return false;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub

	}


	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// TODO Auto-generated method stub

	}

}
