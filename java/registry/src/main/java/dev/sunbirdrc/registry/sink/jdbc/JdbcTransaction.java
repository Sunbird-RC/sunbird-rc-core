package dev.sunbirdrc.registry.sink.jdbc;

import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.util.AbstractTransaction;
import org.apache.tinkerpop.gremlin.structure.util.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * Transaction management for JDBC-backed graph.
 * Uses READ COMMITTED isolation level for YugabyteDB compatibility.
 */
public class JdbcTransaction extends AbstractTransaction {
    private static final Logger logger = LoggerFactory.getLogger(JdbcTransaction.class);

    private final JdbcGraph graph;
    private Connection connection;
    private boolean open = false;

    public JdbcTransaction(JdbcGraph graph) {
        super(graph);
        this.graph = graph;
    }

    @Override
    protected void doOpen() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = graph.getDataSource().getConnection();
                // Use READ COMMITTED for YugabyteDB compatibility
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                connection.setAutoCommit(false);
                graph.setThreadLocalConnection(connection);
                open = true;
                logger.debug("Transaction opened");
            }
        } catch (SQLException e) {
            logger.error("Failed to open transaction", e);
            throw new RuntimeException("Failed to open transaction", e);
        }
    }

    @Override
    protected void doCommit() throws TransactionException {
        if (connection != null) {
            try {
                connection.commit();
                logger.debug("Transaction committed");
            } catch (SQLException e) {
                logger.error("Failed to commit transaction", e);
                throw new TransactionException("Failed to commit transaction", e);
            } finally {
                closeConnection();
            }
        }
    }

    @Override
    protected void doRollback() throws TransactionException {
        if (connection != null) {
            try {
                connection.rollback();
                logger.debug("Transaction rolled back");
            } catch (SQLException e) {
                logger.error("Failed to rollback transaction", e);
                throw new TransactionException("Failed to rollback transaction", e);
            } finally {
                closeConnection();
            }
        }
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("Failed to close connection", e);
            } finally {
                connection = null;
                graph.clearThreadLocalConnection();
                open = false;
            }
        }
    }

    @Override
    public boolean isOpen() {
        return open && connection != null;
    }

    @Override
    protected void doReadWrite() {
        // No-op for JDBC - always read-write
    }

    @Override
    protected void doClose() {
        if (isOpen()) {
            try {
                doCommit();
            } catch (TransactionException e) {
                logger.error("Failed to commit on close, rolling back", e);
                try {
                    doRollback();
                } catch (TransactionException re) {
                    logger.error("Failed to rollback after failed commit", re);
                }
            }
        }
        closeConnection();
    }

    @Override
    protected void fireOnCommit() {
        // No-op
    }

    @Override
    protected void fireOnRollback() {
        // No-op
    }

    @Override
    public void addTransactionListener(Consumer<Status> listener) {
        // No-op - transaction listeners not supported
    }

    @Override
    public void removeTransactionListener(Consumer<Status> listener) {
        // No-op
    }

    @Override
    public void clearTransactionListeners() {
        // No-op
    }

    @Override
    public Transaction onClose(Consumer<Transaction> consumer) {
        // Store the close consumer if needed
        return this;
    }

    @Override
    public Transaction onReadWrite(Consumer<Transaction> consumer) {
        // Store the read-write consumer if needed
        return this;
    }
}
