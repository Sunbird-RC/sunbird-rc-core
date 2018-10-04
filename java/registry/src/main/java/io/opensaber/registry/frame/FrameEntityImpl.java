package io.opensaber.registry.frame;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.io.CharStreams;


import io.opensaber.converters.JenaRDF4J;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.exception.MultipleEntityException;
import io.opensaber.registry.frame.FrameEntity;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;


public class FrameEntityImpl implements FrameEntity {

	private static Logger logger = LoggerFactory.getLogger(FrameEntityImpl.class);

	@Value("${frame.file}")
	private String frameFile;

	@Override
	public String getContent(org.eclipse.rdf4j.model.Model entityModel) throws IOException, MultipleEntityException, EntityCreationException {
		String jenaJson = frameEntity2Json(entityModel);
		logger.info("JsonldResponseContent: Framed Jena JSON- " + jenaJson);
		return jenaJson;
	}
	
	/**
	 * Helper method to convert the RDF4j model to JSONLD
	 * 
	 * @param entityModel
	 * @return
	 * @throws IOException
	 * @throws MultipleEntityException
	 * @throws EntityCreationException
	 */
	private String frameEntity2Json(org.eclipse.rdf4j.model.Model entityModel)
			throws IOException, MultipleEntityException, EntityCreationException {
		Model jenaEntityModel = JenaRDF4J.asJenaModel(entityModel);
		String jenaJSON = "";
		if (!jenaEntityModel.isEmpty()) {
			DatasetGraph g = DatasetFactory.create(jenaEntityModel).asDatasetGraph();
			JsonLDWriteContext ctx = prepareJsonLDWriteContext(jenaEntityModel);
			StringWriter writer = getWriter(g, ctx);
			jenaJSON = writer.toString();
		}
		return jenaJSON;
	}

	/**
	 * Support for framing for add, update, search
	 * 
	 * @param jenaEntityModel
	 * @return
	 * @throws IOException
	 * @throws EntityCreationException
	 * @throws MultipleEntityException
	 */
	private JsonLDWriteContext prepareJsonLDWriteContext(Model jenaEntityModel)
			throws IOException, EntityCreationException, MultipleEntityException {
		JsonLDWriteContext ctx = new JsonLDWriteContext();
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(frameFile);
		String fileString = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);

		List<Resource> rootLabels = RDFUtil.getRootLabels(jenaEntityModel);
		String rootLabelType = null;
		switch (rootLabels.size()) {
		case 0:
			throw new EntityCreationException(Constants.NO_ENTITY_AVAILABLE_MESSAGE);
		default:
			List<String> rootLabelTypes = RDFUtil.getTypeForSubject(jenaEntityModel, rootLabels.iterator().next());
			rootLabelType = rootLabelTypes.get(0);
		}
		if (fileString.contains("<@type>"))
			fileString = fileString.replace("<@type>", rootLabelType);

		ctx.setFrame(fileString);
		return ctx;
	}

	private StringWriter getWriter(DatasetGraph g, JsonLDWriteContext ctx) {
		WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(org.apache.jena.riot.RDFFormat.JSONLD_FRAME_FLAT);
		PrefixMap pm = RiotLib.prefixMap(g);
		String base = null;
		StringWriter sWriter = new StringWriter();
		w.write(sWriter, g, pm, base, ctx);
		return sWriter;
	}
	
/**
 * Frame json specific.
 */
	public String getContent(){				
		InputStreamReader in;
		try {			
			in = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(frameFile));
			return CharStreams.toString(in);
			
		} catch (FileNotFoundException  e1) {
			e1.printStackTrace();
			logger.info(e1.getLocalizedMessage());
		
		} catch (IOException e) {
			e.printStackTrace();
			logger.info(e.getLocalizedMessage());

		}
		return null;
	}

}
