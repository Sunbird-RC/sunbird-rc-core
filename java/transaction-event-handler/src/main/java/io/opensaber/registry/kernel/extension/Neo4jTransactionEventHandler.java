package io.opensaber.registry.kernel.extension;

import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.GraphDatabaseService;


@SuppressWarnings("rawtypes")
public class Neo4jTransactionEventHandler implements TransactionEventHandler {

	

	public static GraphDatabaseService db;

	public Neo4jTransactionEventHandler(GraphDatabaseService graphDatabaseService) {
		db = graphDatabaseService;
	}

	@Override
	public Void beforeCommit(TransactionData transactionData) throws Exception {
		try {
			//TelemetryManager.log("Checking if the Current Instance is Master: " + db.isMaster());
			//if (db.isMaster()) {
				//TelemetryManager.log("Processing the Transaction as I am the Master: " + db.role());
				ProcessTransactionData processTransactionData = new ProcessTransactionData(
						"registry", db);
				processTransactionData.processTxnData(transactionData);
			//}
		} catch (Exception e) {
			throw e;
		}
		return null;
	}

	@Override
	public void afterCommit(TransactionData transactionData, Object o) {
		//TelemetryManager.log("After Commit Executed.");
	}

	@Override
	public void afterRollback(TransactionData transactionData, Object o) {
		//TelemetryManager.log("After Rollback Executed.");
	}
}
