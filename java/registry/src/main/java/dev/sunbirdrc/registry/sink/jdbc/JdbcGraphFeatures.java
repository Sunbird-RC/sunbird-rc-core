package dev.sunbirdrc.registry.sink.jdbc;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

/**
 * Graph features declaration for JDBC-backed graph.
 * Defines what capabilities the JdbcGraph supports.
 */
public class JdbcGraphFeatures implements Graph.Features {

    private final JdbcGraphGraphFeatures graphFeatures = new JdbcGraphGraphFeatures();
    private final JdbcVertexFeatures vertexFeatures = new JdbcVertexFeatures();
    private final JdbcEdgeFeatures edgeFeatures = new JdbcEdgeFeatures();

    @Override
    public GraphFeatures graph() {
        return graphFeatures;
    }

    @Override
    public VertexFeatures vertex() {
        return vertexFeatures;
    }

    @Override
    public EdgeFeatures edge() {
        return edgeFeatures;
    }

    @Override
    public String toString() {
        return StringFactory.featureString(this);
    }

    /**
     * Graph-level features.
     */
    public static class JdbcGraphGraphFeatures implements GraphFeatures {
        private final JdbcVariableFeatures variableFeatures = new JdbcVariableFeatures();

        @Override
        public boolean supportsComputer() {
            return false;
        }

        @Override
        public boolean supportsPersistence() {
            return true;
        }

        @Override
        public boolean supportsConcurrentAccess() {
            return true;
        }

        @Override
        public boolean supportsTransactions() {
            return true;
        }

        @Override
        public boolean supportsThreadedTransactions() {
            return false;
        }

        @Override
        public VariableFeatures variables() {
            return variableFeatures;
        }
    }

    /**
     * Vertex-level features.
     */
    public static class JdbcVertexFeatures implements VertexFeatures {
        private final JdbcVertexPropertyFeatures vertexPropertyFeatures = new JdbcVertexPropertyFeatures();

        @Override
        public VertexProperty.Cardinality getCardinality(String key) {
            return VertexProperty.Cardinality.single;
        }

        @Override
        public boolean supportsAddVertices() {
            return true;
        }

        @Override
        public boolean supportsRemoveVertices() {
            return true;
        }

        @Override
        public boolean supportsMultiProperties() {
            return false;
        }

        @Override
        public boolean supportsMetaProperties() {
            return false;
        }

        @Override
        public boolean supportsCustomIds() {
            return true;
        }

        @Override
        public boolean supportsUserSuppliedIds() {
            return true;
        }

        @Override
        public boolean supportsNumericIds() {
            return false;
        }

        @Override
        public boolean supportsStringIds() {
            return true;
        }

        @Override
        public boolean supportsUuidIds() {
            return true;
        }

        @Override
        public boolean supportsAnyIds() {
            return false;
        }

        @Override
        public boolean willAllowId(Object id) {
            return id instanceof String;
        }

        @Override
        public VertexPropertyFeatures properties() {
            return vertexPropertyFeatures;
        }
    }

    /**
     * Edge-level features.
     */
    public static class JdbcEdgeFeatures implements EdgeFeatures {
        private final JdbcEdgePropertyFeatures edgePropertyFeatures = new JdbcEdgePropertyFeatures();

        @Override
        public boolean supportsAddEdges() {
            return true;
        }

        @Override
        public boolean supportsRemoveEdges() {
            return true;
        }

        @Override
        public boolean supportsCustomIds() {
            return true;
        }

        @Override
        public boolean supportsUserSuppliedIds() {
            return true;
        }

        @Override
        public boolean supportsNumericIds() {
            return false;
        }

        @Override
        public boolean supportsStringIds() {
            return true;
        }

        @Override
        public boolean supportsUuidIds() {
            return true;
        }

        @Override
        public boolean supportsAnyIds() {
            return false;
        }

        @Override
        public boolean willAllowId(Object id) {
            return id instanceof String;
        }

        @Override
        public EdgePropertyFeatures properties() {
            return edgePropertyFeatures;
        }
    }

    /**
     * Vertex property features.
     */
    public static class JdbcVertexPropertyFeatures implements VertexPropertyFeatures {

        @Override
        public boolean supportsUserSuppliedIds() {
            return false;
        }

        @Override
        public boolean supportsNumericIds() {
            return false;
        }

        @Override
        public boolean supportsStringIds() {
            return true;
        }

        @Override
        public boolean supportsUuidIds() {
            return false;
        }

        @Override
        public boolean supportsCustomIds() {
            return false;
        }

        @Override
        public boolean supportsAnyIds() {
            return false;
        }

        @Override
        public boolean supportsBooleanValues() {
            return true;
        }

        @Override
        public boolean supportsByteValues() {
            return true;
        }

        @Override
        public boolean supportsDoubleValues() {
            return true;
        }

        @Override
        public boolean supportsFloatValues() {
            return true;
        }

        @Override
        public boolean supportsIntegerValues() {
            return true;
        }

        @Override
        public boolean supportsLongValues() {
            return true;
        }

        @Override
        public boolean supportsMapValues() {
            return true;
        }

        @Override
        public boolean supportsMixedListValues() {
            return true;
        }

        @Override
        public boolean supportsBooleanArrayValues() {
            return true;
        }

        @Override
        public boolean supportsByteArrayValues() {
            return true;
        }

        @Override
        public boolean supportsDoubleArrayValues() {
            return true;
        }

        @Override
        public boolean supportsFloatArrayValues() {
            return true;
        }

        @Override
        public boolean supportsIntegerArrayValues() {
            return true;
        }

        @Override
        public boolean supportsLongArrayValues() {
            return true;
        }

        @Override
        public boolean supportsStringArrayValues() {
            return true;
        }

        @Override
        public boolean supportsSerializableValues() {
            return false;
        }

        @Override
        public boolean supportsStringValues() {
            return true;
        }

        @Override
        public boolean supportsUniformListValues() {
            return true;
        }
    }

    /**
     * Edge property features.
     */
    public static class JdbcEdgePropertyFeatures implements EdgePropertyFeatures {

        @Override
        public boolean supportsBooleanValues() {
            return true;
        }

        @Override
        public boolean supportsByteValues() {
            return true;
        }

        @Override
        public boolean supportsDoubleValues() {
            return true;
        }

        @Override
        public boolean supportsFloatValues() {
            return true;
        }

        @Override
        public boolean supportsIntegerValues() {
            return true;
        }

        @Override
        public boolean supportsLongValues() {
            return true;
        }

        @Override
        public boolean supportsMapValues() {
            return true;
        }

        @Override
        public boolean supportsMixedListValues() {
            return true;
        }

        @Override
        public boolean supportsBooleanArrayValues() {
            return true;
        }

        @Override
        public boolean supportsByteArrayValues() {
            return true;
        }

        @Override
        public boolean supportsDoubleArrayValues() {
            return true;
        }

        @Override
        public boolean supportsFloatArrayValues() {
            return true;
        }

        @Override
        public boolean supportsIntegerArrayValues() {
            return true;
        }

        @Override
        public boolean supportsLongArrayValues() {
            return true;
        }

        @Override
        public boolean supportsStringArrayValues() {
            return true;
        }

        @Override
        public boolean supportsSerializableValues() {
            return false;
        }

        @Override
        public boolean supportsStringValues() {
            return true;
        }

        @Override
        public boolean supportsUniformListValues() {
            return true;
        }
    }

    /**
     * Graph variable features.
     */
    public static class JdbcVariableFeatures implements VariableFeatures {

        @Override
        public boolean supportsBooleanValues() {
            return true;
        }

        @Override
        public boolean supportsByteValues() {
            return true;
        }

        @Override
        public boolean supportsDoubleValues() {
            return true;
        }

        @Override
        public boolean supportsFloatValues() {
            return true;
        }

        @Override
        public boolean supportsIntegerValues() {
            return true;
        }

        @Override
        public boolean supportsLongValues() {
            return true;
        }

        @Override
        public boolean supportsMapValues() {
            return true;
        }

        @Override
        public boolean supportsMixedListValues() {
            return true;
        }

        @Override
        public boolean supportsBooleanArrayValues() {
            return true;
        }

        @Override
        public boolean supportsByteArrayValues() {
            return true;
        }

        @Override
        public boolean supportsDoubleArrayValues() {
            return true;
        }

        @Override
        public boolean supportsFloatArrayValues() {
            return true;
        }

        @Override
        public boolean supportsIntegerArrayValues() {
            return true;
        }

        @Override
        public boolean supportsLongArrayValues() {
            return true;
        }

        @Override
        public boolean supportsStringArrayValues() {
            return true;
        }

        @Override
        public boolean supportsSerializableValues() {
            return false;
        }

        @Override
        public boolean supportsStringValues() {
            return true;
        }

        @Override
        public boolean supportsUniformListValues() {
            return true;
        }
    }
}
