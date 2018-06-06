package io.opensaber.registry.kernel.extension;

import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neo4j.graphdb.GraphDatabaseService;


@SuppressWarnings("rawtypes")
public class Neo4jTransactionEventHandler implements TransactionEventHandler {

	private static Logger logger = LoggerFactory.getLogger(Neo4jTransactionEventHandler.class);

	public static GraphDatabaseService db;

	public Neo4jTransactionEventHandler(GraphDatabaseService graphDatabaseService) {
		db = graphDatabaseService;
	}

	@Override
	public Void beforeCommit(TransactionData transactionData) throws Exception {
		try {
				ProcessTransactionData processTransactionData = new ProcessTransactionData(
						"registry", db);
				processTransactionData.processTxnData(transactionData);
		} catch (Exception e) {
			throw e;
		}
		return null;
	}

	@Override
	public void afterCommit(TransactionData transactionData, Object o) {
		logger.debug("After Commit Executed.");
	}

	@Override
	public void afterRollback(TransactionData transactionData, Object o) {
		logger.debug("After Rollback Executed.");
	}
}
