package io.opensaber.registry.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.Constants.Direction;
import io.opensaber.registry.transform.Configuration;
import io.opensaber.registry.transform.Data;
import io.opensaber.registry.transform.Transformer;

@Component
public class RDFConversionInterceptor implements HandlerInterceptor {

	private static Logger logger = LoggerFactory.getLogger(RDFConversionInterceptor.class);
	private Middleware rdfConverter;

	@Autowired
	private OpenSaberInstrumentation watch;

	@Autowired
	private APIMessage apiMessage;

	private Transformer transformer;

	public RDFConversionInterceptor(Middleware rdfConverter, Transformer transformer) {
		this.rdfConverter = rdfConverter;
		this.transformer = transformer;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		boolean result = false;
		String dataFromRequest = apiMessage.getRequest().getRequestMapAsString();
		String contentType = request.getContentType();
		logger.debug("ContentType {0} requestBody {1}", contentType, dataFromRequest);
		Configuration config = transformer.getConfiguration(contentType, Direction.IN);
		Data<Object> transformedData = transformer.getInstance(config)
				.transform(new Data<Object>(dataFromRequest));

		logger.debug("After transformation {0}", transformedData.getData());

		apiMessage.addLocalMap(Constants.LD_OBJECT, transformedData.getData().toString());

		watch.start("RDFConversionInterceptor.execute");
		Map<String, Object> attributeMap = rdfConverter.execute(apiMessage.getLocalMap());
		watch.stop("RDFConversionInterceptor.execute");

		if (attributeMap.get(Constants.RDF_OBJECT) != null) {
			apiMessage.addLocalMap(Constants.RDF_OBJECT, attributeMap.get(Constants.RDF_OBJECT));

			logger.debug("RDF object for conversion : {0}", attributeMap.get(Constants.RDF_OBJECT));
			result = true;
		}

		return result;
	}

}
