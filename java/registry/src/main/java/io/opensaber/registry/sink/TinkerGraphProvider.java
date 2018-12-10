package io.opensaber.registry.sink;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

public class TinkerGraphProvider extends DatabaseProvider {

	private Logger logger = LoggerFactory.getLogger(TinkerGraphProvider.class);
	private Graph graph;
	private Object environment;

	public TinkerGraphProvider(Environment inputEnv) {
		graph = TinkerGraph.open();
		environment = inputEnv;
	}

	@Override
	public Graph getGraphStore() {
		return graph;
	}

	@Override
	public Neo4JGraph getNeo4JGraph() {
		return null;
	}

	@PostConstruct
	public void init() {
		logger.info("**************************************************************************");
		logger.info("Initializing TinkerGraphDatabaseFactory instance ...");
		logger.info("**************************************************************************");
	}

	@PreDestroy
	public void shutdown() throws Exception {
		logger.info("**************************************************************************");
		logger.info("Gracefully shutting down TinkerGraphDatabaseFactory instance ...");
		logger.info("**************************************************************************");
		graph.close();
	}
}
