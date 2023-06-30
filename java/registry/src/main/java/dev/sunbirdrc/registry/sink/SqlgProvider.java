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
	        ensureCompositeIndex(graph, label, propertyNames, IndexType.NON_UNIQUE);
		} else {
			logger.info("Could not create composite index for empty properties");
		}
    }

    @Override
    public void createCompositeUniqueIndex(Graph graph, String label, List<String> propertyNames) {
        if (propertyNames.size() > 0) {
            ensureCompositeIndex(graph, label, propertyNames, IndexType.UNIQUE);
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
            List<String> indexPropertyPath =  Arrays.stream(propertyName.split("[.]")).collect(Collectors.toList());
            int indexPropertiesLength = indexPropertyPath.size();
            if(indexPropertiesLength == 1) {
                createIndexOnVertex(label, indexPropertyPath.get(0), indexType, graph);
            }
            else {
                createIndexOnVertex(indexPropertyPath.get(indexPropertiesLength - 2),
                        indexPropertyPath.get(indexPropertiesLength - 1), indexType, graph);
            }
        }
    }

    private void createIndexOnVertex(String label, String property, IndexType indexType, Graph graph) {
        VertexLabel vertexLabel = getVertex(graph, label);
        List<PropertyColumn> properties = new ArrayList<>();
        Optional<PropertyColumn> propertyColumnOptional = vertexLabel.getProperty(property);
        propertyColumnOptional.ifPresent(properties::add);
        propertyColumnOptional.orElseThrow(() -> new RuntimeException("Property not found"));
        ensureIndex(vertexLabel, indexType, properties);
    }

    /**
     * ensures composite index for a given label for non-unique index type
     *
     * @param graph
     * @param label
     * @param propertyNames
     * @param indexType
     */
    private void ensureCompositeIndex(Graph graph, String label, List<String> propertyNames, IndexType indexType) {
        VertexLabel vertexLabel = getVertex(graph, label);
        List<PropertyColumn> properties = new ArrayList<>();

        for (String propertyName : propertyNames) {
            List<String> indexPropertyPath =  Arrays.stream(propertyName.split("[.]")).collect(Collectors.toList());
            int indexPropertiesLength = indexPropertyPath.size();
            if(indexPropertiesLength == 1) {
                Optional<PropertyColumn> property = vertexLabel.getProperty(propertyName);
                property.ifPresent(properties::add);
                property.orElseThrow(() -> new RuntimeException("Property not found"));
            }
            else {
                // composite fields should be of the same entity type
                vertexLabel = getVertex(graph, indexPropertyPath.get(indexPropertiesLength - 2));
                Optional<PropertyColumn> property = vertexLabel.getProperty(indexPropertyPath.get(indexPropertiesLength - 1));
                property.ifPresent(properties::add);
                property.orElseThrow(() -> new RuntimeException("Property not found"));
            }

        }
        if (properties.size() > 0) {
            ensureIndex(vertexLabel, indexType, properties);
        }
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
