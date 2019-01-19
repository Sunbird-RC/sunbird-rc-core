package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.util.RefLabelHelper;
import io.opensaber.registry.util.TypePropertyHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class VertexWriter {
    private String uuidPropertyName;
    private DatabaseProvider databaseProvider;
    private String parentOSid;
    private static final String EMPTY = "";

    private Logger logger = LoggerFactory.getLogger(VertexWriter.class);

    public VertexWriter(String uuidPropertyName, DatabaseProvider databaseProvider) {
        this.uuidPropertyName = uuidPropertyName;
        this.databaseProvider = databaseProvider;
    }

    /**
     * Ensures a parent vertex existence at the exit of this function
     *
     * @param graph
     * @param parentLabel
     * @return
     */
    public Vertex ensureParentVertex(Graph graph, String parentLabel) {
        Vertex parentVertex = null;
        P<String> lblPredicate = P.eq(parentLabel);

        GraphTraversalSource gtRootTraversal = graph.traversal().clone();
        Iterator<Vertex> iterVertex = gtRootTraversal.V().hasLabel(lblPredicate);
        if (!iterVertex.hasNext()) {
            parentVertex = createVertex(graph, parentLabel);
            //added a property to track vertices belong to parent are indexed
            parentVertex.property(Constants.INDEX_FIELDS, EMPTY);
            logger.info("Parent label {} created {}", parentLabel, parentVertex.id().toString());
        } else {
            parentVertex = iterVertex.next();
            logger.info("Parent label {} already existing {}", parentLabel, parentVertex.id().toString());
        }

        return parentVertex;
    }
    
    public Vertex createVertex(Graph graph, String label) {
        Vertex vertex = graph.addVertex(label);

        vertex.property(TypePropertyHelper.getTypeName(), label);
        vertex.property(uuidPropertyName, databaseProvider.generateId(vertex));

        return vertex;
    }

    /**
     * Writes an array into the database. For each array item, if it is an
     * object creates/populates a new vertex/table and stores the reference
     *
     * @param graph
     * @param vertex
     * @param entryKey
     * @param arrayNode
     */
    private void writeArrayNode(Graph graph, Vertex vertex, String entryKey, ArrayNode arrayNode) {
        List<String> uidList = new ArrayList<>();
        boolean isArrayItemObject = arrayNode.get(0).isObject();
        boolean isSignature = entryKey.equals(Constants.SIGNATURES_STR);

        Vertex blankNode = vertex;
        String label = entryKey;
        if (isArrayItemObject) {
            label = RefLabelHelper.getArrayLabel(entryKey, uuidPropertyName);

            // Create a label with array_node_keyword
            blankNode = createVertex(graph, Constants.ARRAY_NODE_KEYWORD);

            if (isSignature) {
                addEdge(entryKey, blankNode, vertex);
            } else {
                addEdge(entryKey, vertex, blankNode);
            }
            vertex.property(label, blankNode.id().toString());
            blankNode.property(Constants.INTERNAL_TYPE_KEYWORD, entryKey);
            blankNode.property(Constants.ROOT_KEYWORD, parentOSid);
        }

        for (JsonNode jsonNode : arrayNode) {
            if (jsonNode.isObject()) {
                Vertex createdV = processNode(graph, entryKey, jsonNode);
                createdV.property(Constants.ROOT_KEYWORD, parentOSid);
                uidList.add(createdV.id().toString());
                if (isSignature) {
                    addEdge(jsonNode.get(Constants.SIGNATURE_FOR).textValue(), blankNode, createdV);
                } else {
                    addEdge(entryKey + Constants.ARRAY_ITEM, blankNode, createdV);
                }
            } else {
                uidList.add(jsonNode.asText());
            }
        }

        // Set up references on a blank node.
        label = RefLabelHelper.getLabel(entryKey, uuidPropertyName);
        if (isArrayItemObject) {
            blankNode.property(label, StringUtils.arrayToCommaDelimitedString(uidList.toArray()));
        } else {
            blankNode.property(entryKey, StringUtils.arrayToCommaDelimitedString(uidList.toArray()));
        }
    }

    private Vertex processNode(Graph graph, String label, JsonNode jsonObject) {
        Vertex vertex = createVertex(graph, label);

        // This attribute will help identify the root from any child
        if (parentOSid == null || parentOSid.isEmpty()) {
            parentOSid = vertex.id().toString();
        }

        jsonObject.fields().forEachRemaining(entry -> {
            JsonNode entryValue = entry.getValue();
            logger.debug("Processing {} -> {}", entry.getKey(), entry.getValue());

            if (entryValue.isValueNode()) {
                // Directly add under the vertex as a property
                vertex.property(entry.getKey(), ValueType.getValue(entryValue));
            } else if (entryValue.isObject()) {
                // Recursive calls
                Vertex v = processNode(graph, entry.getKey(), entryValue);
                addEdge(entry.getKey(), vertex, v);

                String idToSet = databaseProvider.generateId(v);
                vertex.property(RefLabelHelper.getLabel(entry.getKey(), uuidPropertyName), idToSet);

                v.property(Constants.ROOT_KEYWORD, parentOSid);

                logger.debug("Added edge between {} and {}", vertex.label(), v.label());
            } else if (entryValue.isArray()) {
                writeArrayNode(graph, vertex, entry.getKey(), (ArrayNode) entry.getValue());
            }
        });
        return vertex;
    }

    /**
     * Adds an edge between two vertices
     *
     * @param label
     * @param v1
     *            the source
     * @param v2
     *            the target
     * @return
     */
    public Edge addEdge(String label, Vertex v1, Vertex v2) {
        return v1.addEdge(label, v2);
    }

    /**
     * Fetches the parent. In the current use cases, we expect only one top
     * level parent is passed.
     *
     * @param node
     * @return
     */
    private String getParentName(JsonNode node) {
        return node.fieldNames().next();
    }

    private void setParentId(String id) {
        this.parentOSid = id;
    }

    /**
     * Writes the node entity into the database.
     *
     * @param graph
     * @param node
     * @return
     */
    public String writeNodeEntity(Graph graph, JsonNode node) {
        Vertex resultVertex = null;
        Iterator<Map.Entry<String, JsonNode>> entryIterator = node.fields();
        while (entryIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = entryIterator.next();
            // It is expected that node is wrapped under a root, which is the
            // parent name/definition
            if (entry.getValue().isObject()) {
                resultVertex = processNode(graph, entry.getKey(), entry.getValue());
            }
        }

        return databaseProvider.getId(resultVertex);
    }
}
