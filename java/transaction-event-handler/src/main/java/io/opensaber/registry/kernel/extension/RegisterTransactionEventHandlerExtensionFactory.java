package io.opensaber.registry.kernel.extension;

import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.graphdb.GraphDatabaseService;


public class RegisterTransactionEventHandlerExtensionFactory extends KernelExtensionFactory<RegisterTransactionEventHandlerExtensionFactory.Dependencies> {

    public interface Dependencies {
    	GraphDatabaseService getGraphDatabaseService();
    }

    public RegisterTransactionEventHandlerExtensionFactory() {
        super("registerTransactionEventHandler");
    }

    @SuppressWarnings("unchecked")
	@Override
	public Lifecycle newInstance(KernelContext context, final Dependencies dependencies) throws Throwable {
		return new LifecycleAdapter() {

            private Neo4jTransactionEventHandler handler;

			@Override
            public void start() throws Throwable {
                try {
                    handler = new Neo4jTransactionEventHandler(dependencies.getGraphDatabaseService());
                    dependencies.getGraphDatabaseService().registerTransactionEventHandler(handler);
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }

            @Override
            public void shutdown() throws Throwable {
                try {
                    dependencies.getGraphDatabaseService().unregisterTransactionEventHandler(handler);
                } catch (Exception e) {
                }
            }
        };
	}

}
