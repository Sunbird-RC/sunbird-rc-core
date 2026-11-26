package dev.sunbirdrc.registry.sink;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.DBConnectionInfo;
import dev.sunbirdrc.registry.sink.jdbc.JdbcGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.util.List;

/**
 * Database provider for YugabyteDB using pure JDBC (bypassing SQLG).
 * Connects to YugabyteDB's YSQL API (PostgreSQL-compatible, typically port 5433).
 * Uses READ COMMITTED isolation level for YugabyteDB compatibility.
 */
public class YugabyteJdbcProvider extends DatabaseProvider {

    private static final Logger logger = LoggerFactory.getLogger(YugabyteJdbcProvider.class);

    private JdbcGraph graph;
    private HikariDataSource dataSource;
    private OSGraph customGraph;

    public YugabyteJdbcProvider(DBConnectionInfo connectionInfo, String uuidPropertyName) {
        initializeDataSource(connectionInfo);
        initializeGraph();
        setProvider(Constants.GraphDatabaseProvider.YUGABYTE);
        setUuidPropertyName(uuidPropertyName);
    }

    /**
     * Initialize HikariCP connection pool for YugabyteDB YSQL.
     */
    private void initializeDataSource(DBConnectionInfo connectionInfo) {
        HikariConfig config = new HikariConfig();

        // JDBC URL for YugabyteDB YSQL (PostgreSQL compatible)
        config.setJdbcUrl(connectionInfo.getUri());
        config.setUsername(connectionInfo.getUsername());
        config.setPassword(connectionInfo.getPassword());

        // Connection pool settings
        int maxPoolSize = connectionInfo.getMaxPoolSize() > 0 ? connectionInfo.getMaxPoolSize() : 10;
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(Math.min(5, maxPoolSize));

        // Connection timeout settings
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes

        // PostgreSQL/YugabyteDB specific settings
        config.setDriverClassName("org.postgresql.Driver");

        // YugabyteDB-specific connection properties
        // Use READ COMMITTED isolation level (required for YugabyteDB compatibility)
        config.addDataSourceProperty("defaultRowFetchSize", "1000");
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");

        // Pool name for monitoring
        config.setPoolName("YugabyteDB-JDBC-Pool-" + connectionInfo.getShardId());

        logger.info("Initializing YugabyteDB JDBC connection pool: {}", connectionInfo.getUri());
        dataSource = new HikariDataSource(config);

        // Test the connection
        try (Connection conn = dataSource.getConnection()) {
            logger.info("Successfully connected to YugabyteDB at {}", connectionInfo.getUri());
        } catch (Exception e) {
            logger.error("Failed to connect to YugabyteDB", e);
            throw new RuntimeException("Failed to connect to YugabyteDB", e);
        }
    }

    /**
     * Initialize the JdbcGraph.
     */
    private void initializeGraph() {
        graph = new JdbcGraph(dataSource);
        customGraph = new OSGraph(graph, false);
    }

    @PostConstruct
    public void init() {
        logger.info("**************************************************************************");
        logger.info("Initializing YugabyteDB JDBC provider ...");
        logger.info("**************************************************************************");
    }

    @PreDestroy
    @Override
    public void shutdown() throws Exception {
        logger.info("**************************************************************************");
        logger.info("Gracefully shutting down YugabyteDB JDBC provider ...");
        logger.info("**************************************************************************");

        if (graph != null) {
            try {
                graph.close();
            } catch (Exception e) {
                logger.error("Error closing JdbcGraph", e);
            }
        }

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public OSGraph getOSGraph() {
        return customGraph;
    }

    @Override
    public String getId(org.apache.tinkerpop.gremlin.structure.Vertex vertex) {
        return (String) vertex.property(getUuidPropertyName()).value();
    }

    /**
     * Create a non-unique index on a property for a given label.
     */
    @Override
    public void createIndex(Graph graph, String label, List<String> propertyNames) {
        if (propertyNames.size() > 0) {
            for (String propertyName : propertyNames) {
                this.graph.createIndex(label, propertyName);
            }
        } else {
            logger.info("Could not create index for empty properties");
        }
    }

    /**
     * Create a unique index on a property for a given label.
     */
    @Override
    public void createUniqueIndex(Graph graph, String label, List<String> propertyNames) {
        if (propertyNames.size() > 0) {
            for (String propertyName : propertyNames) {
                this.graph.createUniqueIndex(label, propertyName);
            }
        } else {
            logger.info("Could not create unique index for empty properties");
        }
    }

    /**
     * Create a composite index on multiple properties for a given label.
     */
    @Override
    public void createCompositeIndex(Graph graph, String label, List<String> propertyNames) {
        if (propertyNames.size() > 0) {
            this.graph.createCompositeIndex(label, propertyNames);
        } else {
            logger.info("Could not create composite index for empty properties");
        }
    }

    /**
     * Get the underlying JdbcGraph for direct access if needed.
     */
    public JdbcGraph getJdbcGraph() {
        return graph;
    }

    /**
     * Get the HikariDataSource for monitoring or direct SQL operations.
     */
    public HikariDataSource getDataSource() {
        return dataSource;
    }
}
