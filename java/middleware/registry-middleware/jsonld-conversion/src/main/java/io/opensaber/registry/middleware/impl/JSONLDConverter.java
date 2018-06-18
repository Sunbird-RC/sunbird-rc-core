package io.opensaber.registry.middleware.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.sparql.core.DatasetGraph;

import io.opensaber.converters.JenaRDF4J;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;

/**
 * 
 * @author jyotsna
 *
 */
public class JSONLDConverter implements Middleware{
	
	private static final String INVALID_RDF_DATA = "RDF data is invalid!";

	public Map<String,Object> execute(Map<String,Object> mapData) throws IOException, MiddlewareHaltException {
		Object responseData = mapData.get(Constants.RESPONSE_ATTRIBUTE);
		if(responseData != null){
			if(responseData instanceof org.eclipse.rdf4j.model.Model){
				Model jenaEntityModel = JenaRDF4J.asJenaModel((org.eclipse.rdf4j.model.Model)responseData);
				DatasetGraph g = DatasetFactory.create(jenaEntityModel).asDatasetGraph();
				JsonLDWriteContext ctx = new JsonLDWriteContext();
				InputStream is = this.getClass().getClassLoader().getResourceAsStream("sample-frame.json");
				String fileString = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
				ctx.setFrame(fileString);
				WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(org.apache.jena.riot.RDFFormat.JSONLD_FRAME_FLAT) ;
				PrefixMap pm = RiotLib.prefixMap(g);
				String base = null;
				StringWriter sWriterJena = new StringWriter();
				w.write(sWriterJena, g, pm, base, ctx) ;
				String jenaJSON = sWriterJena.toString();
				mapData.put(Constants.RESPONSE_ATTRIBUTE, jenaJSON);
			} else{
				throw new MiddlewareHaltException(INVALID_RDF_DATA);
			}
		}
		
		return mapData;
	}

	public Map<String,Object> next(Map<String,Object> mapData) throws IOException {
		return new HashMap<String,Object>();
	}
}
