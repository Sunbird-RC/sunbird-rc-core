package dev.sunbirdrc.elastic;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.pojos.SearchQuery;
import java.io.IOException;
import java.util.Map;
import org.elasticsearch.rest.RestStatus;

/**
 * This interface contains unimplemented abstract methods with respect to ElasticSearch
 */
public interface IElasticService extends HealthIndicator {

    /** Saves document into ES(ElasticSearch)
     * @param index - ElasticSearch Index
     * @param id - document id
     * @param inputEntity - input json document for adding
     * @return
     */
    RestStatus addEntity(String index, String id, JsonNode inputEntity);

    /** Reads document with respect to input uuidPropertyValue from ES
     * @param index - ElasticSearch Index
     * @param uuidPropertyValue - which maps to document
     * @return
     */
    Map<String, Object> readEntity(String index, String uuidPropertyValue) throws IOException;

    /** updates document with respect to input uuidPropertyValue to ES
     * @param index - ElasticSearch Index
     * @param inputEntity - input json document for updating
     * @param uuidPropertyValue - which maps to document
     * @return
     */
    RestStatus updateEntity(String index, String uuidPropertyValue, JsonNode inputEntity);

    /** deletes document with respect to input uuidPropertyValue from ES
     * @param index - ElasticSearch Index
     * @param uuidPropertyValue - which maps to document
     */
    RestStatus deleteEntity(String index, String uuidPropertyValue);

    /** searches documents from ES based on query
     * @param index - ElasticSearch Index
     * @param searchQuery - which contains details for search
     * @return
     */
    JsonNode search(String index, SearchQuery searchQuery) throws IOException;

}
