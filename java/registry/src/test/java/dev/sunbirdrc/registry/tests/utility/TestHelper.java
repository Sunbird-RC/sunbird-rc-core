package dev.sunbirdrc.registry.tests.utility;

import dev.sunbirdrc.registry.sink.DatabaseProvider;

public class TestHelper {

	public static void clearData(DatabaseProvider databaseProvider) {
		databaseProvider.getGraphStore().traversal().V().drop().iterate();
	}

}
