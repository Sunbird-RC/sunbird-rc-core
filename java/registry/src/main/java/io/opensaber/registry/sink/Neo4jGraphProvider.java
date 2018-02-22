package io.opensaber.registry.sink;

import io.opensaber.registry.middleware.util.Constants;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class Neo4jGraphProvider implements DatabaseProvider {

    private Logger logger = LoggerFactory.getLogger(Neo4jGraphProvider.class);
    private Environment environment;
    private Graph graph;

    public Neo4jGraphProvider(Environment environment) {
        this.environment = environment;
        String graphDbLocation = environment.getProperty(Constants.NEO4J_DIRECTORY);
        logger.info(String.format("Initializing graph db at %s ...", graphDbLocation));
        Configuration config = new BaseConfiguration();
        config.setProperty(Neo4jGraph.CONFIG_DIRECTORY, graphDbLocation);
        config.setProperty("gremlin.neo4j.conf.cache_type", "none");
        graph = Neo4jGraph.open(config);
    }

    @Override
    public Graph getGraphStore() {
        return graph;
    }

    @PostConstruct
    public void init() {
        logger.info("**************************************************************************");
        logger.info("Initializing Graph DB instance ...");
        logger.info("**************************************************************************");
    }

    @PreDestroy
    public void shutdown() throws Exception {
        logger.info("**************************************************************************");
        logger.info("Gracefully shutting down Graph DB instance ...");
        logger.info("**************************************************************************");
        graph.close();
    }
}
