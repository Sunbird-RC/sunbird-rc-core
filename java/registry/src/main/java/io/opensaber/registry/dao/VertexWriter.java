package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.util.ArrayHelper;
import io.opensaber.registry.util.RefLabelHelper;
import io.opensaber.registry.util.TypePropertyHelper;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Helps in writing a vertex, edge into the database
 */
public class VertexWriter {
    private String uuidPropertyName;
    private Graph graph;
    private DatabaseProvider databaseProvider;
    private String parentOSid;
    private static final String EMPTY_STR = "";

    private Logger logger = LoggerFactory.getLogger(VertexWriter.class);

    public VertexWriter(Graph graph, DatabaseProvider databaseProvider, String uuidPropertyName) {
        this.graph = graph;
        this.databaseProvider = databaseProvider;
        this.uuidPropertyName = uuidPropertyName;
    }

    /**
     * Ensures a parent vertex existence at the exit of this function
     *
     * @param parentLabel
     * @return
     */
    public Vertex ensureParentVertex(String parentLabel) {
        Vertex parentVertex = null;
        P<String> lblPredicate = P.eq(parentLabel);

        GraphTraversalSource gtRootTraversal = graph.traversal().clone();
        Iterator<Vertex> iterVertex = gtRootTraversal.V().hasLabel(lblPredicate);
        if (!iterVertex.hasNext()) {
            parentVertex = createVertex(parentLabel);

            //added a property to track vertices belong to parent are indexed
            parentVertex.property(Constants.INDEX_FIELDS, EMPTY_STR);
            parentVertex.property(Constants.UNIQUE_INDEX_FIELDS, EMPTY_STR);
            logger.info("Parent label {} created {}", parentLabel, parentVertex.id().toString());
        } else {
            parentVertex = iterVertex.next();
            logger.info("Parent label {} already existing {}", parentLabel, databaseProvider.getId(parentVertex));
        }

        return parentVertex;
    }


    /**
     * Creates a vertex - each vertex would have a @type and uuidPropertyName attribute
     * @param label - the string you want the vertex to be labelled
     * @return
     */
    public Vertex createVertex(String label) {
        Vertex vertex = graph.addVertex(label);

        vertex.property(TypePropertyHelper.getTypeName(), label);
        vertex.property(uuidPropertyName, databaseProvider.generateId(vertex));

        return vertex;
    }
    
    /**
     * Updates index fields property of parent vertex for a given propertyName
     * 
     * @param parentVertex
     * @param propertyName
     * @param indexFields
     */
    public void updateParentIndexProperty(Vertex parentVertex, String propertyName, List<String> indexFields){
        if (indexFields.size() > 0) {
            StringBuilder properties = new StringBuilder(String.join(",", indexFields));        
            parentVertex.property(propertyName, properties.toString());
            logger.info("parent vertex property {}:{}", propertyName, properties);

        }            
    }

    /**
     * Writes an array into the database. For each array item, if it is an
     * object creates/populates a new vertex/table and stores the reference
     *
     * @param vertex
     * @param entryKey
     * @param arrayNode
     */
    private void writeArrayNode(Vertex vertex, String entryKey, ArrayNode arrayNode, boolean isUpdate) {
        List<Object> uidList = new ArrayList<>();
        boolean isArrayItemObject = (arrayNode !=null && arrayNode.size() > 0 && arrayNode.get(0).isObject());
        boolean isSignature = entryKey.equals(Constants.SIGNATURES_STR);
        Vertex blankNode = vertex;
        String label;

        identifyParentOSid(vertex);

        if (isArrayItemObject) {
            label = RefLabelHelper.getArrayLabel(entryKey, uuidPropertyName);

            // Create a label with array_node_keyword
            blankNode = createVertex(Constants.ARRAY_NODE_KEYWORD);

            if (isSignature) {
                addEdge(entryKey, blankNode, vertex);
            } else {
                addEdge(entryKey, vertex, blankNode);
            }
            vertex.property(label, databaseProvider.getId(blankNode));
            blankNode.property(Constants.INTERNAL_TYPE_KEYWORD, entryKey);
            blankNode.property(Constants.ROOT_KEYWORD, parentOSid);
        }

        for (JsonNode jsonNode : arrayNode) {
            if (jsonNode.isObject()) {
                Vertex createdV = processNode(entryKey, jsonNode);
                ObjectNode objectNode = (ObjectNode) jsonNode;
                objectNode.put(uuidPropertyName,databaseProvider.getId(createdV));
                createdV.property(Constants.ROOT_KEYWORD, parentOSid);
                uidList.add(databaseProvider.getId(createdV));
                if (isSignature) {
                    Edge e = addEdge(Constants.SIGNATURE_FOR+Constants.ARRAY_ITEM, blankNode, createdV);
                    e.property(Constants.SIGNATURE_FOR, jsonNode.get(Constants.SIGNATURE_FOR).textValue());
                } else {
                    addEdge(entryKey + Constants.ARRAY_ITEM, blankNode, createdV);
                }
            } else {
                uidList.add(ValueType.getValue(jsonNode));
            }
        }

        // Set up references on a blank node.
        label = RefLabelHelper.getLabel(entryKey, uuidPropertyName);
        if (isArrayItemObject) {
            blankNode.property(label, ArrayHelper.formatToString(uidList));
        } else {
            blankNode.property(entryKey, ArrayHelper.formatToString(uidList));
        }
    }

    public void createArrayNode(Vertex vertex, String entryKey, ArrayNode arrayNode) {
        writeArrayNode(vertex, entryKey, arrayNode, false);
    }

    public void writeSingleNode(Vertex parentVertex, String label, JsonNode entryValue) {
        Vertex v = processNode(label, entryValue);
        ObjectNode object = (ObjectNode) entryValue;
        addEdge(label, parentVertex, v);

        String idToSet = databaseProvider.getId(v);
        object.put(uuidPropertyName,idToSet);
        parentVertex.property(RefLabelHelper.getLabel(label, uuidPropertyName), idToSet);

        identifyParentOSid(parentVertex);
        v.property(Constants.ROOT_KEYWORD, parentOSid);

        logger.debug("Added edge between {} and {}", parentVertex.label(), v.label());
    }

    private void identifyParentOSid(Vertex vertex) {
        // This attribute will help identify the root from any child
        if (parentOSid == null || parentOSid.isEmpty()) {
            parentOSid = databaseProvider.getId(vertex);
        }
    }

    private Vertex processNode(String label, JsonNode jsonObject) {
        Vertex vertex = createVertex(label);
        identifyParentOSid(vertex);

        jsonObject.fields().forEachRemaining(entry -> {
            JsonNode entryValue = entry.getValue();
            logger.debug("Processing {} -> {}", entry.getKey(), entry.getValue());

            if (entryValue.isValueNode()) {
                // Directly add under the vertex as a property
                vertex.property(entry.getKey(), ValueType.getValue(entryValue));
            } else if (entryValue.isObject()) {
                // Recursive calls
                writeSingleNode(vertex, entry.getKey(), entryValue);
            } else if (entryValue.isArray()) {
                createArrayNode(vertex, entry.getKey(), (ArrayNode) entry.getValue());
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
     * Writes the node entity into the database.
     *
     * @param node
     * @return
     */
    public String writeNodeEntity(JsonNode node) {
        Vertex resultVertex = null;
        String rootOsid = null;
        Iterator<Map.Entry<String, JsonNode>> entryIterator = node.fields();
        while (entryIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = entryIterator.next();
            ObjectNode entryObject = (ObjectNode) entry.getValue();
            // It is expected that node is wrapped under a root, which is the
            // parent name/definition
            if (entry.getValue().isObject()) {
                resultVertex = processNode(entry.getKey(), entry.getValue());
                rootOsid = databaseProvider.getId(resultVertex);
                entryObject.put(uuidPropertyName,rootOsid);
            }
        }
        return rootOsid;
    }
}