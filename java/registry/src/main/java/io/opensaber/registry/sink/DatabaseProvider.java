package io.opensaber.registry.sink;

import io.opensaber.registry.middleware.util.Constants;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DatabaseProvider {

	private static Logger logger = LoggerFactory.getLogger(DatabaseProvider.class);

	public abstract Graph getGraphStore();

	public abstract <T> T getRawGraph();

	public abstract void shutdown() throws Exception;

	/**
	 * This method is used for checking database service. It fires a dummy query to
	 * check for a non-existent label and checks for the count of the vertices
	 * 
	 * @return
	 */
	public boolean isDatabaseServiceUp() {
		boolean databaseStautsUp = false;
		try {
			long count = IteratorUtils.count(getGraphStore().traversal().clone().V().has(T.label, "HealthCheckLabel"));
			if (count >= 0) {
				databaseStautsUp = true;
			}
		} catch (Exception ex) {
			databaseStautsUp = false;
		}
		return databaseStautsUp;
	}

	/**
	 * This method is used to initialize some global graph level configuration
	 */
	public void initializeGlobalGraphConfiguration() {
		Graph graph = getGraphStore();
		if (IteratorUtils.count(graph.traversal().V().has(T.label, Constants.GRAPH_GLOBAL_CONFIG)) == 0) {
			logger.info("Adding GRAPH_GLOBAL_CONFIG node...");
			if (graph.features().graph().supportsTransactions()) {
				org.apache.tinkerpop.gremlin.structure.Transaction tx;
				tx = graph.tx();
				tx.onReadWrite(org.apache.tinkerpop.gremlin.structure.Transaction.READ_WRITE_BEHAVIOR.AUTO);
				Vertex globalConfig = graph.traversal().clone().addV(Constants.GRAPH_GLOBAL_CONFIG).next();
				globalConfig.property(Constants.PERSISTENT_GRAPH, true);
				tx.commit();
				logger.info("Graph initialised using transaction !");
			} else {
				Vertex globalConfig = graph.traversal().clone().addV(Constants.GRAPH_GLOBAL_CONFIG).next();
				globalConfig.property(Constants.PERSISTENT_GRAPH, true);
				logger.info("Graph initialised without transaction !");
			}
		}
		try {
			graph.close();
		} catch (Exception e) {
			// This function is called at boot time and any exception thrown here indicates very serious
			// problem.
			logger.error("Can't close graph " + e.getMessage());
		}
	}

	private boolean supportsTransaction(Graph graph) {
		return graph.features().graph().supportsTransactions();
	}

	public Transaction startTransaction(Graph graph) {
		Transaction tx = null;
		if (supportsTransaction(graph)) {
			tx = graph.tx();
		}
		return tx;
	}

	public void commitTransaction(Graph graph, Transaction tx) {
		if (null != tx && supportsTransaction(graph)) {
			tx.commit();
		}
	}
}
