package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.UniqueIdRequest;
import dev.sunbirdrc.pojos.UniqueIdentifierFields;
import dev.sunbirdrc.registry.exception.UniqueIdentifierException;
import dev.sunbirdrc.registry.service.IdGenService;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import dev.sunbirdrc.registry.middleware.util.Constants;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Component
public class IdGenServiceImpl implements IdGenService {

    private static Logger logger = LoggerFactory.getLogger(IdGenService.class);

    @Autowired
    private RetryRestTemplate retryRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IDefinitionsManager definitionsManager;

    @Value("${idgen.createURL}")
    private String idgenURL;

    @Value("${idgen.host}")
    private String idgenBase;

    @Override
    public Object createUniqueID (ObjectNode reqNode) throws UniqueIdentifierException.UnreachableException, UniqueIdentifierException.CreationException {
        ResponseEntity<String> response = null;
        JsonNode result = null;
        try {
            response = retryRestTemplate.postForEntity( idgenBase+idgenURL, reqNode);
            result = objectMapper.readTree(response.getBody());
            logger.info("Successfully generated unique ID");
        } catch (RestClientException ex) {
            logger.error("RestClientException while uniqueID generation: ", ex);
            throw new UniqueIdentifierException().new UnreachableException(ex.getMessage());
        } catch (Exception e) {
            logger.error("RestClientException while uniqueID generation: : ", e);
            throw new UniqueIdentifierException().new CreationException(e.getMessage());
        }
        return result;
    }

    @Override
    public Object createUniqueIDsForAnEntity(String entityName, JsonNode inputNode) throws UniqueIdentifierException.UnreachableException, UniqueIdentifierException.CreationException {

        try {
            Definition definition = null;
            definition = definitionsManager.getDefinition(entityName);
            String prefixForTenetID = Constants.PREFIX_TENET_ID + entityName ;
            String prefixForIdName = Constants.PREFIX_ID_NAME + entityName;

            // Format request to send to ID generation service
            HashMap reqJsonToSend = new HashMap();
            ObjectNode reqInfo = new ObjectMapper().createObjectNode();
            reqInfo.put("apiId", Constants.APP_ID).put("ver", "1").put("ts", 0);
            reqInfo.put("msgId", Constants.MSG_ID);
            ArrayList<UniqueIdRequest> idRequests = new ArrayList<>();
            List<UniqueIdentifierFields> schemaDefinedUniqueIdentifierFields= new ArrayList<>();
            schemaDefinedUniqueIdentifierFields = definition.getOsSchemaConfiguration().getUniqueIdentifierFields();
            for(UniqueIdentifierFields uniqueIdFieldInSchema : schemaDefinedUniqueIdentifierFields) {
                idRequests.add(UniqueIdRequest.builder()
                        .idName(prefixForIdName + '.' + uniqueIdFieldInSchema.getField())
                        .tenantId(prefixForTenetID + '.' + uniqueIdFieldInSchema.getField())
                        .format(uniqueIdFieldInSchema.getRegularExpression()).build());
            }
            reqJsonToSend.put("RequestInfo", reqInfo);
            reqJsonToSend.put("idRequests", idRequests);
            JsonNode reqToSend = new ObjectMapper().valueToTree(reqJsonToSend);

            Object result = null;
            if (definition.getOsSchemaConfiguration().getUniqueIdentifierFields().size() > 0) {
                result = createUniqueID((ObjectNode) reqToSend);
                // set the values that are returned from the ID_Gen service
                for (int i = 0; i < definition.getOsSchemaConfiguration().getUniqueIdentifierFields().size(); i++) {
                    ((ObjectNode) inputNode.at("/" + entityName)).put(definition.getOsSchemaConfiguration().getUniqueIdentifierFields().get(i).getField(), ((ObjectNode) result).get("idResponses").get(i).get("id"));
                }
            }

            return result;

        } catch (RestClientException ex) {
            logger.error("RestClientException while uniqueID generation: ", ex);
            throw new UniqueIdentifierException().new UnreachableException(ex.getMessage());
        } catch (Exception e) {
            logger.error("RestClientException while uniqueID generation: : ", e);
            throw new UniqueIdentifierException().new CreationException(e.getMessage());
        }

    }


}
