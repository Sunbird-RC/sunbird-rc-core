package io.opensaber.registry.tests.utility;

import io.opensaber.registry.sink.DatabaseProvider;

public class TestHelper {

	public static void clearData(DatabaseProvider databaseProvider) {
		databaseProvider.getGraphStore().traversal().V().drop().iterate();
	}

}
