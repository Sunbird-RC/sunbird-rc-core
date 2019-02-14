package io.opensaber.registry.sink;

import org.apache.tinkerpop.gremlin.structure.Graph;

public class OSGraph implements AutoCloseable {
    private Graph graph;
    private boolean closeRequired;
    private boolean supportsTransaction = false;

    protected OSGraph() {
        supportsTransaction = graph.features().graph().supportsTransactions();
    }

    public OSGraph (Graph g, boolean close) {
        graph = g;
        closeRequired = close;
    }

    public void close() throws Exception {
        if (graph != null && supportsTransaction && graph.tx().isOpen()) {
            graph.tx().close();
        }

        if (closeRequired) {
            graph.close();
        } else {
            // no action required.
        }
    }

    public Graph getGraphStore() {
        return this.graph;
    }
}
