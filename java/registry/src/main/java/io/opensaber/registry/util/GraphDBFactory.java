package io.opensaber.registry.util;

import io.opensaber.registry.middleware.util.Constants;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.tinkerpop.api.impl.Neo4jGraphAPIImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class GraphDBFactory {

	private Logger logger = LoggerFactory.getLogger(GraphDBFactory.class);

	private Environment environment;

	private Neo4jGraph graph;
	private GraphDatabaseService graphDBService;

	public GraphDBFactory(Environment environment) {
		this.environment = environment;
		initializeGraphDb();
	}

	public static Graph getEmptyGraph() {
		return TinkerGraph.open();
	}

	private void initializeGraphDb() {
		String graphDbLocation = environment.getProperty(Constants.NEO4J_DIRECTORY);
		logger.info(String.format("Initializing graph db at %s ...", graphDbLocation));
		Configuration config = new BaseConfiguration();
		config.setProperty(Neo4jGraph.CONFIG_DIRECTORY, graphDbLocation);
		config.setProperty("gremlin.neo4j.conf.cache_type", "none");
		graph = Neo4jGraph.open(config);
	}

	private Neo4jGraph getGraphDB() {
		return graph;
	}

	public GraphDatabaseService getGraphDatabaseService() {
		if (graphDBService == null || !graphDBService.isAvailable(0)) {
			graphDBService = ((Neo4jGraphAPIImpl) getGraphDB().getBaseGraph()).getGraphDatabase();
		}
		return graphDBService;
	}

	@PostConstruct
	public void init() {
		logger.info("**************************************************************************");
		logger.info("Initializing GraphDBFactory instance ...");
		logger.info("**************************************************************************");
	}

	@PreDestroy
	public void destroy() throws Exception {
		logger.info("**************************************************************************");
		logger.info("Gracefully shutting down GraphDBFactory instance ...");
		logger.info("**************************************************************************");
		graphDBService.shutdown();
		graph.close();
	}
}
