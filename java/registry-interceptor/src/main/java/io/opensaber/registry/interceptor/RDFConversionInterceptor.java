package io.opensaber.registry.interceptor;

import java.util.Map;
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
import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.impl.RDFConverter;
import io.opensaber.registry.middleware.util.Constants;

@Component
public class RDFConversionInterceptor implements HandlerInterceptor{

	private static Logger logger = LoggerFactory.getLogger(RDFConversionInterceptor.class);
	private Middleware rdfConverter;
	private Gson gson;

	@Autowired
	private OpenSaberInstrumentation watch;

	public RDFConversionInterceptor(Middleware rdfConverter, Gson gson){
		this.rdfConverter = rdfConverter;
		this.gson = gson;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		BaseRequestHandler baseRequestHandler = new BaseRequestHandler();
		try{
			baseRequestHandler.setRequest(request);
			watch.start("RDFConversionInterceptor.execute");
			Map<String, Object> attributeMap = rdfConverter.execute(baseRequestHandler.getRequestBodyMap());
			baseRequestHandler.mergeRequestAttributes(attributeMap);
			watch.stop("RDFConversionInterceptor.execute");
			request = baseRequestHandler.getRequest();
			if (request.getAttribute(Constants.RDF_OBJECT) != null) {
				logger.debug("RDF object for conversion :" + request.getAttribute(Constants.RDF_OBJECT));
				return true;
			}
		}catch(MiddlewareHaltException e){
			logger.error("MiddlewareHaltException from RDFConversionInterceptor !"+ e);
			baseRequestHandler.setResponse(response);
			baseRequestHandler.writeResponseObj(gson, e.getMessage());
			response = baseRequestHandler.getResponse();
		}catch(Exception e){
			logger.error("Exception from RDFConversionInterceptor!"+ e);
			e.printStackTrace();
			baseRequestHandler.setResponse(response);
			baseRequestHandler.writeResponseObj(gson, Constants.JSONLD_PARSE_ERROR);
			response = baseRequestHandler.getResponse();
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
