package io.opensaber.registry.interceptor;

import java.io.IOException;
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
import io.opensaber.registry.middleware.util.Constants;

@Component
public class RDFValidationMappingInterceptor implements HandlerInterceptor {

	@Autowired
	private OpenSaberInstrumentation watch;

	private Middleware rdfValidationMapper;

	private Gson gson;

	private static Logger logger = LoggerFactory.getLogger(RDFValidationMappingInterceptor.class);

	public RDFValidationMappingInterceptor(Middleware rdfValidationMapper, Gson gson){
		this.rdfValidationMapper = rdfValidationMapper;
		this.gson = gson;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object arg2) throws IOException, MiddlewareHaltException {
		BaseRequestHandler baseRequestHandler = new BaseRequestHandler();
		try {
			baseRequestHandler.setRequest(request);
			watch.start("RDFValidationMappingInterceptor.execute");
			Map<String, Object> attributeMap = rdfValidationMapper.execute(baseRequestHandler.getRequestAttributeMap());
			baseRequestHandler.mergeRequestAttributes(attributeMap);
			watch.stop("RDFValidationMappingInterceptor.execute");
			request = baseRequestHandler.getRequest();
			if (request.getAttribute(Constants.RDF_VALIDATION_MAPPER_OBJECT) != null) {
				logger.debug("RDF validator object mapped successfully !");
				return true;
			}
		}catch(MiddlewareHaltException e){
			logger.error("MiddlewareHaltException from RDFValidationMappingInterceptor !" + e);
			baseRequestHandler.setResponse(response);
			baseRequestHandler.writeResponseObj(gson, e.getMessage());
			response = baseRequestHandler.getResponse();
		}catch(Exception e){
			logger.error("Exception from RDFValidationMappingInterceptor !" + e);
			baseRequestHandler.setResponse(response);
			baseRequestHandler.writeResponseObj(gson, Constants.RDF_VALIDATION_MAPPING_ERROR);
			response = baseRequestHandler.getResponse();
		}
		return false;
	}

	@Override
	public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3)
			throws Exception {
	}

	@Override
	public void postHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, ModelAndView arg3)
			throws Exception {
		// TODO Auto-generated method stub

	}

}
