package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

/**
 * Save audit details to file system
 */
public class AuditFileWriter {
    private static Logger logger = LoggerFactory.getLogger(AuditFileWriter.class);

    @Async("auditExecutor")
    public void auditToFile(JsonNode auditRecord) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String auditString = objectMapper.writeValueAsString(auditRecord);
        logger.info("{}", auditString);
    }

}
