package dev.sunbirdrc.registry.frame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));

        }
    }

    public String getContent() {
        return frameContent;
    }

    /**
     * Always return one domain that the registry represents
     *
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
            logger.error(ExceptionUtils.getStackTrace(e));
        }
        return JSONUtil.findKey(frameNode, registryContextBase);
    }


}
