package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.exception.UniqueIdentifierException;
import dev.sunbirdrc.registry.service.IdGenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class IdGenServiceImpl implements IdGenService {

    private static Logger logger = LoggerFactory.getLogger(IdGenService.class);

    @Autowired
    private RetryRestTemplate retryRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${idgen.idgenURL}")
    private String idgenURL;

    @Override
    public Object createUniqueID(ObjectNode reqNode) throws UniqueIdentifierException.UnreachableException, UniqueIdentifierException.CreationException {
        ResponseEntity<String> response = null;
        JsonNode result = null;
        try {
            response = retryRestTemplate.postForEntity(idgenURL,reqNode);
            result = objectMapper.readTree(response.getBody());
            logger.info("Successfully generated unique ID");
        } catch (RestClientException ex) {
            logger.error("RestClientException when signing: ", ex);
            throw new UniqueIdentifierException().new UnreachableException(ex.getMessage());
        } catch (Exception e) {
            logger.error("RestClientException when signing: ", e);
            throw new UniqueIdentifierException().new CreationException(e.getMessage());
        }
        return result;
    }


}
