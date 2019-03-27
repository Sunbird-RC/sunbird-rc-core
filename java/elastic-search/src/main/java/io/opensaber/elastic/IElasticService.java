package io.opensaber.elastic;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.pojos.SearchQuery;
import java.io.IOException;
import java.util.Map;
import org.elasticsearch.rest.RestStatus;

/**
 * This interface contains unimplemented abstract methods with respect to ElasticSearch
 */
public interface IElasticService {

    /** Saves document into ES(ElasticSearch)
     * @param index - ElasticSearch Index
     * @param id - document id
     * @param inputEntity - input json document for adding
     * @return
     */
    RestStatus addEntity(String index, String id, JsonNode inputEntity);

    /** Reads document with respect to input osid from ES
     * @param index - ElasticSearch Index
     * @param osid - which maps to document
     * @return
     */
    Map<String, Object> readEntity(String index, String osid) throws IOException;

    /** updates document with respect to input osid to ES
     * @param index - ElasticSearch Index
     * @param inputEntity - input json document for updating
     * @param osid - which maps to document
     * @return
     */
    RestStatus updateEntity(String index, String osid, JsonNode inputEntity);

    /** deletes document with respect to input osid from ES
     * @param index - ElasticSearch Index
     * @param osid - which maps to document
     */
    RestStatus deleteEntity(String index, String osid);

    /** searches documents from ES based on query
     * @param index - ElasticSearch Index
     * @param searchQuery - which contains details for search
     * @return
     */
    JsonNode search(String index, SearchQuery searchQuery) throws IOException;
}
