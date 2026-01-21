package dev.sunbirdrc.registry.sink.jdbc;

import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;

import java.util.*;

/**
 * Edge implementation backed by database rows using JDBC.
 * Works with per-entity tables (E_{label} naming convention).
 * Uses Long IDs (BIGSERIAL) and SQLG-style vertex references.
 */
public class JdbcEdge implements Edge {

    private final JdbcGraph graph;
    private final Long id;
    private final String label;
    private final Long outVertexId;
    private final String outVertexLabel;
    private final Long inVertexId;
    private final String inVertexLabel;
    private Map<String, Object> properties;
    private boolean removed = false;

    public JdbcEdge(JdbcGraph graph, Long id, String label,
                    Long outVertexId, String outVertexLabel,
                    Long inVertexId, String inVertexLabel,
                    Map<String, Object> properties) {
        this.graph = graph;
        this.id = id;
        this.label = label;
        this.outVertexId = outVertexId;
        this.outVertexLabel = outVertexLabel;
        this.inVertexId = inVertexId;
        this.inVertexLabel = inVertexLabel;
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
    }

    @Override
    public Iterator<Vertex> vertices(Direction direction) {
        if (removed) {
            return Collections.emptyIterator();
        }

        List<Vertex> vertices = new ArrayList<>();
        switch (direction) {
            case OUT:
                vertices.add(getOutVertex());
                break;
            case IN:
                vertices.add(getInVertex());
                break;
            case BOTH:
                vertices.add(getOutVertex());
                vertices.add(getInVertex());
                break;
        }
        return vertices.iterator();
    }

    @Override
    public Vertex outVertex() {
        return getOutVertex();
    }

    @Override
    public Vertex inVertex() {
        return getInVertex();
    }

    /**
     * Get the out vertex, lazily loading if necessary.
     */
    private Vertex getOutVertex() {
        return graph.getVertexById(outVertexId, outVertexLabel);
    }

    /**
     * Get the in vertex, lazily loading if necessary.
     */
    private Vertex getInVertex() {
        return graph.getVertexById(inVertexId, inVertexLabel);
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public Graph graph() {
        return graph;
    }

    @Override
    public void remove() {
        if (!removed) {
            // Pass label for per-entity table deletion
            graph.deleteEdge(id, label);
            removed = true;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> Iterator<Property<V>> properties(String... propertyKeys) {
        if (removed) {
            return Collections.emptyIterator();
        }

        List<Property<V>> result = new ArrayList<>();

        if (propertyKeys == null || propertyKeys.length == 0) {
            // Return all properties
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                result.add(new JdbcProperty<>(this, entry.getKey(), (V) entry.getValue()));
            }
        } else {
            // Return specific properties
            for (String key : propertyKeys) {
                Object value = properties.get(key);
                if (value != null) {
                    result.add(new JdbcProperty<>(this, key, (V) value));
                }
            }
        }

        return result.iterator();
    }

    @Override
    public <V> Property<V> property(String key, V value) {
        if (removed) {
            throw new IllegalStateException("Edge has been removed");
        }

        ElementHelper.validateProperty(key, value);

        properties.put(key, value);
        // Pass label for per-entity table update
        graph.updateEdgeProperty(id, label, key, value);

        return new JdbcProperty<>(this, key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> Property<V> property(String key) {
        if (removed) {
            return Property.empty();
        }

        Object value = properties.get(key);
        if (value != null) {
            return new JdbcProperty<>(this, key, (V) value);
        }
        return Property.empty();
    }

    public Map<String, Object> getPropertiesMap() {
        return properties;
    }

    /**
     * Get the out vertex ID.
     */
    public Long getOutVertexId() {
        return outVertexId;
    }

    /**
     * Get the out vertex label.
     */
    public String getOutVertexLabel() {
        return outVertexLabel;
    }

    /**
     * Get the in vertex ID.
     */
    public Long getInVertexId() {
        return inVertexId;
    }

    /**
     * Get the in vertex label.
     */
    public String getInVertexLabel() {
        return inVertexLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        Edge that = (Edge) o;
        return Objects.equals(id, that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "e[" + id + "][" + outVertexId + "-" + label + "->" + inVertexId + "]";
    }

    /**
     * Simple Property implementation for Edge properties.
     */
    private static class JdbcProperty<V> implements Property<V> {
        private final JdbcEdge edge;
        private final String key;
        private final V value;

        JdbcProperty(JdbcEdge edge, String key, V value) {
            this.edge = edge;
            this.key = key;
            this.value = value;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public V value() throws NoSuchElementException {
            return value;
        }

        @Override
        public boolean isPresent() {
            return value != null;
        }

        @Override
        public Element element() {
            return edge;
        }

        @Override
        public void remove() {
            edge.properties.remove(key);
        }
    }
}
