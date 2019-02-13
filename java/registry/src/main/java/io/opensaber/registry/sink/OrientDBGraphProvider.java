package io.opensaber.registry.sink;

import io.opensaber.registry.middleware.util.Constants;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class OrientDBGraphProvider extends DatabaseProvider {

	private Logger logger = LoggerFactory.getLogger(OrientDBGraphProvider.class);
	private OrientGraph graph;
	private OSGraph customGraph;

	public OrientDBGraphProvider(Environment environment) {
		String graphDbLocation = environment.getProperty(Constants.ORIENTDB_DIRECTORY);
		Configuration config = new BaseConfiguration();
		config.setProperty(OrientGraph.CONFIG_URL, String.format("embedded:%s", graphDbLocation));
		config.setProperty(OrientGraph.CONFIG_TRANSACTIONAL, true);
		graph = OrientGraph.open(config);
		customGraph = new OSGraph(graph, false);
	}

	@PostConstruct
	public void init() {
		logger.info("**************************************************************************");
		logger.info("Initializing OrientGraph DB instance ...");
		logger.info("**************************************************************************");
	}

	@PreDestroy
	public void shutdown() throws Exception {
		logger.info("**************************************************************************");
		logger.info("Gracefully shutting down OrientGraph DB instance ...");
		logger.info("**************************************************************************");
		graph.close();
	}

	@Override
	public OSGraph getOSGraph() {
		return customGraph;
	}
}
