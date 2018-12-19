package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.registry.util.ReadConfigurator;
import io.opensaber.registry.util.RefLabelHelper;
import io.opensaber.registry.util.TypePropertyHelper;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Given a vertex from the graph, constructs a json out it
 */
public class VertexReader {
    private Graph graph;
    private ReadConfigurator configurator;
    private String uuidPropertyName;
    private List<String> privatePropertyList;
    private HashMap<String, ObjectNode> uuidNodeMap = new HashMap<>();
    private String entityType;

    private Logger logger = LoggerFactory.getLogger(VertexReader.class);

    public VertexReader(Graph graph, ReadConfigurator configurator, String uuidPropertyName, List<String> pvtPropertyList) {
        this.graph = graph;
        this.configurator = configurator;
        this.uuidPropertyName = uuidPropertyName;
        this.privatePropertyList = pvtPropertyList;
    }

    /**
     * For the given vertex, constructs the json ObjectNode.
     * If the given vertex, contains an array, a mere reference is put up without any expansion.
     * @param currVertex
     * @return
     */
    private ObjectNode constructObject(Vertex currVertex) {

        ObjectNode contentNode = JsonNodeFactory.instance.objectNode();
        Iterator<VertexProperty<Object>> properties = currVertex.properties();
        while (properties.hasNext()) {
            VertexProperty<Object> prop = properties.next();
            if (!RefLabelHelper.isParentLabel(prop.key())) {
                if (RefLabelHelper.isRefLabel(prop.key(), uuidPropertyName)) {
                    logger.debug("{} is a referenced entity", prop.key());
                    // There is a chance that it may have been already read or otherwise.

                    String refEntityName = RefLabelHelper.getRefEntityName(prop.key());
                    String[] valueArr = prop.value().toString().split("\\s*,\\s*");
                    boolean isObjectNode = valueArr.length == 1;

                    ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                    for (String value : valueArr) {
                        ObjectNode on = JsonNodeFactory.instance.objectNode();
                        on.put(uuidPropertyName, value);
                        arrayNode.add(on);
                    }
                    if (isObjectNode) {
                        contentNode.set(refEntityName, arrayNode.get(0));
                    } else {
                        contentNode.set(refEntityName, arrayNode);
                    }
                } else {
                    logger.debug("{} is a simple value", prop.key());
                    boolean canAdd = true;
                    if (TypePropertyHelper.isTypeProperty(prop.key())) {
                        canAdd &= configurator.isIncludeTypeAttributes();
                    } else if (privatePropertyList.contains(prop.key())) {
                        canAdd &= configurator.isIncludeEncryptedProp();
                    }

                    if (canAdd) {
                        contentNode.put(prop.key(), prop.value().toString());
                    }
                }
            } else {
                logger.debug("Root type, ignoring");
            }
        }

        // Here we set the id
        contentNode.put(uuidPropertyName, currVertex.id().toString());

        return contentNode;
    }

    /**
     * Loads the signature vertices
     * @param currVertex
     * @return
     */
    ArrayNode loadSignatures(Vertex currVertex) {
        ArrayNode signatures = null;
        if (configurator.isIncludeSignatures()) {
            signatures = JsonNodeFactory.instance.arrayNode();
            Iterator<Vertex> signatureVertices = currVertex.vertices(Direction.IN, "Signatures");
            while (signatureVertices.hasNext()) {
                Vertex oneSignature = signatureVertices.next();
                ObjectNode signatureNode = constructObject(oneSignature);
                signatures.add(signatureNode);
            }
        }
        return signatures;
    }

    /**
     * Determines whether the depth setting allows to fetch this additional vertices
     * @param currLevel
     * @param maxLevel
     * @return
     */
    private boolean canLoadVertex(int currLevel, int maxLevel) {
        return currLevel < maxLevel;
    }

    /**
     * Loads the OUT edge vertices of the given vertex
     * @param vertex
     * @param currLevel
     */
    private void loadOtherVertices(Vertex vertex, int currLevel) {
        Iterator<Vertex> otherVertices = vertex.vertices(Direction.OUT);
        while (otherVertices.hasNext()) {
            Vertex currVertex = otherVertices.next();
            ObjectNode node = constructObject(currVertex);
            uuidNodeMap.put(node.get(uuidPropertyName).textValue(), node);

            if (configurator.isIncludeSignatures()) {
                ArrayNode signatureNode = loadSignatures(currVertex);
                node.set("Signatures", signatureNode);
            }

            if (canLoadVertex(++currLevel, configurator.getDepth())) {
                loadOtherVertices(currVertex, currLevel);
            }
        }
    }

    /**
     * After loading all the associated objects, this function sets the object content in
     * the right paths
     * @param entityNode
     */
    private void expandChildObject(ObjectNode entityNode) {
        entityNode.fields().forEachRemaining(entry -> {
            if (entry.getValue().toString().compareToIgnoreCase(entityType) == 0) {
                // Same as parent, don't expand
                // do nothing
                return;
            } else if (entry.getKey().compareToIgnoreCase(uuidPropertyName) == 0){
                // has a uuid that may have been populated
                // This could be a textual value or an array
                if (uuidNodeMap.containsKey(entry.getValue().textValue())) {
                    entityNode.setAll(uuidNodeMap.get(entry.getValue().textValue()));
                } else {
                    logger.debug("Key {} not found", entry.getValue());
                }
            } else if (entry.getValue().isObject()) {
                logger.debug("Key {} is an object. Expanding further.", entry.getKey());
                expandChildObject((ObjectNode) entry.getValue());
            } else if (entry.getValue().isArray()) {
                logger.debug("Key {} is an array.", entry.getKey());
                ArrayNode ar = (ArrayNode) entry.getValue();
                ArrayNode expanded = JsonNodeFactory.instance.arrayNode();
                for (JsonNode node : ar) {
                    if (!node.get(uuidPropertyName).isNull() &&
                            uuidNodeMap.containsKey(node.get(uuidPropertyName).textValue())) {
                            ObjectNode ovalue = uuidNodeMap.get(node.get(uuidPropertyName).textValue());
                            expanded.add(ovalue);
                    } else {
                        logger.info("Not found in map, maybe too deep");
                    }
                }

                if (expanded.size() != 0) {
                    entry.setValue(expanded);
                }
            }
        });
    }

    /**
     * Hits the database to read contents
     *
     * @param osid         the id to be read
     * @return
     * @throws Exception
     */
    public JsonNode read(String osid) throws Exception {
        Iterator<Vertex> itrV = graph.vertices(osid);
        if (!itrV.hasNext()) {
            throw new Exception("Invalid id");
        }
        Vertex rootVertex = itrV.next();

        int currLevel = 0;
        ObjectNode rootNode = constructObject(rootVertex);
        entityType = rootNode.get(TypePropertyHelper.getTypeName()).textValue();

        // Set the type for the root node, so as to wrap.
        uuidNodeMap.put(rootNode.get(uuidPropertyName).textValue(), rootNode);

        if (configurator.getDepth() > 0) {
            loadOtherVertices(rootVertex, currLevel);
        }

        ObjectNode entityNode = JsonNodeFactory.instance.objectNode();
        entityNode.set(entityType, rootNode);

        // For the entity Node, now go and replace the array values with actual objects.
        // The properties could exist anywhere. Refer to the local arrMap.
        expandChildObject(entityNode);

        return entityNode;
    }

}
