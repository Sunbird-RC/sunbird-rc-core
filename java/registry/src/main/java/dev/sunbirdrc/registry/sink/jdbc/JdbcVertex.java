package dev.sunbirdrc.registry.sink.jdbc;

import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;

import java.util.*;

/**
 * Vertex implementation backed by database rows using JDBC.
 * Works with per-entity tables (V_{label} naming convention).
 * Uses Long IDs (BIGSERIAL) for SQLG compatibility.
 */
public class JdbcVertex implements Vertex {

    private final JdbcGraph graph;
    private final Long id;
    private String label;
    private Map<String, Object> properties;
    private boolean removed = false;

    public JdbcVertex(JdbcGraph graph, Long id, String label, Map<String, Object> properties) {
        this.graph = graph;
        this.id = id;
        this.label = label;
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
    }

    @Override
    public Edge addEdge(String label, Vertex inVertex, Object... keyValues) {
        if (removed) {
            throw new IllegalStateException("Vertex has been removed");
        }
        ElementHelper.validateLabel(label);
        ElementHelper.legalPropertyKeyValueArray(keyValues);

        Map<String, Object> edgeProperties = new HashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) {
            String key = keyValues[i].toString();
            Object value = keyValues[i + 1];

            // Skip id and label - id is auto-generated, label is set via the method parameter
            if (!key.equals(T.id.getAccessor()) && !key.equals(T.label.getAccessor())) {
                edgeProperties.put(key, value);
            }
        }

        JdbcVertex inJdbcVertex = (JdbcVertex) inVertex;
        return graph.addEdge(this.id, this.label(), inJdbcVertex.id, inJdbcVertex.label(),
                            label, edgeProperties);
    }

    @Override
    public <V> VertexProperty<V> property(VertexProperty.Cardinality cardinality, String key, V value, Object... keyValues) {
        if (removed) {
            throw new IllegalStateException("Vertex has been removed");
        }

        ElementHelper.validateProperty(key, value);

        // For simplicity, we treat all cardinalities as single
        properties.put(key, value);

        // Pass label to graph method for per-entity table update
        graph.updateVertexProperty(id, label(), key, value);

        return new JdbcVertexProperty<>(this, key, value);
    }

    @Override
    public Iterator<Edge> edges(Direction direction, String... edgeLabels) {
        if (removed) {
            return Collections.emptyIterator();
        }
        return graph.getVertexEdges(id, label(), direction, edgeLabels);
    }

    @Override
    public Iterator<Vertex> vertices(Direction direction, String... edgeLabels) {
        if (removed) {
            return Collections.emptyIterator();
        }
        return graph.getAdjacentVertices(id, label(), direction, edgeLabels);
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public String label() {
        if (label == null) {
            // Lazy load label if not available
            Vertex v = graph.getVertexById(id);
            if (v != null) {
                label = v.label();
            }
        }
        return label;
    }

    @Override
    public Graph graph() {
        return graph;
    }

    @Override
    public void remove() {
        if (!removed) {
            // Pass label to graph method for per-entity table deletion
            graph.deleteVertex(id, label());
            removed = true;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> Iterator<VertexProperty<V>> properties(String... propertyKeys) {
        if (removed) {
            return Collections.emptyIterator();
        }

        // Ensure properties are loaded
        if (properties == null || properties.isEmpty()) {
            Vertex v = graph.getVertexById(id);
            if (v instanceof JdbcVertex) {
                properties = ((JdbcVertex) v).properties;
            }
            if (properties == null) {
                properties = new HashMap<>();
            }
        }

        List<VertexProperty<V>> result = new ArrayList<>();

        if (propertyKeys == null || propertyKeys.length == 0) {
            // Return all properties
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                result.add(new JdbcVertexProperty<>(this, entry.getKey(), (V) entry.getValue()));
            }
        } else {
            // Return specific properties
            for (String key : propertyKeys) {
                Object value = properties.get(key);
                if (value != null) {
                    result.add(new JdbcVertexProperty<>(this, key, (V) value));
                }
            }
        }

        return result.iterator();
    }

    @Override
    public <V> VertexProperty<V> property(String key) {
        if (removed) {
            return VertexProperty.empty();
        }

        // Ensure properties are loaded
        if (properties == null || properties.isEmpty()) {
            Vertex v = graph.getVertexById(id);
            if (v instanceof JdbcVertex) {
                properties = ((JdbcVertex) v).properties;
            }
            if (properties == null) {
                properties = new HashMap<>();
            }
        }

        Object value = properties.get(key);
        if (value != null) {
            return new JdbcVertexProperty<>(this, key, (V) value);
        }
        return VertexProperty.empty();
    }

    @Override
    public <V> VertexProperty<V> property(String key, V value) {
        return property(VertexProperty.Cardinality.single, key, value);
    }

    public Map<String, Object> getPropertiesMap() {
        return properties;
    }

    public void setPropertiesMap(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vertex)) return false;
        Vertex that = (Vertex) o;
        return Objects.equals(id, that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "v[" + id + "]";
    }

    /**
     * Returns the value of a property directly.
     * Useful for VertexReader.
     */
    @SuppressWarnings("unchecked")
    public <V> V value(String key) {
        VertexProperty<V> prop = property(key);
        if (prop.isPresent()) {
            return prop.value();
        }
        throw new NoSuchElementException("Property " + key + " not found");
    }
}
