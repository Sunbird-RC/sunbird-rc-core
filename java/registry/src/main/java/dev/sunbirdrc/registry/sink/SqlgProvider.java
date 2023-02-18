package dev.sunbirdrc.registry.sink;

import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.DBConnectionInfo;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umlg.sqlg.structure.SqlgGraph;
import org.umlg.sqlg.structure.topology.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;

public class SqlgProvider extends DatabaseProvider {

    private Logger logger = LoggerFactory.getLogger(SqlgProvider.class);
    private SqlgGraph graph;
    private OSGraph customGraph;

    public SqlgProvider(DBConnectionInfo connectionInfo, String uuidPropertyName) {
        Configuration config = new BaseConfiguration();
        config.setProperty("jdbc.url", connectionInfo.getUri());
        config.setProperty("jdbc.username", connectionInfo.getUsername());
        config.setProperty("jdbc.password", connectionInfo.getPassword());
        config.setProperty("maxPoolSize", connectionInfo.getMaxPoolSize());
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
		if (propertyNames.size() > 0) {
	        createIndexByIndexType(graph, IndexType.NON_UNIQUE, label, propertyNames);
		} else {
			logger.info("Could not create single index for empty properties");
		}

    }

    @Override
    public void createCompositeIndex(Graph graph, String label, List<String> propertyNames) {
		if (propertyNames.size() > 0) {
	        ensureCompositeIndex(graph, label, propertyNames);
		} else {
			logger.info("Could not create composite index for empty properties");
		}
    }

    @Override
    public void createUniqueIndex(Graph graph, String label, List<String> propertyNames) {
		if (propertyNames.size() > 0) {
	        createIndexByIndexType(graph, IndexType.UNIQUE, label, propertyNames);
		} else {
			logger.info("Could not create unique index for empty properties");
		}
    }

    /**
     * creates sqlg index for a given index type(unique/non-unique)
     * 
     * @param graph
     * @param indexType
     * @param label
     * @param propertyNames
     */
    private void createIndexByIndexType(Graph graph, IndexType indexType, String label, List<String> propertyNames) {
        for (String propertyName : propertyNames) {
            List<String> properties1 = Arrays.stream(propertyName.split("[.]")).collect(Collectors.toList());
            if(properties1.size() == 1) {
                VertexLabel vertexLabel = getVertex(graph, label);
                List<PropertyColumn> properties = new ArrayList<>();
                properties.add(vertexLabel.getProperty(propertyName.split("[.]")[0]).get());
                ensureIndex(vertexLabel, indexType, properties);
            }
            else {
                createIndexByIndexType(graph, indexType, properties1.get(0), properties1.subList(1, properties1.size()));
            }
        }
//        createIndexByIndexType(graph, indexType, propertyNames.get(0).split("[.]")[0], propertyNames.subList(1, propertyNames.size()));
//        VertexLabel vertexLabel1 = getVertex(graph, propertyNames.get(0).split("[.]")[0]);
    }
    /**
     * ensures composite index for a given label for non-unique index type
     * @param graph
     * @param label
     * @param propertyNames
     */
    private void ensureCompositeIndex(Graph graph, String label, List<String> propertyNames) {
        VertexLabel vertexLabel = getVertex(graph, label);
        List<PropertyColumn> properties = new ArrayList<>();

        for (String propertyName : propertyNames) {
            properties.add(vertexLabel.getProperty(propertyName).get());
        }
        ensureIndex(vertexLabel, IndexType.NON_UNIQUE, properties);
    }
    /**
     * Ensures that the vertex table exist in the db.
     * @param graph
     * @param label
     * @return
     */
    private VertexLabel getVertex(Graph graph, String label) {
        return ((SqlgGraph) graph).getTopology().ensureVertexLabelExist(label);
    }
    /**
     * ensure index for a given label for non-unique index type
     * @param vertexLabel
     * @param indexType
     * @param properties
     */
    private void ensureIndex(VertexLabel vertexLabel, IndexType indexType, List<PropertyColumn> properties) {
        Index index = vertexLabel.ensureIndexExists(indexType, properties);
        logger.info(indexType + "index created for " + vertexLabel.getLabel() + " - " + index.getName());
    }

}
