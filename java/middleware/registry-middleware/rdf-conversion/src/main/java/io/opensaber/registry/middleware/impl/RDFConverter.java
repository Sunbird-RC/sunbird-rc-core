package io.opensaber.registry.middleware.impl;

import java.io.IOException;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;

public class RDFConverter implements Middleware {
	private static Logger logger = LoggerFactory.getLogger(RDFConverter.class);

	public Map<String, Object> execute(Map<String, Object> mapData) throws IOException, MiddlewareHaltException {
		logger.debug("Attempting to convert LD to RDF");
		Object jsonld = mapData.get(Constants.LD_OBJECT);
		if (jsonld == null) {
			throw new MiddlewareHaltException(Constants.JSONLD_DATA_IS_MISSING);
		} else if (jsonld instanceof String) {
			Model rdfModel = RDFUtil.getRdfModelBasedOnFormat(jsonld.toString(), Constants.JENA_LD_FORMAT);
			mapData.put(Constants.RDF_OBJECT, rdfModel);
		} else {
			throw new MiddlewareHaltException(Constants.JSONLD_PARSE_ERROR);
		}
		logger.debug("Converted to RDF success");
		return mapData;
	}

	public Map<String, Object> next(Map<String, Object> mapData) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
