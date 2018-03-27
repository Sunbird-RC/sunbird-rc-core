package io.opensaber.registry.sink;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

public abstract class DatabaseProvider {

    public abstract Graph getGraphStore();
    public abstract  void shutdown() throws Exception;

    /**
     * This method is used for checking database service. It fires a dummy query to check for a non-existent label
     * and checks for the count of the vertices
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

}

