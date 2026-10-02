package dev.sunbirdrc.registry.sink.jdbc;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * V().hasLabel(...) on JdbcGraph must read the label's own table, not every table:
 * the unfiltered read loaded the whole registry into memory at startup, on each
 * /health call and on each native search, and ran a 3 GB heap out of memory once
 * the registry held a few hundred thousand certificates.
 */
public class JdbcGraphStepStrategyTest {

    /** Records which read JdbcGraphStep chose, and serves canned vertices instead of SQL. */
    static class RecordingGraph extends JdbcGraph {
        final List<String> reads = new ArrayList<>();
        final Map<String, List<Vertex>> tables = new HashMap<>();
        boolean failPropertyQuery;

        RecordingGraph() {
            super(emptyDatabase());
        }

        void put(String label, String osid, String status) {
            Map<String, Object> props = new HashMap<>();
            props.put("osid", osid);
            if (status != null) {
                props.put("status", status);
            }
            tables.computeIfAbsent(label, l -> new ArrayList<>())
                    .add(new JdbcVertex(this, (long) (osid.hashCode()), label, props));
        }

        @Override
        public Iterator<Vertex> vertices(Object... ids) {
            reads.add(ids.length == 0 ? "ALL" : "ids");
            List<Vertex> all = new ArrayList<>();
            tables.values().forEach(all::addAll);
            return all.iterator();
        }

        @Override
        public Iterator<Vertex> getVerticesByLabel(String label) {
            reads.add("label:" + label);
            return tables.getOrDefault(label, Collections.emptyList()).iterator();
        }

        @Override
        public Iterator<Vertex> getVerticesByProperty(String label, String key, Object value) {
            reads.add("property:" + label + ":" + key + "=" + value);
            if (failPropertyQuery) {
                throw new RuntimeException("column type mismatch");
            }
            List<Vertex> found = new ArrayList<>();
            for (Map.Entry<String, List<Vertex>> table : tables.entrySet()) {
                if (label != null && !label.equals(table.getKey())) {
                    continue;
                }
                for (Vertex v : table.getValue()) {
                    if (v.property(key).isPresent() && value.equals(v.value(key))) {
                        found.add(v);
                    }
                }
            }
            return found.iterator();
        }
    }

    /** A DataSource whose database has no tables, enough for JdbcGraph's constructor. */
    static DataSource emptyDatabase() {
        return (DataSource) stub(DataSource.class);
    }

    private static Object stub(Class<?> type) {
        return Proxy.newProxyInstance(JdbcGraphStepStrategyTest.class.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) {
                        return false;
                    }
                    if (returns == int.class || returns == long.class) {
                        return returns == int.class ? (Object) 0 : (Object) 0L;
                    }
                    if (returns.isInterface() && returns.getName().startsWith("java.sql")) {
                        return stub(returns);
                    }
                    return null;
                });
    }

    private RecordingGraph graph;

    @Before
    public void setUp() {
        graph = new RecordingGraph();
        graph.put("TrainingCertificate", "1-a", "ACTIVE");
        graph.put("TrainingCertificate", "1-b", null);
        graph.put("TrainingCertificate_GROUP", "1-parent", null);
        graph.put("PublicKey", "1-key", null);
    }

    @Test
    public void hasLabelReadsOnlyThatLabelsTable() {
        List<Vertex> found = IteratorUtils.list(graph.traversal().V().hasLabel("TrainingCertificate_GROUP"));
        assertEquals(1, found.size());
        assertEquals(Collections.singletonList("label:TrainingCertificate_GROUP"), graph.reads);
    }

    @Test
    public void hasTLabelUsedByHealthCheckDoesNotLoadEveryTable() {
        long count = IteratorUtils.count(graph.traversal().V().has(T.label, "HealthCheckLabel"));
        assertEquals(0, count);
        assertEquals(Collections.singletonList("label:HealthCheckLabel"), graph.reads);
    }

    @Test
    public void labelPlusEqualityBecomesOnePropertyQuery() {
        List<Vertex> found = IteratorUtils.list(graph.traversal().V().hasLabel("TrainingCertificate").has("osid", "1-b"));
        assertEquals(1, found.size());
        assertEquals("1-b", found.get(0).value("osid"));
        assertEquals(Collections.singletonList("property:TrainingCertificate:osid=1-b"), graph.reads);
    }

    @Test
    public void severalLabelsReadEachTableOnce() {
        long count = IteratorUtils.count(graph.traversal().V().hasLabel("PublicKey", "TrainingCertificate_GROUP"));
        assertEquals(2, count);
        assertEquals(Arrays.asList("label:PublicKey", "label:TrainingCertificate_GROUP"), graph.reads);
    }

    @Test
    public void conditionsThatAreNotPushedDownStillFilterTheResult() {
        List<Vertex> found = IteratorUtils.list(graph.traversal().V().hasLabel("TrainingCertificate")
                .has("status", P.neq("REVOKED")).has("status", "ACTIVE"));
        assertEquals(1, found.size());
        assertEquals("1-a", found.get(0).value("osid"));
    }

    @Test
    public void aFailingPropertyQueryFallsBackToTheLabelTable() {
        graph.failPropertyQuery = true;
        List<Vertex> found = IteratorUtils.list(graph.traversal().V().hasLabel("TrainingCertificate").has("osid", "1-a"));
        assertEquals(1, found.size());
        assertEquals(Arrays.asList("property:TrainingCertificate:osid=1-a", "label:TrainingCertificate"), graph.reads);
    }

    @Test
    public void noLabelButAnEqualitySearchesByThePropertyNotEveryRow() {
        List<Vertex> found = IteratorUtils.list(graph.traversal().V().has("osid", "1-key"));
        assertEquals(1, found.size());
        assertEquals(Collections.singletonList("property:null:osid=1-key"), graph.reads);
    }

    @Test
    public void anUnfilteredTraversalStillReadsEverything() {
        assertEquals(4, IteratorUtils.count(graph.traversal().V()));
        assertTrue(graph.reads.contains("ALL"));
    }
}
