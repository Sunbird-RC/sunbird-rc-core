package io.opensaber.registry.sink;

import org.apache.tinkerpop.gremlin.structure.Graph;

public interface DatabaseProvider {

    public Graph getGraphStore();
    public void shutdown() throws Exception;

}

