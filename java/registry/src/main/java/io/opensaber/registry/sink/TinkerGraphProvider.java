package io.opensaber.registry.sink;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import io.opensaber.registry.middleware.util.Constants;

public class TinkerGraphProvider extends DatabaseProvider {

	 private Logger logger = LoggerFactory.getLogger(TinkerGraphProvider.class);
	    private Graph graph;
		private Object environment;

	     public TinkerGraphProvider(Environment environment2) {
			graph = TinkerGraph.open();
		}

		@Override
	    public Graph getGraphStore() {
	        return graph;
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
