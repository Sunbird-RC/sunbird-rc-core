package io.opensaber.registry.sink;

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class TinkerGraphProvider extends DatabaseProvider {

	private Logger logger = LoggerFactory.getLogger(TinkerGraphProvider.class);
	private TinkerGraph graph;
	private OSGraph osGraph;
	private Object environment;

	public TinkerGraphProvider(Environment inputEnv) {
		graph = TinkerGraph.open();
		osGraph = new OSGraph(graph, false);
		environment = inputEnv;
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

	@Override
	public OSGraph getOSGraph() {
		return osGraph;
	}
}
