package io.opensaber.registry.sink;

import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfo;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umlg.sqlg.structure.SqlgGraph;
import org.umlg.sqlg.structure.topology.Index;
import org.umlg.sqlg.structure.topology.IndexType;
import org.umlg.sqlg.structure.topology.PropertyColumn;
import org.umlg.sqlg.structure.topology.VertexLabel;

public class SqlgProvider extends DatabaseProvider {

    private Logger logger = LoggerFactory.getLogger(SqlgProvider.class);
    private SqlgGraph graph;
    private OSGraph customGraph;

    public SqlgProvider(DBConnectionInfo connectionInfo, String uuidPropertyName) {
        Configuration config = new BaseConfiguration();
        config.setProperty("jdbc.url", connectionInfo.getUri());
        config.setProperty("jdbc.username", connectionInfo.getUsername());
        config.setProperty("jdbc.password", connectionInfo.getPassword());
        setProvider(Constants.GraphDatabaseProvider.SQLG);
        setUuidPropertyName(uuidPropertyName);
        graph = SqlgGraph.open(config);
        customGraph = new OSGraph(graph, false);
    }

    @PostConstruct
    public void init() {
        logger.info("**************************************************************************");
        logger.info("Initializing SQLG DB instance ...");
        logger.info("**************************************************************************");
    }

    @PreDestroy
    public void shutdown() throws Exception {
        logger.info("**************************************************************************");
        logger.info("Gracefully shutting down SQLG DB instance ...");
        logger.info("**************************************************************************");
        graph.close();
    }

    @Override
    public OSGraph getOSGraph() {
        return customGraph;
    }

    @Override
    public String getId(Vertex vertex) {
        return (String) vertex.property(getUuidPropertyName()).value();
    }

    @Override
    public void createIndex(Graph graph, String label, List<String> propertyNames) {
        createIndexByIndexType(graph, IndexType.NON_UNIQUE, label, propertyNames);
    }

    @Override
    public void createUniqueIndex(Graph graph, String label, List<String> propertyNames) {
        createIndexByIndexType(graph, IndexType.UNIQUE, label, propertyNames);
    }

    /**
     * creates sqlg index for a given index type(unique/non-unique)
     * @param graph
     * @param indexType
     * @param label
     * @param propertyNames
     */
    private void createIndexByIndexType(Graph graph, IndexType indexType, String label, List<String> propertyNames) {
        VertexLabel vertexLabel = ((SqlgGraph) graph).getTopology().ensureVertexLabelExist(label);
        List<PropertyColumn> properties = new ArrayList<>();
        for (String propertyName : propertyNames) {
            properties.add(vertexLabel.getProperty(propertyName).get());
        }
        if (properties.size() > 0) {
            Index index = vertexLabel.ensureIndexExists(indexType, properties);
            logger.info(indexType + "index created for " + label + " - " + index.getName());
        }

    }
}
