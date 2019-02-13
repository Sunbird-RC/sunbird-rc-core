package io.opensaber.registry.frame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import io.opensaber.registry.middleware.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FrameContext {
	private static Logger logger = LoggerFactory.getLogger(FrameContext.class);

	private String registryContextBase;
	private String frameContent;

	public FrameContext(String frameFileName, String registryContextBase) {
		this.registryContextBase = registryContextBase;

		InputStream in;
		try {
			in = this.getClass().getClassLoader().getResourceAsStream(frameFileName);
			frameContent = new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8);

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			logger.error(e1.getLocalizedMessage());

		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getLocalizedMessage());

		}
	}

	public String getContent() {
		return frameContent;
	}
    /**
     * Always return one domain that the registry represents
     * @return
     */
	public String getDomain() {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode frameNode = null;
		logger.debug(
				"for FrameContext registryContextBase: " + registryContextBase + " and frame content: " + frameContent);
		try {
			frameNode = mapper.readTree(getContent());
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getLocalizedMessage());
		}
		return JSONUtil.findKey(frameNode, registryContextBase);
	}


}
