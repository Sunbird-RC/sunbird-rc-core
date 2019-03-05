package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.elastic.IElasticService;
import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import io.opensaber.registry.util.ReadConfigurator;
import io.opensaber.registry.util.RecordIdentifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This class provide read option with Elastic search
 * database to operate
 *
 */
@Component
public class ElasticReadService implements IReadService {

    @Autowired
    private IElasticService elasticService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * This method interacts with the Elasticsearch and reads the record
     *
     * @param id           - osid
     * @param entityType   - elastic-search index
     * @param configurator
     * @return
     * @throws Exception
     */
    @Override
    public JsonNode getEntity(String id, String entityType, ReadConfigurator configurator) throws Exception {
        JsonNode result = null;
        Map<String, Object> response = elasticService.readEntity(entityType.toLowerCase(), id);
        if(response == null) {
            throw new RecordNotFoundException("Record with " +id+ " not found");
        }
        result = objectMapper.convertValue(response, JsonNode.class);
        if(!configurator.isIncludeSignatures()) {
            JSONUtil.removeNode((ObjectNode) result, Constants.SIGNATURES_STR);
        }
        return result;
    }

}
