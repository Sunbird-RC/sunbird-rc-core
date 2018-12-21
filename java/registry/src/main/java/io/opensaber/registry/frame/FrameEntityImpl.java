package io.opensaber.registry.frame;

import com.google.common.io.CharStreams;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class FrameEntityImpl implements FrameEntity {

	private static Logger logger = LoggerFactory.getLogger(FrameEntityImpl.class);

	@Value("${frame.file}")
	private String frameFile;


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
	public String getContent() {
		InputStreamReader in;
		try {
			in = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(frameFile));
			return CharStreams.toString(in);

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			logger.info(e1.getLocalizedMessage());

		} catch (IOException e) {
			e.printStackTrace();
			logger.info(e.getLocalizedMessage());

		}
		return null;
	}

}
