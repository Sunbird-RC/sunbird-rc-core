package io.opensaber.elastic;

import io.opensaber.pojos.SearchQuery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** This method runs when the application is started in order to add all the indcies to the elastic search
     * @param indices
     * @throws RuntimeException
     */
    public static void init(Set<String> indices) throws RuntimeException {
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
    public RestStatus addEntity(String index, String entityId, Map<String, Object> inputEntity) {
        logger.debug("addEntity starts with index {} and entityId {}", index, entityId);
        IndexResponse response = null;
        try {
            response = getClient(index).index(new IndexRequest(index, searchType, entityId).source(inputEntity), RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("Exception in adding record to ElasticSearch", e);
        }
        return response.status();
    }

    @Override
    public Map<String, Object> readEntity(String index, String osid) {
        return null;
    }

    @Override
    public boolean updateEntity(String index, Map<String, Object> inputEntity, String osid) {
        return false;
    }

    @Override
    public void deleteEntity(String index, String osid) {

    }

    @Override
    public Map<String, Object> search(String index, SearchQuery searchQuery) {
        return null;
    }

}
