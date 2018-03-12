package io.opensaber.registry.middleware.impl;

import java.io.IOException;
import java.util.Map;

import org.apache.jena.rdf.model.Model;

import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFConverter implements BaseMiddleware {

	private static Logger logger = LoggerFactory.getLogger(RDFConverter.class);
	private static final String JSONLD_DATA_IS_MISSING = "JSON-LD data is missing!";
	private static final String FORMAT = "JSON-LD";

	public Map<String, Object> execute(Map<String, Object> mapData) throws IOException, MiddlewareHaltException {
		Object jsonld = mapData.get(Constants.ATTRIBUTE_NAME);
		if (jsonld == null) {
			throw new MiddlewareHaltException(this.getClass().getName() + JSONLD_DATA_IS_MISSING);
		} else if (jsonld instanceof String) {
			Model model = ShaclexValidator.parse(jsonld.toString(), FORMAT);
			logger.info("RDF Converter called...");
			mapData.put(Constants.RDF_OBJECT, model);
		} else {
			throw new MiddlewareHaltException(this.getClass().getName() + "JSONLD data is invalid!");
		}
		return mapData;
	}

	public Map<String, Object> next(Map<String, Object> mapData) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
}
