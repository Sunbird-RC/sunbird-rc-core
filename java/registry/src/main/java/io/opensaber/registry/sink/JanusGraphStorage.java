package io.opensaber.registry.sink;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.janusgraph.core.JanusGraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class JanusGraphStorage extends DatabaseProvider {

    private Logger logger = LoggerFactory.getLogger(JanusGraphStorage.class);
    private Graph graph;

    public JanusGraphStorage(Environment environment) {
        String graphFactory = environment.getProperty("database.janus_cassandra.graphFactory");
        String storageBackend = environment.getProperty("database.janus_cassandra.storage.backend");
        String hostname = environment.getProperty("database.janus_cassandra.storage.hostname");
        String storageKeyspace = environment.getProperty("database.janus_cassandra.storage.keyspace");
        String dbCacheSize = environment.getProperty("database.janus_cassandra.db.cache.size");
        String dbCacheCleanUpWaitTime = environment.getProperty("database.janus_cassandra.db.cache.clean.wait");
        String searchIndex = environment.getProperty("database.janus_cassandra.index.storage.backend");
        String searchHostname = environment.getProperty("database.janus_cassandra.index.hostname");

        Configuration config = new BaseConfiguration();
        config.setProperty("gremlin.graph", graphFactory);
        config.setProperty("storage.backend", storageBackend);
        config.setProperty("storage.cassandra.keyspace", storageKeyspace);
        config.setProperty("storage.hostname", hostname);
        config.setProperty("index.search.backend", searchIndex);
        config.setProperty("index.search.hostname", searchHostname);
        config.setProperty("cache.db-cache-size", Float.parseFloat(dbCacheSize));
        config.setProperty("cache.db-cache-clean-wait", Integer.parseInt(dbCacheCleanUpWaitTime));
        graph = JanusGraphFactory.open(config);
    }

    @Override
    public Graph getGraphStore() {
        return graph;
    }

    @PostConstruct
    public void init() {
        logger.info("**************************************************************************");
        logger.info("Initializing Janus GraphDB instance ...");
        logger.info("**************************************************************************");
    }

    @PreDestroy
    public void shutdown() throws Exception {
        logger.info("**************************************************************************");
        logger.info("Gracefully shutting down Janus GraphDB instance ...");
        logger.info("**************************************************************************");
        graph.close();
    }
}
