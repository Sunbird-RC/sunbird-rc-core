package io.opensaber.registry.sink;

import com.steelbridgelabs.oss.neo4j.structure.Neo4JElementIdProvider;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import com.steelbridgelabs.oss.neo4j.structure.providers.DatabaseSequenceElementIdProvider;
import com.steelbridgelabs.oss.neo4j.structure.providers.Neo4JNativeElementIdProvider;
import io.opensaber.registry.middleware.util.Constants;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class Neo4jGraphProvider extends DatabaseProvider {

    private Logger logger = LoggerFactory.getLogger(Neo4jGraphProvider.class);
    private Graph graph;
    private Driver driver;

    public Neo4jGraphProvider(Environment environment) {
        Boolean isDatabaseEmbedded = Boolean.parseBoolean(environment.getProperty("database.neo4j.embedded"));
        if (isDatabaseEmbedded) {
            String graphDbLocation = environment.getProperty(Constants.NEO4J_DIRECTORY);
            logger.info(String.format("Initializing graph db at %s ...", graphDbLocation));
            Configuration config = new BaseConfiguration();
            config.setProperty(Neo4jGraph.CONFIG_DIRECTORY, graphDbLocation);
            config.setProperty("gremlin.neo4j.conf.cache_type", "none");
            graph = Neo4jGraph.open(config);
        } else {
            try {
                String databaseHost = environment.getProperty("database.neo4j.host");
                String databasePort = environment.getProperty("database.neo4j.port");
                Boolean profilerEnabled = Boolean.parseBoolean(environment.getProperty("database.neo4j.profiler_enabled"));
                driver = GraphDatabase.driver(String.format("bolt://%s:%s", databaseHost, databasePort), AuthTokens.none());
                Neo4JElementIdProvider<?> idProvider = new Neo4JNativeElementIdProvider();
                Neo4JGraph neo4JGraph = new Neo4JGraph(driver, idProvider, idProvider);
                neo4JGraph.setProfilerEnabled(profilerEnabled);
                graph = neo4JGraph;
                logger.info("Initializing remote graph db for ");
                logger.info("host: %s \n\t port: %s \n\t driver:  %s", databaseHost, databasePort,driver);

            } catch (Exception ex) {
                logger.error("Exception when initializing Neo4J DB connection...", ex);
                throw ex;
            }
        }
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
        if (driver != null) {
            driver.close();
        }
    }
}
