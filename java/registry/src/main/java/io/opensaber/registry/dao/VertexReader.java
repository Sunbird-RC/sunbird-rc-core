package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.util.ReadConfigurator;
import io.opensaber.registry.util.RefLabelHelper;
import io.opensaber.registry.util.TypePropertyHelper;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
     *
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
                    if (privatePropertyList.contains(prop.key())) {
                        canAdd &= configurator.isIncludeEncryptedProp();
                    } else if (prop.key().equals(Constants.ROOT_KEYWORD)) {
                        canAdd = false;
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
     *
     * @param currVertex
     * @return
     */
    ArrayNode loadSignatures(Vertex currVertex) {
        ArrayNode signatures = null;
        if (configurator.isIncludeSignatures()) {
            try {
                Iterator<Vertex> signatureArrayIter = currVertex.vertices(Direction.IN, Constants.SIGNATURES_STR);
                Vertex signatureArrayV = signatureArrayIter.next();
                Iterator<Vertex> signatureVertices = signatureArrayV.vertices(Direction.OUT);

                signatures = JsonNodeFactory.instance.arrayNode();
                while (signatureVertices.hasNext()) {
                    Vertex oneSignature = signatureVertices.next();
                    if (!oneSignature.label().equals(entityType)) {
                        ObjectNode signatureNode = constructObject(oneSignature);
                        signatures.add(signatureNode);
                        logger.debug("Added signature node for " + signatureNode.get(Constants.SIGNATURE_FOR));
                    }
                }
            } catch (NoSuchElementException e) {
                logger.debug("There are no signatures found for " + currVertex.label());
            }
        }
        return signatures;
    }

    /**
     * Determines whether the depth setting allows to fetch this additional vertices
     *
     * @param currLevel
     * @param maxLevel
     * @return
     */
    private boolean canLoadVertex(int currLevel, int maxLevel) {
        return currLevel < maxLevel;
    }

    /**
     * Loads the OUT edge vertices of the given vertex
     *
     * @param vertex
     * @param currLevel
     */
    private void loadOtherVertices(Vertex vertex, int currLevel) {
        // NOTE: We can load selective vertices, but we don't know the labels here.
        // So in the process, we will have loaded signature nodes as well here
        Iterator<Vertex> otherVertices = vertex.vertices(Direction.OUT);

        int tempCurrLevel = currLevel;
        while (otherVertices.hasNext()) {
            Vertex currVertex = otherVertices.next();
            VertexProperty internalTypeProp = currVertex.property(Constants.INTERNAL_TYPE_KEYWORD);
            String internalType = internalTypeProp.isPresent() ? internalTypeProp.value().toString() : "";

            // Do not work on the signatures again here.
            if (!currVertex.label().equals(entityType) &&
                            !internalType.equals(Constants.SIGNATURES_STR)) {
                logger.debug("Reading vertex label {} and internal type {}", currVertex.label(), internalType);

                ObjectNode node = constructObject(currVertex);
                uuidNodeMap.put(node.get(uuidPropertyName).textValue(), node);

                // Load any signatures within child entity
                ArrayNode signatureNode = loadSignatures(currVertex);
                if (signatureNode != null) {
                    node.set(Constants.SIGNATURES_STR, signatureNode);
                }

                if (currVertex.property(Constants.TYPE_STR_JSON_LD).value().equals(Constants.ARRAY_NODE_KEYWORD)) {
                    // Not incrementing levels here, because it is we who inserted a blank array_node
                    // for data modelling.
                    loadOtherVertices(currVertex, tempCurrLevel);
                }

                if (canLoadVertex(++tempCurrLevel, configurator.getDepth())) {
                    loadOtherVertices(currVertex, tempCurrLevel);
                    tempCurrLevel = currLevel; // After loading reset for other vertices.
                }
            }
        }
    }

    private void printUuidNodeMap() {
        uuidNodeMap.keySet().forEach(entry -> {
            logger.debug(entry.toString() + " -> " + uuidNodeMap.get(entry).get(Constants.TYPE_STR_JSON_LD));
        });
    }

    /**
     * After loading all the associated objects, this function sets the object content in
     * the right paths
     *
     * @param entityNode
     */
    private ArrayNode expandChildObject(ObjectNode entityNode) {
        ArrayNode resultArr = JsonNodeFactory.instance.arrayNode();

        List<String> fieldNames = new ArrayList<String>();
        entityNode.fieldNames().forEachRemaining(fieldName -> {
            fieldNames.add(fieldName);
        });

        for (String field : fieldNames) {
            JsonNode entry = entityNode.get(field);

            logger.debug("Working on field {}", field );

            if (field.equals(uuidPropertyName)) {
                // has a uuid that may have been populated
                // This could be a textual value or an array
                JsonNode entryValNode = entry;
                String uuidVal = entryValNode.asText();
                JsonNode temp = uuidNodeMap.getOrDefault(uuidVal, null);
                boolean isArray = (temp != null && temp.get(Constants.TYPE_STR_JSON_LD).asText().equals(Constants.ARRAY_NODE_KEYWORD));

                if (temp == null) {
                    // No node loaded for this.
                    // No action required.
                } else if (!isArray) {
                    logger.debug("Field {} Not an array type", field);
                    entityNode.setAll(uuidNodeMap.get(uuidVal));
                } else {
                    // Now first query the uuidNodeMap for the list of items
                    JsonNode blankNode = temp;

                    Iterator<Map.Entry<String, JsonNode>> entryIterator = blankNode.fields();
                    while (entryIterator.hasNext()) {
                        Map.Entry<String, JsonNode> item = entryIterator.next();
                        JsonNode ar = item.getValue();
                        for (JsonNode node : ar) {
                            ObjectNode ovalue = uuidNodeMap.getOrDefault(node.get(uuidPropertyName).asText(), null);
                            if (ovalue != null) {
                                resultArr.add(ovalue);
                            } else {
                                logger.info("Field {} Array items not found in map", field);
                            }
                        }
                    }
                }
            } else if (entry.isObject()) {
                logger.debug("Field {} is an object. Expanding further.", entry);
                ArrayNode expandChildObject = expandChildObject((ObjectNode) entry);
                if (expandChildObject != null && expandChildObject.size() > 0) {
                    entityNode.set(field, expandChildObject);
                }
            }
        }
        return resultArr;
    }

    /**
     * Hits the database to read contents
     *
     * @param osid the id to be read
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

        ArrayNode signatureNode = loadSignatures(rootVertex);
        if (signatureNode != null) {
            rootNode.set(Constants.SIGNATURES_STR, signatureNode);
        } else {
            rootNode.remove(Constants.SIGNATURES_STR);
        }

        if (configurator.getDepth() > 0) {
            loadOtherVertices(rootVertex, currLevel);
        }

        printUuidNodeMap();

        logger.info("Finished loading information. Start creating the response");

        ObjectNode entityNode = JsonNodeFactory.instance.objectNode();
        // For the entity Node, now go and replace the array values with actual objects.
        // The properties could exist anywhere. Refer to the local arrMap.
        expandChildObject(rootNode);

        entityNode.set(entityType, rootNode);

        // After reading the entire type, now trim the @type property
        if (!configurator.isIncludeTypeAttributes()) {
            JSONUtil.removeNode(entityNode, Constants.TYPE_STR_JSON_LD);
        }

        return entityNode;
    }

}
