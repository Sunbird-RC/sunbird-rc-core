package io.opensaber.registry.middleware.impl;

import java.io.IOException;
import java.util.Map;
import org.apache.jena.rdf.model.Model;

import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFConverter implements Middleware {

	private static Logger logger = LoggerFactory.getLogger(RDFConverter.class);
	private static final String JSONLD_DATA_IS_MISSING = "JSON-LD data is missing!";
	private static final String FORMAT = "JSON-LD";

	public Map<String, Object> execute(Map<String, Object> mapData) throws IOException, MiddlewareHaltException {
		Object jsonld = mapData.get(Constants.ATTRIBUTE_NAME);
		if (jsonld == null) {
			throw new MiddlewareHaltException(JSONLD_DATA_IS_MISSING);
		} else if (jsonld instanceof String) {
			Model rdfModel = RDFUtil.getRdfModelBasedOnFormat(jsonld.toString(), FORMAT);
			mapData.put(Constants.RDF_OBJECT, rdfModel);
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
