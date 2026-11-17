package dev.sunbirdrc.registry.sink.jdbc;

import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom TinkerPop Graph implementation over JDBC for YugabyteDB.
 * Uses SQLG-compatible schema with individual columns for properties.
 * Creates per-entity tables using SQLG naming convention (V_ prefix for vertices, E_ prefix for edges).
 */
public class JdbcGraph implements Graph {
    private static final Logger logger = LoggerFactory.getLogger(JdbcGraph.class);

    private final DataSource dataSource;
    private final JdbcGraphFeatures features;
    private final Map<String, Object> variables;
    private boolean closed = false;

    // Thread-local connection for transaction support
    private final ThreadLocal<Connection> threadLocalConnection = new ThreadLocal<>();
    private final ThreadLocal<JdbcTransaction> threadLocalTransaction = new ThreadLocal<>();

    // Table registry for tracking existing per-entity tables
    private final Set<String> existingVertexTables = ConcurrentHashMap.newKeySet();
    private final Set<String> existingEdgeTables = ConcurrentHashMap.newKeySet();

    // Column registry for tracking columns per table
    private final Map<String, Set<String>> tableColumns = new ConcurrentHashMap<>();

    // Edge table schema registry: tableName -> EdgeTableSchema
    private final Map<String, EdgeTableSchema> edgeTableSchemas = new ConcurrentHashMap<>();

    /**
     * Holds edge table schema information for SQLG-style edge tables.
     */
    private static class EdgeTableSchema {
        final String outVertexLabel;
        final String inVertexLabel;

        EdgeTableSchema(String outVertexLabel, String inVertexLabel) {
            this.outVertexLabel = outVertexLabel;
            this.inVertexLabel = inVertexLabel;
        }
    }

    public JdbcGraph(DataSource dataSource) {
        this.dataSource = dataSource;
        this.features = new JdbcGraphFeatures();
        this.variables = new ConcurrentHashMap<>();
        initializeSchema();
    }

    /**
     * Convert a label to a vertex table name using SQLG convention.
     */
    public static String getVertexTableName(String label) {
        return "V_" + sanitizeLabel(label);
    }

    /**
     * Convert a label to an edge table name using SQLG convention.
     */
    public static String getEdgeTableName(String label) {
        return "E_" + sanitizeLabel(label);
    }

    /**
     * Sanitize label for use in table names.
     */
    private static String sanitizeLabel(String label) {
        return label.replace("-", "_").replace(" ", "_").replace(".", "_");
    }

    /**
     * Get SQL type for a given Java value.
     */
    private String getSqlType(Object value) {
        if (value instanceof Integer) return "INTEGER";
        if (value instanceof Long) return "BIGINT";
        if (value instanceof Double) return "DOUBLE PRECISION";
        if (value instanceof Float) return "REAL";
        if (value instanceof Boolean) return "BOOLEAN";
        if (value instanceof Collection || value instanceof Map) return "TEXT"; // Store as JSON string
        return "TEXT";  // Default for strings and complex types
    }

    /**
     * Initialize the database schema - load existing tables and their columns from database metadata.
     */
    private void initializeSchema() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Load existing V_* and E_* tables into registry
            try (ResultSet tables = metaData.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (tableName.startsWith("V_")) {
                        existingVertexTables.add(tableName);
                        loadTableColumns(conn, tableName);
                        logger.debug("Found existing vertex table: {}", tableName);
                    } else if (tableName.startsWith("E_")) {
                        existingEdgeTables.add(tableName);
                        loadEdgeTableSchema(conn, tableName);
                        logger.debug("Found existing edge table: {}", tableName);
                    }
                }
            }

            logger.info("JdbcGraph schema initialized. Found {} vertex tables and {} edge tables",
                    existingVertexTables.size(), existingEdgeTables.size());
        } catch (SQLException e) {
            logger.error("Failed to initialize JdbcGraph schema", e);
            throw new RuntimeException("Failed to initialize JdbcGraph schema", e);
        }
    }

    /**
     * Load existing columns for a table from database metadata.
     */
    private void loadTableColumns(Connection conn, String tableName) throws SQLException {
        Set<String> columns = ConcurrentHashMap.newKeySet();
        DatabaseMetaData metaData = conn.getMetaData();

        try (ResultSet rs = metaData.getColumns(null, "public", tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                columns.add(columnName);
            }
        }

        tableColumns.put(tableName, columns);
        logger.debug("Loaded {} columns for table {}", columns.size(), tableName);
    }

    /**
     * Load edge table schema from existing columns.
     */
    private void loadEdgeTableSchema(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        String outVertexLabel = null;
        String inVertexLabel = null;

        try (ResultSet rs = metaData.getColumns(null, "public", tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                // SQLG format: public.{label}__O for out vertex, public.{label}__I for in vertex
                if (columnName.startsWith("public.") && columnName.endsWith("__O")) {
                    outVertexLabel = columnName.substring(7, columnName.length() - 3);
                } else if (columnName.startsWith("public.") && columnName.endsWith("__I")) {
                    inVertexLabel = columnName.substring(7, columnName.length() - 3);
                }
            }
        }

        if (outVertexLabel != null && inVertexLabel != null) {
            edgeTableSchemas.put(tableName, new EdgeTableSchema(outVertexLabel, inVertexLabel));
            logger.debug("Loaded edge schema for {}: out={}, in={}", tableName, outVertexLabel, inVertexLabel);
        }
    }

    /**
     * Refresh the vertex table cache by re-scanning database metadata.
     * Called when a table lookup fails to ensure we haven't missed any tables.
     */
    public void refreshVertexTableCache() {
        logger.debug("Refreshing vertex table cache from database metadata");
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet tables = metaData.getTables(null, "public", "V_%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (existingVertexTables.add(tableName)) {
                        loadTableColumns(conn, tableName);
                        logger.info("Discovered new vertex table during refresh: {}", tableName);
                    }
                }
            }

            logger.debug("Vertex table cache refresh complete. Total tables: {}", existingVertexTables.size());
        } catch (SQLException e) {
            logger.error("Failed to refresh vertex table cache", e);
        }
    }

    /**
     * Ensure a vertex table exists for the given label.
     * Creates table with only the ID column initially; other columns are added dynamically.
     */
    private void ensureVertexTableExists(String label) {
        String tableName = getVertexTableName(label);
        if (existingVertexTables.contains(tableName)) {
            return;
        }

        synchronized (existingVertexTables) {
            if (existingVertexTables.contains(tableName)) {
                return;
            }

            // SQLG-style: Create table with BIGSERIAL primary key named "ID"
            String sql = "CREATE TABLE IF NOT EXISTS \"" + tableName + "\" (" +
                    "\"ID\" BIGSERIAL PRIMARY KEY)";

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);

                existingVertexTables.add(tableName);

                // Initialize column registry with ID column
                Set<String> columns = ConcurrentHashMap.newKeySet();
                columns.add("ID");
                tableColumns.put(tableName, columns);

                logger.info("Created vertex table: {}", tableName);
            } catch (SQLException e) {
                logger.error("Failed to create vertex table: " + tableName, e);
                throw new RuntimeException("Failed to create vertex table: " + tableName, e);
            }
        }
    }

    /**
     * Ensure a column exists in a table, adding it if necessary.
     */
    private void ensureColumnExists(String tableName, String columnName, Object value) {
        Set<String> columns = tableColumns.computeIfAbsent(tableName, k -> {
            try (Connection conn = dataSource.getConnection()) {
                Set<String> cols = ConcurrentHashMap.newKeySet();
                loadTableColumnsInto(conn, tableName, cols);
                return cols;
            } catch (SQLException e) {
                logger.error("Failed to load columns for table: " + tableName, e);
                return ConcurrentHashMap.newKeySet();
            }
        });

        if (columns.contains(columnName)) {
            return;
        }

        synchronized (columns) {
            if (columns.contains(columnName)) {
                return;
            }

            String sqlType = getSqlType(value);
            // Quote column name to handle special characters like @
            String sql = "ALTER TABLE \"" + tableName + "\" ADD COLUMN IF NOT EXISTS \"" +
                         columnName + "\" " + sqlType;

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                columns.add(columnName);
                logger.debug("Added column '{}' ({}) to table '{}'", columnName, sqlType, tableName);
            } catch (SQLException e) {
                // Column might already exist from concurrent request
                if (!e.getMessage().contains("already exists")) {
                    logger.error("Failed to add column {} to table {}", columnName, tableName, e);
                    throw new RuntimeException("Failed to add column", e);
                }
                columns.add(columnName);
            }
        }
    }

    /**
     * Helper to load columns into a provided set.
     */
    private void loadTableColumnsInto(Connection conn, String tableName, Set<String> columns) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, "public", tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
    }

    /**
     * Ensure an edge table exists with SQLG-style schema.
     */
    private void ensureEdgeTableExists(String edgeLabel, String outVertexLabel, String inVertexLabel) {
        String tableName = getEdgeTableName(edgeLabel);

        // Check if table already exists with correct schema
        EdgeTableSchema existingSchema = edgeTableSchemas.get(tableName);
        if (existingSchema != null &&
            existingSchema.outVertexLabel.equals(outVertexLabel) &&
            existingSchema.inVertexLabel.equals(inVertexLabel)) {
            return;
        }

        synchronized (existingEdgeTables) {
            existingSchema = edgeTableSchemas.get(tableName);
            if (existingSchema != null &&
                existingSchema.outVertexLabel.equals(outVertexLabel) &&
                existingSchema.inVertexLabel.equals(inVertexLabel)) {
                return;
            }

            // SQLG-style column names
            String outColumn = "public." + outVertexLabel + "__O";
            String inColumn = "public." + inVertexLabel + "__I";

            if (!existingEdgeTables.contains(tableName)) {
                // Create new edge table
                String sql = "CREATE TABLE IF NOT EXISTS \"" + tableName + "\" (" +
                        "\"ID\" BIGSERIAL PRIMARY KEY," +
                        "\"" + outColumn + "\" BIGINT," +
                        "\"" + inColumn + "\" BIGINT)";

                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);

                    // Create indexes for edge lookups
                    String outIndexSql = "CREATE INDEX IF NOT EXISTS \"idx_" + tableName + "_out\" " +
                            "ON \"" + tableName + "\" (\"" + outColumn + "\")";
                    String inIndexSql = "CREATE INDEX IF NOT EXISTS \"idx_" + tableName + "_in\" " +
                            "ON \"" + tableName + "\" (\"" + inColumn + "\")";
                    stmt.execute(outIndexSql);
                    stmt.execute(inIndexSql);

                    existingEdgeTables.add(tableName);
                    edgeTableSchemas.put(tableName, new EdgeTableSchema(outVertexLabel, inVertexLabel));
                    logger.info("Created edge table: {} with columns {}, {}", tableName, outColumn, inColumn);
                } catch (SQLException e) {
                    logger.error("Failed to create edge table: " + tableName, e);
                    throw new RuntimeException("Failed to create edge table: " + tableName, e);
                }
            } else {
                // Table exists but might need additional columns for different vertex type combinations
                // Add new columns if they don't exist
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    String addOutCol = "ALTER TABLE \"" + tableName + "\" ADD COLUMN IF NOT EXISTS \"" +
                                       outColumn + "\" BIGINT";
                    String addInCol = "ALTER TABLE \"" + tableName + "\" ADD COLUMN IF NOT EXISTS \"" +
                                      inColumn + "\" BIGINT";
                    stmt.execute(addOutCol);
                    stmt.execute(addInCol);

                    edgeTableSchemas.put(tableName, new EdgeTableSchema(outVertexLabel, inVertexLabel));
                    logger.debug("Added columns to existing edge table: {} -> {}, {}", tableName, outColumn, inColumn);
                } catch (SQLException e) {
                    logger.error("Failed to add columns to edge table: " + tableName, e);
                    throw new RuntimeException("Failed to add columns to edge table", e);
                }
            }
        }
    }

    @Override
    public Vertex addVertex(Object... keyValues) {
        String label = Vertex.DEFAULT_LABEL;
        Map<String, Object> properties = new LinkedHashMap<>();

        // Parse key-value pairs with robust T enum handling
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object keyObj = keyValues[i];
            Object value = keyValues[i + 1];

            // Check for T.label enum directly (most common case)
            if (keyObj == T.label) {
                label = value.toString();
            } else if (keyObj == T.id) {
                // Ignore T.id - we use auto-generated BIGSERIAL IDs
                logger.debug("Ignoring T.id={}, using auto-generated BIGSERIAL", value);
            } else {
                // Fallback to string comparison
                String key = keyObj.toString();
                if ("label".equals(key) || T.label.getAccessor().equals(key)) {
                    label = value.toString();
                } else if ("id".equals(key) || "~id".equals(key) || T.id.getAccessor().equals(key)) {
                    // Ignore id - we use auto-generated BIGSERIAL IDs
                    logger.debug("Ignoring id={}, using auto-generated BIGSERIAL", value);
                } else {
                    properties.put(key, value);
                }
            }
        }

        logger.debug("Creating vertex with label='{}'", label);

        // Ensure the table for this label exists
        ensureVertexTableExists(label);

        String tableName = getVertexTableName(label);

        // Ensure all columns exist
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            ensureColumnExists(tableName, entry.getKey(), entry.getValue());
        }

        // Build dynamic INSERT with column names
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (columns.length() > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append("\"").append(entry.getKey()).append("\"");
            placeholders.append("?");
            params.add(convertValueForStorage(entry.getValue()));
        }

        String sql;
        if (columns.length() > 0) {
            sql = "INSERT INTO \"" + tableName + "\" (" + columns + ") VALUES (" + placeholders + ") RETURNING \"ID\"";
        } else {
            sql = "INSERT INTO \"" + tableName + "\" DEFAULT VALUES RETURNING \"ID\"";
        }

        try {
            Connection conn = getConnection();
            Long id;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        id = rs.getLong("ID");
                    } else {
                        throw new SQLException("Failed to get generated ID");
                    }
                }
            }

            if (!isInTransaction()) {
                conn.close();
            }

            logger.debug("Created vertex with label='{}', ID={}", label, id);
            return new JdbcVertex(this, id, label, properties);
        } catch (SQLException e) {
            logger.error("Failed to add vertex with label '{}': {}", label, e.getMessage());
            throw new RuntimeException("Failed to add vertex", e);
        }
    }

    /**
     * Convert a value for SQL storage.
     */
    private Object convertValueForStorage(Object value) {
        if (value instanceof Collection || value instanceof Map) {
            return valueToJson(value);
        }
        return value;
    }

    @Override
    public <C extends GraphComputer> C compute(Class<C> graphComputerClass) throws IllegalArgumentException {
        throw Graph.Exceptions.graphComputerNotSupported();
    }

    @Override
    public GraphComputer compute() throws IllegalArgumentException {
        throw Graph.Exceptions.graphComputerNotSupported();
    }

    @Override
    public Iterator<Vertex> vertices(Object... vertexIds) {
        if (vertexIds.length == 0) {
            return getAllVertices();
        }

        List<Vertex> vertices = new ArrayList<>();
        for (Object id : vertexIds) {
            Long longId = toLongId(id);
            if (longId != null) {
                Vertex v = getVertexById(longId);
                if (v != null) {
                    vertices.add(v);
                }
            }
        }
        return vertices.iterator();
    }

    /**
     * Convert an object to Long ID.
     */
    private Long toLongId(Object id) {
        if (id instanceof Long) {
            return (Long) id;
        } else if (id instanceof Number) {
            return ((Number) id).longValue();
        } else if (id instanceof String) {
            try {
                return Long.parseLong((String) id);
            } catch (NumberFormatException e) {
                logger.warn("Cannot convert '{}' to Long ID", id);
                return null;
            }
        }
        return null;
    }

    /**
     * Get all vertices across all vertex tables.
     */
    private Iterator<Vertex> getAllVertices() {
        List<Vertex> vertices = new ArrayList<>();

        for (String tableName : existingVertexTables) {
            String label = tableName.substring(2); // Remove "V_" prefix
            String sql = "SELECT * FROM \"" + tableName + "\"";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    vertices.add(resultSetToVertex(rs, label));
                }
            } catch (SQLException e) {
                logger.error("Failed to get vertices from table: " + tableName, e);
                throw new RuntimeException("Failed to get vertices from table: " + tableName, e);
            }
        }

        return vertices.iterator();
    }

    /**
     * Get a vertex by ID, searching across all vertex tables.
     */
    public Vertex getVertexById(Long id) {
        for (String tableName : existingVertexTables) {
            String label = tableName.substring(2); // Remove "V_" prefix
            String sql = "SELECT * FROM \"" + tableName + "\" WHERE \"ID\" = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return resultSetToVertex(rs, label);
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to get vertex by id: " + id + " from table: " + tableName, e);
                throw new RuntimeException("Failed to get vertex by id", e);
            }
        }

        return null;
    }

    /**
     * Get a vertex by ID with a label hint for optimization.
     */
    public Vertex getVertexById(Long id, String labelHint) {
        if (labelHint != null) {
            String tableName = getVertexTableName(labelHint);
            if (existingVertexTables.contains(tableName)) {
                String sql = "SELECT * FROM \"" + tableName + "\" WHERE \"ID\" = ?";

                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return resultSetToVertex(rs, labelHint);
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Failed to get vertex by id: " + id, e);
                    throw new RuntimeException("Failed to get vertex by id", e);
                }
            }
        }

        // Fall back to searching all tables
        return getVertexById(id);
    }

    /**
     * Get vertices by a property value, optionally filtered by label.
     * This efficiently queries the specific V_{label} table using SQL.
     *
     * @param label Optional label to filter by (maps to V_{label} table)
     * @param propertyKey The property key to search
     * @param propertyValue The property value to match
     * @return Iterator of matching vertices
     */
    public Iterator<Vertex> getVerticesByProperty(String label, String propertyKey, Object propertyValue) {
        List<Vertex> vertices = new ArrayList<>();

        logger.debug("getVerticesByProperty: label={}, propertyKey={}, propertyValue={}",
                     label, propertyKey, propertyValue);

        Set<String> tablesToQuery;
        if (label != null) {
            String tableName = getVertexTableName(label);

            // First check the cache
            if (!existingVertexTables.contains(tableName)) {
                // Not in cache - try refreshing from database
                logger.debug("Table '{}' not in cache (size={}), attempting refresh",
                            tableName, existingVertexTables.size());
                refreshVertexTableCache();
            }

            if (existingVertexTables.contains(tableName)) {
                tablesToQuery = Collections.singleton(tableName);
            } else {
                // Still not found after refresh - table truly doesn't exist
                logger.warn("Table '{}' does not exist even after cache refresh. " +
                           "Label='{}', PropertyKey='{}', PropertyValue='{}'",
                           tableName, label, propertyKey, propertyValue);
                return Collections.emptyIterator();
            }
        } else {
            // No label specified - refresh and search all tables
            refreshVertexTableCache();
            tablesToQuery = new HashSet<>(existingVertexTables);
        }

        for (String tableName : tablesToQuery) {
            String tableLabel = tableName.substring(2); // Remove "V_" prefix

            // Check if the column exists in this table
            Set<String> columns = tableColumns.get(tableName);
            if (columns == null || !columns.contains(propertyKey)) {
                logger.debug("Column '{}' not found in table '{}', skipping", propertyKey, tableName);
                continue;
            }

            // Query using the column directly
            String sql = "SELECT * FROM \"" + tableName + "\" WHERE \"" + propertyKey + "\" = ?";

            logger.debug("Executing SQL on table '{}': propertyKey={}, propertyValue={}",
                         tableName, propertyKey, propertyValue);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, propertyValue);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        vertices.add(resultSetToVertex(rs, tableLabel));
                        logger.debug("Found vertex with ID={} in table '{}'", rs.getLong("ID"), tableName);
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to query table '{}': {}", tableName, e.getMessage());
                throw new RuntimeException("Failed to get vertices by property", e);
            }
        }

        if (vertices.isEmpty()) {
            logger.warn("No vertices found for label='{}', {}='{}'. Tables searched: {}",
                        label, propertyKey, propertyValue, tablesToQuery);
        }

        return vertices.iterator();
    }

    /**
     * Convert a ResultSet row to a JdbcVertex, reading all columns as properties.
     */
    private JdbcVertex resultSetToVertex(ResultSet rs, String label) throws SQLException {
        Long id = rs.getLong("ID");
        Map<String, Object> properties = new HashMap<>();

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            // Skip the ID column - it's the internal identifier
            if ("ID".equals(columnName)) {
                continue;
            }

            Object value = rs.getObject(i);
            if (value != null) {
                properties.put(columnName, value);
            }
        }

        return new JdbcVertex(this, id, label, properties);
    }

    @Override
    public Iterator<Edge> edges(Object... edgeIds) {
        if (edgeIds.length == 0) {
            return getAllEdges();
        }

        List<Edge> edges = new ArrayList<>();
        for (Object id : edgeIds) {
            Long longId = toLongId(id);
            if (longId != null) {
                Edge e = getEdgeById(longId);
                if (e != null) {
                    edges.add(e);
                }
            }
        }
        return edges.iterator();
    }

    /**
     * Get all edges across all edge tables.
     */
    private Iterator<Edge> getAllEdges() {
        List<Edge> edges = new ArrayList<>();

        for (String tableName : existingEdgeTables) {
            String label = tableName.substring(2); // Remove "E_" prefix
            EdgeTableSchema schema = edgeTableSchemas.get(tableName);
            if (schema == null) {
                logger.warn("No schema found for edge table: {}", tableName);
                continue;
            }

            String outColumn = "public." + schema.outVertexLabel + "__O";
            String inColumn = "public." + schema.inVertexLabel + "__I";

            String sql = "SELECT \"ID\", \"" + outColumn + "\", \"" + inColumn + "\" FROM \"" + tableName + "\"";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    edges.add(resultSetToEdge(rs, label, schema, outColumn, inColumn));
                }
            } catch (SQLException e) {
                logger.error("Failed to get edges from table: " + tableName, e);
                throw new RuntimeException("Failed to get edges from table: " + tableName, e);
            }
        }

        return edges.iterator();
    }

    /**
     * Get an edge by ID, searching across all edge tables.
     */
    public Edge getEdgeById(Long id) {
        for (String tableName : existingEdgeTables) {
            String label = tableName.substring(2); // Remove "E_" prefix
            EdgeTableSchema schema = edgeTableSchemas.get(tableName);
            if (schema == null) continue;

            String outColumn = "public." + schema.outVertexLabel + "__O";
            String inColumn = "public." + schema.inVertexLabel + "__I";

            String sql = "SELECT \"ID\", \"" + outColumn + "\", \"" + inColumn + "\" FROM \"" + tableName + "\" WHERE \"ID\" = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return resultSetToEdge(rs, label, schema, outColumn, inColumn);
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to get edge by id: " + id + " from table: " + tableName, e);
                throw new RuntimeException("Failed to get edge by id", e);
            }
        }

        return null;
    }

    /**
     * Get an edge by ID with a label hint for optimization.
     */
    public Edge getEdgeById(Long id, String labelHint) {
        if (labelHint != null) {
            String tableName = getEdgeTableName(labelHint);
            if (existingEdgeTables.contains(tableName)) {
                EdgeTableSchema schema = edgeTableSchemas.get(tableName);
                if (schema != null) {
                    String outColumn = "public." + schema.outVertexLabel + "__O";
                    String inColumn = "public." + schema.inVertexLabel + "__I";

                    String sql = "SELECT \"ID\", \"" + outColumn + "\", \"" + inColumn + "\" FROM \"" + tableName + "\" WHERE \"ID\" = ?";

                    try (Connection conn = dataSource.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setLong(1, id);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                return resultSetToEdge(rs, labelHint, schema, outColumn, inColumn);
                            }
                        }
                    } catch (SQLException e) {
                        logger.error("Failed to get edge by id: " + id, e);
                        throw new RuntimeException("Failed to get edge by id", e);
                    }
                }
            }
        }

        // Fall back to searching all tables
        return getEdgeById(id);
    }

    /**
     * Convert a ResultSet row to a JdbcEdge.
     */
    private JdbcEdge resultSetToEdge(ResultSet rs, String label, EdgeTableSchema schema,
                                      String outColumn, String inColumn) throws SQLException {
        Long id = rs.getLong("ID");
        Long outVertexId = rs.getLong(outColumn);
        Long inVertexId = rs.getLong(inColumn);

        return new JdbcEdge(this, id, label, outVertexId, schema.outVertexLabel,
                           inVertexId, schema.inVertexLabel, new HashMap<>());
    }

    @Override
    public Transaction tx() {
        JdbcTransaction tx = threadLocalTransaction.get();
        if (tx == null) {
            tx = new JdbcTransaction(this);
            threadLocalTransaction.set(tx);
        }
        return tx;
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            closed = true;
            logger.info("JdbcGraph closed");
        }
    }

    @Override
    public Variables variables() {
        return new Variables() {
            @Override
            public Set<String> keys() {
                return variables.keySet();
            }

            @Override
            public <R> Optional<R> get(String key) {
                return Optional.ofNullable((R) variables.get(key));
            }

            @Override
            public void set(String key, Object value) {
                variables.put(key, value);
            }

            @Override
            public void remove(String key) {
                variables.remove(key);
            }
        };
    }

    @Override
    public Configuration configuration() {
        return new org.apache.commons.configuration.BaseConfiguration();
    }

    @Override
    public Features features() {
        return features;
    }

    // Helper methods for JSON conversion (used for complex types)
    public String propertiesToJson(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            sb.append(valueToJson(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private String valueToJson(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson(value.toString()) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Collection) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (Collection<?>) value) {
                if (!first) sb.append(",");
                first = false;
                sb.append(valueToJson(item));
            }
            sb.append("]");
            return sb.toString();
        } else if (value instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey().toString())).append("\":");
                sb.append(valueToJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    public Map<String, Object> jsonToProperties(String json) {
        Map<String, Object> properties = new HashMap<>();
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return properties;
        }

        // Simple JSON parsing for flat structures
        // Remove outer braces
        json = json.trim();
        if (json.startsWith("{")) {
            json = json.substring(1);
        }
        if (json.endsWith("}")) {
            json = json.substring(0, json.length() - 1);
        }

        if (json.isEmpty()) {
            return properties;
        }

        // Parse key-value pairs
        int pos = 0;
        while (pos < json.length()) {
            // Skip whitespace
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
            if (pos >= json.length()) break;

            // Parse key
            if (json.charAt(pos) != '"') break;
            int keyStart = pos + 1;
            int keyEnd = json.indexOf('"', keyStart);
            if (keyEnd < 0) break;
            String key = json.substring(keyStart, keyEnd);

            pos = keyEnd + 1;

            // Skip colon
            while (pos < json.length() && (Character.isWhitespace(json.charAt(pos)) || json.charAt(pos) == ':')) pos++;
            if (pos >= json.length()) break;

            // Parse value
            Object value = null;
            char c = json.charAt(pos);
            if (c == '"') {
                // String value
                int valueStart = pos + 1;
                int valueEnd = findClosingQuote(json, valueStart);
                if (valueEnd < 0) break;
                value = unescapeJson(json.substring(valueStart, valueEnd));
                pos = valueEnd + 1;
            } else if (c == '[') {
                // Array value - find matching bracket
                int depth = 1;
                int start = pos;
                pos++;
                while (pos < json.length() && depth > 0) {
                    if (json.charAt(pos) == '[') depth++;
                    else if (json.charAt(pos) == ']') depth--;
                    else if (json.charAt(pos) == '"') {
                        pos = findClosingQuote(json, pos + 1);
                    }
                    pos++;
                }
                value = json.substring(start, pos);
            } else if (c == '{') {
                // Object value - find matching brace
                int depth = 1;
                int start = pos;
                pos++;
                while (pos < json.length() && depth > 0) {
                    if (json.charAt(pos) == '{') depth++;
                    else if (json.charAt(pos) == '}') depth--;
                    else if (json.charAt(pos) == '"') {
                        pos = findClosingQuote(json, pos + 1);
                    }
                    pos++;
                }
                value = json.substring(start, pos);
            } else if (json.substring(pos).startsWith("true")) {
                value = true;
                pos += 4;
            } else if (json.substring(pos).startsWith("false")) {
                value = false;
                pos += 5;
            } else if (json.substring(pos).startsWith("null")) {
                value = null;
                pos += 4;
            } else {
                // Number
                int numStart = pos;
                while (pos < json.length() && (Character.isDigit(json.charAt(pos)) ||
                       json.charAt(pos) == '.' || json.charAt(pos) == '-' ||
                       json.charAt(pos) == 'e' || json.charAt(pos) == 'E' || json.charAt(pos) == '+')) {
                    pos++;
                }
                String numStr = json.substring(numStart, pos);
                if (numStr.contains(".") || numStr.toLowerCase().contains("e")) {
                    value = Double.parseDouble(numStr);
                } else {
                    try {
                        value = Integer.parseInt(numStr);
                    } catch (NumberFormatException e) {
                        value = Long.parseLong(numStr);
                    }
                }
            }

            properties.put(key, value);

            // Skip comma
            while (pos < json.length() && (Character.isWhitespace(json.charAt(pos)) || json.charAt(pos) == ',')) pos++;
        }

        return properties;
    }

    private int findClosingQuote(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }

    private String unescapeJson(String value) {
        if (value == null) return null;
        return value
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t");
    }

    // Connection management for transactions
    public Connection getConnection() throws SQLException {
        Connection conn = threadLocalConnection.get();
        if (conn != null && !conn.isClosed()) {
            return conn;
        }
        return dataSource.getConnection();
    }

    public void setThreadLocalConnection(Connection conn) {
        threadLocalConnection.set(conn);
    }

    public void clearThreadLocalConnection() {
        threadLocalConnection.remove();
        threadLocalTransaction.remove();
    }

    public boolean isInTransaction() {
        return threadLocalConnection.get() != null;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    // Methods for vertex/edge operations with label support
    public void updateVertexProperty(Long vertexId, String label, String key, Object value) {
        String tableName = getVertexTableName(label);

        // Ensure column exists
        ensureColumnExists(tableName, key, value);

        String sql = "UPDATE \"" + tableName + "\" SET \"" + key + "\" = ? WHERE \"ID\" = ?";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, convertValueForStorage(value));
                stmt.setLong(2, vertexId);
                stmt.executeUpdate();
            }

            if (!isInTransaction()) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to update vertex property", e);
            throw new RuntimeException("Failed to update vertex property", e);
        }
    }

    public void removeVertexProperty(Long vertexId, String label, String key) {
        String tableName = getVertexTableName(label);
        String sql = "UPDATE \"" + tableName + "\" SET \"" + key + "\" = NULL WHERE \"ID\" = ?";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, vertexId);
                stmt.executeUpdate();
            }

            if (!isInTransaction()) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to remove vertex property", e);
            throw new RuntimeException("Failed to remove vertex property", e);
        }
    }

    public void deleteVertex(Long vertexId, String label) {
        // First delete all edges connected to this vertex
        deleteEdgesForVertex(vertexId, label);

        // Then delete the vertex
        String tableName = getVertexTableName(label);
        String sql = "DELETE FROM \"" + tableName + "\" WHERE \"ID\" = ?";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, vertexId);
                stmt.executeUpdate();
            }

            if (!isInTransaction()) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to delete vertex", e);
            throw new RuntimeException("Failed to delete vertex", e);
        }
    }

    /**
     * Delete all edges connected to a vertex.
     */
    private void deleteEdgesForVertex(Long vertexId, String vertexLabel) {
        for (String tableName : existingEdgeTables) {
            EdgeTableSchema schema = edgeTableSchemas.get(tableName);
            if (schema == null) continue;

            String outColumn = "public." + schema.outVertexLabel + "__O";
            String inColumn = "public." + schema.inVertexLabel + "__I";

            // Build delete conditions based on whether this vertex could be connected
            List<String> conditions = new ArrayList<>();
            if (schema.outVertexLabel.equals(vertexLabel)) {
                conditions.add("\"" + outColumn + "\" = ?");
            }
            if (schema.inVertexLabel.equals(vertexLabel)) {
                conditions.add("\"" + inColumn + "\" = ?");
            }

            if (conditions.isEmpty()) continue;

            String sql = "DELETE FROM \"" + tableName + "\" WHERE " + String.join(" OR ", conditions);

            try {
                Connection conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (int i = 0; i < conditions.size(); i++) {
                        stmt.setLong(i + 1, vertexId);
                    }
                    stmt.executeUpdate();
                }

                if (!isInTransaction()) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error("Failed to delete edges for vertex: " + vertexId + " from table: " + tableName, e);
                throw new RuntimeException("Failed to delete edges for vertex", e);
            }
        }
    }

    /**
     * Add an edge with SQLG-style schema.
     */
    public Edge addEdge(Long outVertexId, String outLabel, Long inVertexId, String inLabel,
                        String edgeLabel, Map<String, Object> properties) {

        logger.debug("Adding edge: {} -> [{}] -> {}", outVertexId, edgeLabel, inVertexId);

        ensureEdgeTableExists(edgeLabel, outLabel, inLabel);

        String tableName = getEdgeTableName(edgeLabel);
        String outColumn = "public." + outLabel + "__O";
        String inColumn = "public." + inLabel + "__I";

        String sql = "INSERT INTO \"" + tableName + "\" (\"" + outColumn + "\", \"" + inColumn + "\") " +
                     "VALUES (?, ?) RETURNING \"ID\"";

        try {
            Connection conn = getConnection();
            Long edgeId;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, outVertexId);
                stmt.setLong(2, inVertexId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        edgeId = rs.getLong("ID");
                    } else {
                        throw new SQLException("Failed to get generated edge ID");
                    }
                }
            }

            if (!isInTransaction()) {
                conn.close();
            }

            logger.debug("Created edge with ID={}: {} [{}] -> {}", edgeId, outVertexId, edgeLabel, inVertexId);
            return new JdbcEdge(this, edgeId, edgeLabel, outVertexId, outLabel, inVertexId, inLabel, properties);
        } catch (SQLException e) {
            logger.error("Failed to add edge", e);
            throw new RuntimeException("Failed to add edge", e);
        }
    }

    /**
     * Add an edge from JdbcVertex objects (for TinkerPop compatibility).
     */
    public Edge addEdge(String id, String label, JdbcVertex outVertex, JdbcVertex inVertex, Map<String, Object> properties) {
        // id parameter is ignored - we use auto-generated BIGSERIAL IDs
        return addEdge((Long) outVertex.id(), outVertex.label(),
                      (Long) inVertex.id(), inVertex.label(),
                      label, properties);
    }

    public void deleteEdge(Long edgeId, String label) {
        String tableName = getEdgeTableName(label);
        String sql = "DELETE FROM \"" + tableName + "\" WHERE \"ID\" = ?";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, edgeId);
                stmt.executeUpdate();
            }

            if (!isInTransaction()) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to delete edge", e);
            throw new RuntimeException("Failed to delete edge", e);
        }
    }

    public void updateEdgeProperty(Long edgeId, String label, String key, Object value) {
        String tableName = getEdgeTableName(label);

        // For edge properties, we need to add columns dynamically too
        Set<String> columns = tableColumns.computeIfAbsent(tableName, k -> ConcurrentHashMap.newKeySet());
        if (!columns.contains(key)) {
            String sqlType = getSqlType(value);
            String alterSql = "ALTER TABLE \"" + tableName + "\" ADD COLUMN IF NOT EXISTS \"" + key + "\" " + sqlType;
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                columns.add(key);
            } catch (SQLException e) {
                if (!e.getMessage().contains("already exists")) {
                    throw new RuntimeException("Failed to add column to edge table", e);
                }
                columns.add(key);
            }
        }

        String sql = "UPDATE \"" + tableName + "\" SET \"" + key + "\" = ? WHERE \"ID\" = ?";

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, convertValueForStorage(value));
                stmt.setLong(2, edgeId);
                stmt.executeUpdate();
            }

            if (!isInTransaction()) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to update edge property", e);
            throw new RuntimeException("Failed to update edge property", e);
        }
    }

    public Iterator<Edge> getVertexEdges(Long vertexId, String vertexLabel, Direction direction, String... edgeLabels) {
        List<Edge> edges = new ArrayList<>();

        // Determine which edge tables to query
        Set<String> tablesToQuery = new HashSet<>();
        if (edgeLabels != null && edgeLabels.length > 0) {
            for (String edgeLabel : edgeLabels) {
                String tableName = getEdgeTableName(edgeLabel);
                if (existingEdgeTables.contains(tableName)) {
                    tablesToQuery.add(tableName);
                }
            }
        } else {
            tablesToQuery.addAll(existingEdgeTables);
        }

        for (String tableName : tablesToQuery) {
            String label = tableName.substring(2); // Remove "E_" prefix
            EdgeTableSchema schema = edgeTableSchemas.get(tableName);
            if (schema == null) continue;

            String outColumn = "public." + schema.outVertexLabel + "__O";
            String inColumn = "public." + schema.inVertexLabel + "__I";

            // Determine if this vertex can be connected via this edge table
            boolean canBeOut = schema.outVertexLabel.equals(vertexLabel);
            boolean canBeIn = schema.inVertexLabel.equals(vertexLabel);

            if (!canBeOut && !canBeIn) continue;

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT \"ID\", \"").append(outColumn).append("\", \"").append(inColumn)
               .append("\" FROM \"").append(tableName).append("\" WHERE ");

            List<String> conditions = new ArrayList<>();
            if ((direction == Direction.OUT || direction == Direction.BOTH) && canBeOut) {
                conditions.add("\"" + outColumn + "\" = ?");
            }
            if ((direction == Direction.IN || direction == Direction.BOTH) && canBeIn) {
                conditions.add("\"" + inColumn + "\" = ?");
            }

            if (conditions.isEmpty()) continue;

            sql.append(String.join(" OR ", conditions));

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

                for (int i = 0; i < conditions.size(); i++) {
                    stmt.setLong(i + 1, vertexId);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        edges.add(resultSetToEdge(rs, label, schema, outColumn, inColumn));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to get vertex edges from table: " + tableName, e);
                throw new RuntimeException("Failed to get vertex edges", e);
            }
        }

        return edges.iterator();
    }

    public Iterator<Vertex> getAdjacentVertices(Long vertexId, String vertexLabel, Direction direction, String... edgeLabels) {
        Set<Long> vertexIds = new HashSet<>();
        Map<Long, String> vertexLabels = new HashMap<>(); // vertexId -> label

        // Determine which edge tables to query
        Set<String> tablesToQuery = new HashSet<>();
        if (edgeLabels != null && edgeLabels.length > 0) {
            for (String edgeLabel : edgeLabels) {
                String tableName = getEdgeTableName(edgeLabel);
                if (existingEdgeTables.contains(tableName)) {
                    tablesToQuery.add(tableName);
                }
            }
        } else {
            tablesToQuery.addAll(existingEdgeTables);
        }

        for (String tableName : tablesToQuery) {
            EdgeTableSchema schema = edgeTableSchemas.get(tableName);
            if (schema == null) continue;

            String outColumn = "public." + schema.outVertexLabel + "__O";
            String inColumn = "public." + schema.inVertexLabel + "__I";

            // Determine if this vertex can be connected via this edge table
            boolean canBeOut = schema.outVertexLabel.equals(vertexLabel);
            boolean canBeIn = schema.inVertexLabel.equals(vertexLabel);

            if (!canBeOut && !canBeIn) continue;

            // For OUT direction: vertex is the out vertex, we want the in vertex
            // For IN direction: vertex is the in vertex, we want the out vertex
            if (direction == Direction.OUT && canBeOut) {
                String sql = "SELECT DISTINCT \"" + inColumn + "\" FROM \"" + tableName + "\" WHERE \"" + outColumn + "\" = ?";
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, vertexId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Long vId = rs.getLong(1);
                            vertexIds.add(vId);
                            vertexLabels.put(vId, schema.inVertexLabel);
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Failed to get adjacent vertices from table: " + tableName, e);
                    throw new RuntimeException("Failed to get adjacent vertices", e);
                }
            }

            if (direction == Direction.IN && canBeIn) {
                String sql = "SELECT DISTINCT \"" + outColumn + "\" FROM \"" + tableName + "\" WHERE \"" + inColumn + "\" = ?";
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, vertexId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Long vId = rs.getLong(1);
                            vertexIds.add(vId);
                            vertexLabels.put(vId, schema.outVertexLabel);
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Failed to get adjacent vertices from table: " + tableName, e);
                    throw new RuntimeException("Failed to get adjacent vertices", e);
                }
            }

            if (direction == Direction.BOTH) {
                if (canBeOut) {
                    String sql = "SELECT DISTINCT \"" + inColumn + "\" FROM \"" + tableName + "\" WHERE \"" + outColumn + "\" = ?";
                    try (Connection conn = dataSource.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setLong(1, vertexId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                Long vId = rs.getLong(1);
                                vertexIds.add(vId);
                                vertexLabels.put(vId, schema.inVertexLabel);
                            }
                        }
                    } catch (SQLException e) {
                        logger.error("Failed to get adjacent vertices from table: " + tableName, e);
                        throw new RuntimeException("Failed to get adjacent vertices", e);
                    }
                }
                if (canBeIn) {
                    String sql = "SELECT DISTINCT \"" + outColumn + "\" FROM \"" + tableName + "\" WHERE \"" + inColumn + "\" = ?";
                    try (Connection conn = dataSource.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setLong(1, vertexId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                Long vId = rs.getLong(1);
                                vertexIds.add(vId);
                                vertexLabels.put(vId, schema.outVertexLabel);
                            }
                        }
                    } catch (SQLException e) {
                        logger.error("Failed to get adjacent vertices from table: " + tableName, e);
                        throw new RuntimeException("Failed to get adjacent vertices", e);
                    }
                }
            }
        }

        // Fetch actual vertices with their labels as hints
        List<Vertex> vertices = new ArrayList<>();
        for (Long vId : vertexIds) {
            String labelHint = vertexLabels.get(vId);
            Vertex v = getVertexById(vId, labelHint);
            if (v != null) {
                vertices.add(v);
            }
        }

        return vertices.iterator();
    }

    // Index management for per-entity tables
    public void createIndex(String label, String propertyName) {
        String tableName = getVertexTableName(label);

        // Ensure table exists first
        ensureVertexTableExists(label);

        String indexName = "idx_" + tableName.toLowerCase() + "_" + propertyName.toLowerCase().replace("@", "_");
        String sql = "CREATE INDEX IF NOT EXISTS \"" + indexName + "\" ON \"" + tableName + "\" (\"" + propertyName + "\")";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Created index: {}", indexName);
        } catch (SQLException e) {
            logger.error("Failed to create index", e);
            throw new RuntimeException("Failed to create index", e);
        }
    }

    public void createUniqueIndex(String label, String propertyName) {
        String tableName = getVertexTableName(label);

        // Ensure table exists first
        ensureVertexTableExists(label);

        String indexName = "idx_unique_" + tableName.toLowerCase() + "_" + propertyName.toLowerCase().replace("@", "_");
        String sql = "CREATE UNIQUE INDEX IF NOT EXISTS \"" + indexName + "\" ON \"" + tableName + "\" (\"" + propertyName + "\")";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Created unique index: {}", indexName);
        } catch (SQLException e) {
            logger.error("Failed to create unique index", e);
            throw new RuntimeException("Failed to create unique index", e);
        }
    }

    public void createCompositeIndex(String label, List<String> propertyNames) {
        String tableName = getVertexTableName(label);

        // Ensure table exists first
        ensureVertexTableExists(label);

        String indexName = "idx_comp_" + tableName.toLowerCase() + "_" +
                          String.join("_", propertyNames).toLowerCase().replace("@", "_");
        StringBuilder sql = new StringBuilder("CREATE INDEX IF NOT EXISTS \"" + indexName + "\" ON \"" + tableName + "\" (");
        for (int i = 0; i < propertyNames.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("\"").append(propertyNames.get(i)).append("\"");
        }
        sql.append(")");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
            logger.info("Created composite index: {}", indexName);
        } catch (SQLException e) {
            logger.error("Failed to create composite index", e);
            throw new RuntimeException("Failed to create composite index", e);
        }
    }

    /**
     * Get all existing vertex table names.
     */
    public Set<String> getExistingVertexTables() {
        return Collections.unmodifiableSet(existingVertexTables);
    }

    /**
     * Get all existing edge table names.
     */
    public Set<String> getExistingEdgeTables() {
        return Collections.unmodifiableSet(existingEdgeTables);
    }
}
