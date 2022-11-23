package dev.sunbirdrc.elastic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.Filter;
import dev.sunbirdrc.pojos.FilterOperators;
import dev.sunbirdrc.pojos.SearchQuery;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import java.io.IOException;
import java.net.ConnectException;
import java.util.*;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_ELASTIC_SERVICE_NAME;

public class ElasticServiceImpl implements IElasticService {
    private static Map<String, RestHighLevelClient> esClient = new HashMap<String, RestHighLevelClient>();
    private static Logger logger = LoggerFactory.getLogger(ElasticServiceImpl.class);

    private static String connectionInfo;
    private static String searchType;

    public void setConnectionInfo(String connection) {
        connectionInfo = connection;
    }

    public void setType(String type) {
        searchType = type;
    }

    /**
     * This method runs when the application is started in order to add all the indcies to the elastic search
     *
     * @param indices
     * @throws RuntimeException
     */
    public void init(Set<String> indices) throws RuntimeException {
        indices.iterator().forEachRemaining(index -> {
            try {
                addIndex(index.toLowerCase(), searchType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * This method creates the high-level-client w.r.to index, if client is not created. for every index one client object is created
     *
     * @param indexName      for ElasticSearch
     * @param connectionInfo of ElasticSearch
     */
    private static void createClient(String indexName, String connectionInfo) {
        if (!esClient.containsKey(indexName)) {
            Map<String, Integer> hostPort = new HashMap<String, Integer>();
            for (String info : connectionInfo.split(",")) {
                hostPort.put(info.split(":")[0], Integer.valueOf(info.split(":")[1]));
            }
            List<HttpHost> httpHosts = new ArrayList<>();
            for (String host : hostPort.keySet()) {
                httpHosts.add(new HttpHost(host, hostPort.get(host)));
            }
            RestHighLevelClient client = new RestHighLevelClient(
                    RestClient.builder(httpHosts.toArray(new HttpHost[httpHosts.size()])));
            if (null != client)
                esClient.put(indexName, client);
        }
    }

    /**
     * Get client details from map
     *
     * @param indexName of ElasticSearch
     * @return
     */
    private static RestHighLevelClient getClient(String indexName) {
        logger.info("connection info: index:{} connectioninfo:{}", indexName, connectionInfo);
        if (null == esClient.get(indexName)) {
            createClient(indexName, connectionInfo);
        }
        logger.info("resthighclient obj:" + esClient.get(indexName));
        return esClient.get(indexName);
    }

    /**
     * creates index for elastic-search
     *
     * @param indexName    of ElasticSearch
     * @param documentType of ElasticSearch
     * @return
     * @throws IOException
     */
    @Retryable(value = {IOException.class, ConnectException.class}, maxAttemptsExpression = "#{${service.retry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${service.retry.backoff.delay}}"))
    public static boolean addIndex(String indexName, String documentType) throws IOException {
        boolean response = false;
        //To do need to analysis regarding settings and analysis and modify this code later
        /*String settings = "{\"analysis\": {       \"analyzer\": {         \"doc_index_analyzer\": {           \"type\": \"custom\",           \"tokenizer\": \"standard\",           \"filter\": [             \"lowercase\",             \"mynGram\"           ]         },         \"doc_search_analyzer\": {           \"type\": \"custom\",           \"tokenizer\": \"standard\",           \"filter\": [             \"standard\",             \"lowercase\"           ]         },         \"keylower\": {           \"tokenizer\": \"keyword\",           \"filter\": \"lowercase\"         }       },       \"filter\": {         \"mynGram\": {           \"type\": \"nGram\",           \"min_gram\": 1,           \"max_gram\": 20,           \"token_chars\": [             \"letter\",             \"digit\",             \"whitespace\",             \"punctuation\",             \"symbol\"           ]         }       }     }   }";
        String mappings = "{\"dynamic_templates\":[{\"longs\":{\"match_mapping_type\":\"long\",\"mapping\":{\"type\":\"long\",\"fields\":{\"raw\":{\"type\":\"long\"}}}}},{\"booleans\":{\"match_mapping_type\":\"boolean\",\"mapping\":{\"type\":\"boolean\",\"fields\":{\"raw\":{\"type\":\"boolean\"}}}}},{\"doubles\":{\"match_mapping_type\":\"double\",\"mapping\":{\"type\":\"double\",\"fields\":{\"raw\":{\"type\":\"double\"}}}}},{\"dates\":{\"match_mapping_type\":\"date\",\"mapping\":{\"type\":\"date\",\"fields\":{\"raw\":{\"type\":\"date\"}}}}},{\"strings\":{\"match_mapping_type\":\"string\",\"mapping\":{\"type\":\"text\",\"copy_to\":\"all_fields\",\"analyzer\":\"doc_index_analyzer\",\"search_analyzer\":\"doc_search_analyzer\",\"fields\":{\"raw\":{\"type\":\"text\",\"analyzer\":\"keylower\"}}}}}],\"properties\":{\"all_fields\":{\"type\":\"text\",\"analyzer\":\"doc_index_analyzer\",\"search_analyzer\":\"doc_search_analyzer\",\"fields\":{\"raw\":{\"type\":\"text\",\"analyzer\":\"keylower\"}}}}}";*/
        RestHighLevelClient client = getClient(indexName);
        if (!isIndexExists(indexName)) {
            CreateIndexRequest createRequest = new CreateIndexRequest(indexName);

            /*if (StringUtils.isNotBlank(settings))
               createRequest.settings(Settings.builder().loadFromSource(settings, XContentType.JSON));
            if (StringUtils.isNotBlank(documentType) && StringUtils.isNotBlank(mappings))
                createRequest.mapping(documentType, mappings, XContentType.JSON);*/
            CreateIndexResponse createIndexResponse = client.indices().create(createRequest, RequestOptions.DEFAULT);

            response = createIndexResponse.isAcknowledged();
        }
        return response;
    }

    /**
     * checks whether input index exists in the elastic-search
     *
     * @param indexName of elastic-search
     * @return
     */
    public static boolean isIndexExists(String indexName) {
        Response response;
        try {
            response = getClient(indexName).getLowLevelClient().performRequest(new Request("HEAD", "/" + indexName));
            return (200 == response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            return false;
        }

    }

    /**
     * Adds input as document into elastic-search
     *
     * @param index       - ElasticSearch Index
     * @param entityId    - entity id as document id
     * @param inputEntity - input document for adding
     * @return
     */
    @Override
    public RestStatus addEntity(String index, String entityId, JsonNode inputEntity) {
        logger.debug("addEntity starts with index {} and entityId {}", index, entityId);
        IndexResponse response = null;
        try {
            Map<String, Object> inputMap = JSONUtil.convertJsonNodeToMap(inputEntity);
            response = getClient(index).index(new IndexRequest(index, searchType, entityId).source(inputMap), RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("Exception in adding record to ElasticSearch", e);
        }
        return response.status();
    }

    /**
     * Reads the document from Elastic search
     *
     * @param index - ElasticSearch Index
     * @param osid  - which maps to document
     * @return
     */
    @Override
    @Retryable(value = {IOException.class, ConnectException.class}, maxAttemptsExpression = "#{${service.retry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${service.retry.backoff.delay}}"))
    public Map<String, Object> readEntity(String index, String osid) throws IOException {
        logger.debug("readEntity starts with index {} and entityId {}", index, osid);
        
        GetResponse response = null;
        response = getClient(index).get(new GetRequest(index, searchType, osid), RequestOptions.DEFAULT);
        return response.getSourceAsMap();
    }
    

    /**
     * Updates the document with updated inputEntity
     *
     * @param index       - ElasticSearch Index
     * @param osid        - which maps to document
     * @param inputEntity - input json document for updating
     * @return
     */
    @Override
    public RestStatus updateEntity(String index, String osid, JsonNode inputEntity) {
        logger.debug("updateEntity starts with index {} and entityId {}", index, osid);
        UpdateResponse response = null;
        try {
            Map<String, Object> inputMap = JSONUtil.convertJsonNodeToMap(inputEntity);
            logger.debug("updateEntity inputMap {}", inputMap);
            logger.debug("updateEntity inputEntity {}", inputEntity);
            response = getClient(index.toLowerCase()).update(new UpdateRequest(index.toLowerCase(), searchType, osid).doc(inputMap), RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("Exception in updating a record to ElasticSearch", e);
        }
        return response.status();
    }

    /**
     * Updates the document status to inactive into elastic-search
     *
     * @param index - ElasticSearch Index
     * @param osid  - which maps to document
     * @return
     */
    @Override
    public RestStatus deleteEntity(String index, String osid) {
        UpdateResponse response = null;
        try {
            String indexL = index.toLowerCase();
            Map<String, Object> readMap = readEntity(indexL, osid);
           // Map<String, Object> entityMap = (Map<String, Object>) readMap.get(index);
            readMap.put(Constants.STATUS_KEYWORD, Constants.STATUS_INACTIVE);
            response = getClient(indexL).update(new UpdateRequest(indexL, searchType, osid).doc(readMap), RequestOptions.DEFAULT);
        } catch (NullPointerException | IOException e) {
            logger.error("exception in deleteEntity {}", e);
            return RestStatus.NOT_FOUND;
        }
        return response.status();
    }

    @Override
    @Retryable(value = {IOException.class, ConnectException.class}, maxAttemptsExpression = "#{${service.retry.maxAttempts}}",
            backoff = @Backoff(delayExpression = "#{${service.retry.backoff.delay}}"))
    public JsonNode search(String index, SearchQuery searchQuery) throws IOException {
        BoolQueryBuilder query = buildQuery(searchQuery);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(query)
                .size(searchQuery.getLimit())
                .from(searchQuery.getOffset());
        SearchRequest searchRequest = new SearchRequest(index).source(sourceBuilder);
        ArrayNode resultArray = JsonNodeFactory.instance.arrayNode();
        ObjectMapper mapper = new ObjectMapper();
            SearchResponse searchResponse = getClient(index).search(searchRequest, RequestOptions.DEFAULT);
            for (SearchHit hit : searchResponse.getHits()) {
                JsonNode node = mapper.readValue(hit.getSourceAsString(), JsonNode.class);
                // TODO: Add draft mode condition
                if(node.get("_status") == null || node.get("_status").asBoolean()) {
                    resultArray.add(node);
                }
            }
            logger.debug("Total search records found " + resultArray.size());

        return resultArray;

    }

    @Override
    public String getServiceName() {
        return SUNBIRD_ELASTIC_SERVICE_NAME;
    }

    @Override
    public ComponentHealthInfo getHealthInfo() {
        ClusterHealthRequest request = new ClusterHealthRequest();
        try {
            ClusterHealthResponse health = getClient("schema").cluster().health(request, RequestOptions.DEFAULT);
            return new ComponentHealthInfo(getServiceName(), Arrays.asList("yellow", "green").contains(health.getStatus().name().toLowerCase()), "", "");
        } catch (IOException e) {
            logger.error("Elastic health status", e);
            return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, e.getMessage());
        }
    }

    /**
     * Builds the final query builder for given searchQuery
     *
     * @param searchQuery
     * @return
     */
    private BoolQueryBuilder buildQuery(SearchQuery searchQuery) {
        List<Filter> filters = searchQuery.getFilters();
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        for (Filter filter : filters) {
            String field = filter.getProperty();
            Object value = filter.getValue();
            FilterOperators operator = filter.getOperator();
            String path = filter.getPath();

            if (path != null) {
                field = path + "." + field;
            }
            switch (operator) {
            case eq:
                query = query.must(QueryBuilders.matchPhraseQuery(field, value));
                break;
            case neq:
                query = query.mustNot(QueryBuilders.matchPhraseQuery(field, value));
                break;
            case gt:
                query = query.must(QueryBuilders.rangeQuery(field).gt(value));
                break;
            case lt:
                query = query.must(QueryBuilders.rangeQuery(field).lt(value));
                break;
            case gte:
                query = query.must(QueryBuilders.rangeQuery(field).gte(value));
                break;
            case lte:
                query = query.must(QueryBuilders.rangeQuery(field).lte(value));
                break;
            case between:
                List<Object> objects = (List<Object>) value;
                query = query
                        .must(QueryBuilders.rangeQuery(field).from(objects.get(0)).to(objects.get(objects.size() - 1)));
                break;
            case or:
                List<Object> values = (List<Object>) value;
                query = query.must(QueryBuilders.termsQuery(String.format("%s.keyword", field), values));
                break;

            case contains:
                query = query.must(QueryBuilders.matchPhraseQuery(field, value));
                break;
            case startsWith:
                query = query.must(QueryBuilders.matchPhrasePrefixQuery(field, value.toString()));
                break;
            case endsWith:
                query = query.must(QueryBuilders.wildcardQuery(field, "*" + value));
                break;
            case notContains:
                query = query.mustNot(QueryBuilders.matchPhraseQuery(field, value));
                break;
            case notStartsWith:
                query = query.mustNot(QueryBuilders.matchPhrasePrefixQuery(field, value.toString()));
                break;
            case notEndsWith:
                query = query.mustNot(QueryBuilders.wildcardQuery(field, "*" + value));
                break;                
            case queryString:
                query = query.must(QueryBuilders.queryStringQuery(value.toString()));
                break;
            default:
                query = query.must(QueryBuilders.matchQuery(field, value));
                break;
            }
        }

        return query;
    }

}
