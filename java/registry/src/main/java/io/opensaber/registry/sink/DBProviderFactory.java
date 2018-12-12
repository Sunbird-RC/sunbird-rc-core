package io.opensaber.registry.sink;

import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component("dbProviderFactory")
public class DBProviderFactory {
	
	@Autowired
	Environment environment;

	@Autowired
	DBConnectionInfoMgr dbConnectionInfoMgr;
	
	public DatabaseProvider getInstance(DBConnectionInfo connectionInfo) {
		String dbProvider = environment.getProperty(Constants.DATABASE_PROVIDER);
		DatabaseProvider provider;
		if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.ORIENTDB.getName())) {
			provider = new OrientDBGraphProvider(environment);
			provider.initializeGlobalGraphConfiguration();
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.NEO4J.getName())) {
			provider = new Neo4jGraphProvider(connectionInfo);
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.SQLG.getName())) {
			provider = new SqlgProvider(environment);
			provider.initializeGlobalGraphConfiguration();
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.TINKERGRAPH.getName())) {
			provider = new TinkerGraphProvider(environment);
			provider.initializeGlobalGraphConfiguration();
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.CASSANDRA.getName())) {
			provider = new JanusGraphStorage(environment);
			provider.initializeGlobalGraphConfiguration();
		} else {
			throw new RuntimeException("No Database Provider is configured. Please configure a Database Provider");
		}
		return provider;

	}

}
