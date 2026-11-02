package dev.sunbirdrc.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.exception.RecordNotFoundException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.sink.DatabaseProvider;
import dev.sunbirdrc.registry.util.*;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static dev.sunbirdrc.registry.middleware.util.Constants.DID_TYPE;

/**
 * Given a vertex from the graph, constructs a json out it
 */
public class VertexReader {
    private DatabaseProvider databaseProvider;
    private Graph graph;
    private ReadConfigurator configurator;
    private String uuidPropertyName;
    private LinkedHashMap<String, ObjectNode> uuidNodeMap = new LinkedHashMap<>();
    private String entityType;
    private IDefinitionsManager definitionsManager;
    private Vertex rootVertex;
    private LinkedHashMap<String, Vertex> uuidVertexMap = new LinkedHashMap<>();

    private boolean expandReferenceObj;
    private Logger logger = LoggerFactory.getLogger(VertexReader.class);


    public VertexReader(DatabaseProvider databaseProvider, Graph graph, ReadConfigurator configurator, String uuidPropertyName,
                        IDefinitionsManager definitionsManager, boolean expandReferenceObj) {
        this.databaseProvider = databaseProvider;
        this.graph = graph;
        this.configurator = configurator;
        this.uuidPropertyName = uuidPropertyName;
        this.definitionsManager = definitionsManager;
        this.expandReferenceObj = expandReferenceObj;
    }

    /**
     * Retrieves all vertex UUID for given all labels.
     */
    public List<String> getUUIDs(Set<String> labels) {
        List<String> uuids = new ArrayList<>();
        GraphTraversal<Vertex, Vertex> graphTraversal = graph.traversal().V();
        while (graphTraversal.hasNext()) {
            Vertex v = graphTraversal.next();
            uuids.add(databaseProvider.getId(v));
            logger.debug("vertex info- label :" + v.label() + " id: " + v.id());
        }
        return uuids;
    }

    /**
     * Returns whether or not the given key could be added in response or not.
     * @param key
     * @return
     */
    private boolean canAdd(String key) {
        boolean canAdd = true;
        if (key.equals(Constants.ROOT_KEYWORD)) {
            canAdd &= configurator.isIncludeRootIdentifiers();
        } else if (key.equals(uuidPropertyName)){
            canAdd &= configurator.isIncludeIdentifiers();
        }
        return canAdd;
    }

    /**
     * For the given vertex, constructs the json ObjectNode. If the given
     * vertex, contains an array, a mere reference is put up without any
     * expansion.
     *
     * @param currVertex
     * @return
     */
    public ObjectNode constructObject(Vertex currVertex) {

        ObjectNode contentNode = JsonNodeFactory.instance.objectNode();
        Iterator<VertexProperty<Object>> properties = currVertex.properties();
        while (properties.hasNext()) {
            VertexProperty<Object> prop = properties.next();
            if (!RefLabelHelper.isParentLabel(prop.key())) {
                boolean isArrayType = ArrayHelper.isArray(prop.value().toString());
                String propValue = ArrayHelper.removeSquareBraces(prop.value().toString());
                if (RefLabelHelper.isRefLabel(prop.key(), uuidPropertyName)) {
                    logger.debug("{} is a referenced entity", prop.key());
                    // There is a chance that it may have been already read or
                    // otherwise.

                    String refEntityName = RefLabelHelper.getRefEntityName(prop.key());
                    String[] valueArr = propValue.split("\\s*,\\s*");
                    boolean isObjectNode = valueArr.length == 1;

                    ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                    for (String value : valueArr) {
                        ObjectNode on = JsonNodeFactory.instance.objectNode();
                        on.put(uuidPropertyName, value);
                        arrayNode.add(on);
                    }
                    if (isObjectNode && !isArrayType) {
                        contentNode.set(refEntityName, arrayNode.get(0));
                    } else {
                        contentNode.set(refEntityName, arrayNode);
                    }
                } else {
                    logger.debug("{} is a simple value", prop.key());
                    if (canAdd(prop.key())) {
                        if (isArrayType) {
                            ArrayNode arrayNode = ArrayHelper.constructArrayNode(prop.value().toString());
                            contentNode.set(prop.key(), arrayNode);
                        } else {
                            ValueType.setValue(contentNode, prop.key(), prop.value());
                        }
                    } else {
                        logger.debug("-- Not adding");
                    }
                }
            } else {
                logger.debug("Root type, ignoring");
            }
        }

        // In Neo4j, the uuidPropertyName is given a special handling
        // It is not part of the list of attributes, even though it is persisted
        contentNode.put(uuidPropertyName, databaseProvider.getId(currVertex));

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
                Iterator<Vertex> signatureVertices = signatureArrayV.vertices(Direction.OUT, Constants.SIGNATURE_FOR+Constants.ARRAY_ITEM);

                signatures = JsonNodeFactory.instance.arrayNode();
                while (signatureVertices.hasNext()) {
                    Vertex oneSignature = signatureVertices.next();
                    if( oneSignature.label().equalsIgnoreCase(Constants.SIGNATURES_STR) &&
                            !(oneSignature.property(Constants.STATUS_KEYWORD).isPresent() &&
                            oneSignature.property(Constants.STATUS_KEYWORD).value().toString().equalsIgnoreCase(Constants.STATUS_INACTIVE))) {
                        ObjectNode signatureNode = constructObject(oneSignature);
                        signatures.add(signatureNode);
                        logger.debug("Added signature node for " + signatureNode.get(Constants.SIGNATURE_FOR));

                        if (configurator.isIncludeIdentifiers()) {
                            uuidVertexMap.put(databaseProvider.getId(oneSignature), oneSignature);
                        }
                    }
                }
                uuidVertexMap.put(Constants.SIGNATURES_STR, signatureArrayV);
            } catch (NoSuchElementException e) {
                logger.debug("There are no signatures found for " + currVertex.label());
            }
        }
        return signatures;
    }

    /**
     * Determines whether the depth setting allows to fetch this additional
     * vertices
     *
     * @param currLevel
     * @param maxLevel
     * @return
     */
    private boolean canLoadVertex(int currLevel, int maxLevel) {
        return currLevel < maxLevel;
    }

    /**
     * Populates the internal maps with uuid, Node and uuid, Vertex pairs
     * @param node
     * @param vertex
     */
    private void populateMaps(ObjectNode node, Vertex vertex) {
        String uuid = node.get(uuidPropertyName).textValue();
        uuidNodeMap.put(uuid, node);
        uuidVertexMap.put(uuid, vertex);
    }

    /**
     * Loads the OUT edge vertices of the given vertex
     *
     * @param vertex
     * @param currLevel
     * @param visitedUuids uuids already visited in this top-level read, to guard against graph cycles
     */
    private void loadOtherVertices(Vertex vertex, int currLevel, Set<String> visitedUuids) {
        // NOTE: We can load selective vertices, but we don't know the labels
        // here.
        // So in the process, we will have loaded signature nodes as well here
        //
        // Reference edges (RefLabelHelper.REFERENCE_EDGE_PREFIX) are excluded: they are a
        // distinct concern (entityReferences / ownedReferences), handled by
        // inlineOwnedReferences, not this generic composition walk. Without this exclusion,
        // this method claims an owned reference's target uuid into visitedUuids first, and
        // inlineOwnedReferences's own cycle-guard then silently skips it as "already visited".
        Set<Object> referenceEdgeTargetIds = new HashSet<>();
        Iterator<Edge> refEdges = vertex.edges(Direction.OUT);
        while (refEdges.hasNext()) {
            Edge refEdge = refEdges.next();
            if (refEdge.label().startsWith(RefLabelHelper.REFERENCE_EDGE_PREFIX)) {
                referenceEdgeTargetIds.add(refEdge.inVertex().id());
            }
        }

        Iterator<Vertex> otherVertices = vertex.vertices(Direction.OUT);

        int tempCurrLevel = currLevel;
        while (otherVertices.hasNext()) {
            Vertex currVertex = otherVertices.next();
            if (referenceEdgeTargetIds.contains(currVertex.id())) {
                continue;
            }
            if(currVertex.property(Constants.STATUS_KEYWORD).isPresent() &&
                    currVertex.property(Constants.STATUS_KEYWORD).value().equals(Constants.STATUS_INACTIVE)){
                continue;
            }
            String currUuid = databaseProvider.getId(currVertex);
            if (!visitedUuids.add(currUuid)) {
                logger.debug("Already visited vertex {}, skipping to avoid cycle", currUuid);
                continue;
            }
            VertexProperty internalTypeProp = currVertex.property(Constants.INTERNAL_TYPE_KEYWORD);
            String internalType = internalTypeProp.isPresent() ? internalTypeProp.value().toString() : "";
            logger.debug("Current vertext {}", currVertex.label());

            // Do not work on the signatures again here.
            if (!currVertex.label().equals(entityType) && !internalType.equals(Constants.SIGNATURES_STR)) {
                logger.debug("Reading vertex label {} and internal type {}", currVertex.label(), internalType);

                ObjectNode node = constructObject(currVertex);
                populateMaps(node, currVertex);

                // Load any signatures within child entity
                ArrayNode signatureNode = loadSignatures(currVertex);
                if (signatureNode != null) {
                    node.set(Constants.SIGNATURES_STR, signatureNode);
                }

                if (currVertex.property(Constants.TYPE_STR_JSON_LD).value().equals(Constants.ARRAY_NODE_KEYWORD)) {
                    // Not incrementing levels here, because it is we who
                    // inserted a blank array_node
                    // for data modelling.
                    loadOtherVertices(currVertex, tempCurrLevel, visitedUuids);
                }

                if (canLoadVertex(++tempCurrLevel, configurator.getDepth())) {
                    logger.debug("Going to load children of {}", currVertex.label());
                    loadOtherVertices(currVertex, tempCurrLevel, visitedUuids);
                    logger.debug("End load children of {}", currVertex.label());
                    tempCurrLevel = currLevel;
                }
            }
        }
    }

    /**
     * Inlines every entity that OWNS-REFERENCES this vertex (dev.sunbirdrc.registry.util
     * .OSSchemaConfiguration#ownedReferences), grouped by the referencer's own entity type, so a
     * read of the target (e.g. Student) embeds its owned children (e.g. Records, Attendance)
     * directly — no second call needed. Scoped tightly to the edges that feature forms: an edge
     * whose label starts with RefLabelHelper.REFERENCE_EDGE_PREFIX ("ref_") and carries NO
     * RefLabelHelper.REFERENCE_EDGE_MARKER property. A MARKED "ref_" edge is the separate,
     * non-owning entityReferences case (e.g. this vertex's own outgoing reference to another
     * entity) and must stay invisible on read, exactly as before this method existed.
     *
     * @param vertex the vertex being read
     * @param node the ObjectNode already built for vertex, mutated in place
     * @param currLevel current recursion depth, same counter constructObject's own expansion uses
     * @param visitedUuids uuids already visited in this top-level read, to guard against graph cycles —
     *                     the SAME set instance used elsewhere in this read, not a fresh one
     */
    private void inlineOwnedReferences(Vertex vertex, ObjectNode node, int currLevel, Set<String> visitedUuids) {
        if (!canLoadVertex(currLevel, configurator.getDepth())) {
            return;
        }
        Set<Object> qualifyingIds = new HashSet<>();
        Iterator<Edge> edges = vertex.edges(Direction.OUT);
        while (edges.hasNext()) {
            Edge edge = edges.next();
            boolean hasPrefix = edge.label().startsWith(RefLabelHelper.REFERENCE_EDGE_PREFIX);
            boolean hasMarker = edge.property(RefLabelHelper.REFERENCE_EDGE_MARKER).isPresent();
            if (hasPrefix && !hasMarker) {
                qualifyingIds.add(edge.inVertex().id());
            }
        }
        if (qualifyingIds.isEmpty()) {
            return;
        }
        Map<String, ArrayNode> byEntityType = new LinkedHashMap<>();
        Iterator<Vertex> otherVertices = vertex.vertices(Direction.OUT);
        while (otherVertices.hasNext()) {
            Vertex referencer = otherVertices.next();
            if (!qualifyingIds.contains(referencer.id())) {
                continue;
            }
            if (referencer.property(Constants.STATUS_KEYWORD).isPresent()
                    && referencer.property(Constants.STATUS_KEYWORD).value().equals(Constants.STATUS_INACTIVE)) {
                continue;
            }
            String referencerUuid = databaseProvider.getId(referencer);
            if (!visitedUuids.add(referencerUuid)) {
                logger.debug("Already visited vertex {}, skipping to avoid cycle", referencerUuid);
                continue;
            }
            ObjectNode referencerNode = constructObject(referencer);
            inlineOwnedReferences(referencer, referencerNode, currLevel + 1, visitedUuids);
            byEntityType.computeIfAbsent(referencer.label(), k -> JsonNodeFactory.instance.arrayNode())
                    .add(referencerNode);
        }
        byEntityType.forEach(node::set);
    }

    private void printUuidNodeMap() {
        uuidNodeMap.keySet().forEach(entry -> {
            logger.debug(entry.toString() + " -> " + uuidNodeMap.get(entry).get(Constants.TYPE_STR_JSON_LD));
        });
    }

    private ArrayNode expandChildObject(ObjectNode entityNode, int currentLevel, Set<String> visitedUuids) {
        if (currentLevel <= Constants.MAX_EXPAND_LEVEL) {
            return expandChildObject(entityNode, null, currentLevel, visitedUuids);
        } else {
            logger.warn("Maximum expand level reached");
            return JsonNodeFactory.instance.arrayNode();
        }
    }

    /**
     * After loading all the associated objects, this function sets the object
     * content in the right paths
     *
     * @param entityNode
     * @param visitedUuids uuids already visited in this top-level read, to guard against graph cycles
     */
    private ArrayNode expandChildObject(ObjectNode entityNode, List<String> processedUUIDs, int currentLevel, Set<String> visitedUuids) {
        ArrayNode resultArr = JsonNodeFactory.instance.arrayNode();
        if (currentLevel <= Constants.MAX_EXPAND_LEVEL) {
            if (entityNode == null) {
                return resultArr;
            }
            List<String> fieldNames = new ArrayList<String>();
            entityNode.fieldNames().forEachRemaining(fieldName -> {
                fieldNames.add(fieldName);
            });

            for (String field : fieldNames) {
                JsonNode entry = entityNode.get(field);
                if (entry != null) {

                    logger.debug("Working on field {}", field);
                    logger.debug("Working on entry {}", entry);

                    if (field.equals(uuidPropertyName)) {
                        // has a uuid that may have been populated
                        // This could be a textual value or an array
                        JsonNode entryValNode = entry;
                        String uuidVal = entryValNode.asText();
                        JsonNode temp = uuidNodeMap.getOrDefault(uuidVal, null);
                        boolean isArray = (temp != null
                                && temp.get(Constants.TYPE_STR_JSON_LD).asText().equals(Constants.ARRAY_NODE_KEYWORD));

                        if (temp == null) {
                            // No node loaded for this.
                            // No action required.
                            entityNode.remove(field);
                        } else if (!isArray) {
                            logger.debug("Field {} Not an array type", field);
                            if (null == processedUUIDs) {
                                processedUUIDs = new ArrayList<>();
                                logger.debug("Provision for expansion");
                            }

                            if (!processedUUIDs.contains(uuidVal) && visitedUuids.add(uuidVal)) {
                                processedUUIDs.add(uuidVal);
                                ObjectNode tmp = uuidNodeMap.get(uuidVal);
                                ArrayNode result = expandChildObject(tmp, processedUUIDs, currentLevel + 1, visitedUuids);
                                entityNode.setAll(tmp);
                            }
                        } else {
                            // Now first query the uuidNodeMap for the list of items
                            JsonNode blankNode = temp;

                            Iterator<Map.Entry<String, JsonNode>> entryIterator = blankNode.fields();
                            while (entryIterator.hasNext()) {
                                Map.Entry<String, JsonNode> item = entryIterator.next();
                                JsonNode ar = item.getValue();
                                for (JsonNode node : ar) {
                                    String uuidPropertyValue;
                                    if (node.isObject()) {
                                        uuidPropertyValue = ArrayHelper.unquoteString(node.get(uuidPropertyName).asText());
                                    } else {
                                        uuidPropertyValue = ArrayHelper.unquoteString(node.asText());
                                    }
                                    ObjectNode ovalue = uuidNodeMap.getOrDefault(uuidPropertyValue, null);
                                    if (visitedUuids.add(uuidPropertyValue)) {
                                        expandChildObject(ovalue, currentLevel + 1, visitedUuids);
                                    }
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
                        ArrayNode expandChildObject = expandChildObject((ObjectNode) entry, currentLevel + 1, visitedUuids);
                        if (expandChildObject.size() == 0 && entityNode.get(field).size() == 0) {
                            entityNode.remove(field);
                        }
                        if (expandChildObject != null && entityNode.get(field).size() == 1) {
                            entityNode.set(field, expandChildObject);
                        }
                    }
                }
            }
        } else {
            logger.warn("Maximum expand level reached");
        }
        return resultArr;
    }



    /**
     * Neo4j supports custom ids and so we can directly query vertex with id - without client side filtering.
     * SqlG does not support custom id, but the result is direct from the database without client side filtering
     *      unlike Neo4j.
     * @param uuidPropertyValue the uuidPropertyValue of vertex to be loaded
     * @return the vertex associated with uuidPropertyValue passed
     */
    public Vertex getVertex(String entityType, String uuidPropertyValue) {
        Vertex vertex = null;
        Iterator<Vertex> itrV = null;
        switch (databaseProvider.getProvider()) {
            case NEO4J:
                itrV = graph.vertices(uuidPropertyValue);
                break;
            case SQLG:
            case CASSANDRA:
            case TINKERGRAPH:
                if (null != entityType) {
                    itrV = graph.traversal().clone().V().hasLabel(entityType).has(uuidPropertyName, uuidPropertyValue);
                } else {
                    itrV = graph.traversal().clone().V().has(uuidPropertyName, uuidPropertyValue);
                }
                break;
            default:
                itrV = graph.vertices(uuidPropertyValue);
                break;
        }

        if (itrV.hasNext()) {
            vertex = itrV.next();
        }

        return vertex;
    }

    /**
     * Returns the root vertex of the entity.
     * @return
     */
    public Vertex getRootVertex() {
        return this.rootVertex;
    }

    /**
     * Given the array node root vertex, returns a set of string of item uuids
     * @param blankArrayVertex
     * @return
     */
    public Set<String> getArrayItemUuids(Vertex blankArrayVertex) {
        String arrayOfType = blankArrayVertex.value(Constants.INTERNAL_TYPE_KEYWORD).toString();
        String propName = RefLabelHelper.getLabel(arrayOfType, uuidPropertyName);
        String allItemUuids = ArrayHelper.removeSquareBraces(blankArrayVertex.value(propName).toString());
        return StringUtils.commaDelimitedListToSet(allItemUuids);
    }

    public String getInternalType(Vertex vertex) {
        String intType = vertex.value(Constants.INTERNAL_TYPE_KEYWORD);
        return intType;
    }

    /**
     * Hits the database to read contents
     * This is the entry function to read contents of a given entity
     * @param uuidPropertyValue
     *            the id to be read
     * @return
     * @throws Exception
     */
    public JsonNode read(String entityType, String uuidPropertyValue) throws Exception {
        rootVertex = getVertex(entityType, uuidPropertyValue);
        return readInternal(rootVertex);
    }

    public JsonNode read(String uuidPropertyValue) throws  Exception {
        rootVertex = getVertex(null, uuidPropertyValue);
        return readInternal(rootVertex);
    }

    public JsonNode readInternal(Vertex rootVertex) throws Exception {
        if (null == rootVertex) {
            throw new RecordNotFoundException("Invalid id");
        }

        int currLevel = 0;
        if (rootVertex.property(Constants.STATUS_KEYWORD).isPresent()
                && rootVertex.property(Constants.STATUS_KEYWORD).value().equals(Constants.STATUS_INACTIVE)) {
            throw new RecordNotFoundException("entity status is inactive");
        }
        ObjectNode rootNode = constructObject(rootVertex);
        String entityType = (String) ValueType.getValue(rootNode.get(TypePropertyHelper.getTypeName()));

        // Set the type for the root node, so as to wrap.
        populateMaps(rootNode, rootVertex);

        ArrayNode signatureNode = loadSignatures(rootVertex);
        if (signatureNode != null) {
            rootNode.set(Constants.SIGNATURES_STR, signatureNode);
        } else {
            rootNode.remove(Constants.SIGNATURES_STR);
        }

        Set<String> visitedUuids = new HashSet<>();
        visitedUuids.add(databaseProvider.getId(rootVertex));

        if (configurator.getDepth() > 0) {
            loadOtherVertices(rootVertex, currLevel, visitedUuids);
        }

        inlineOwnedReferences(rootVertex, rootNode, currLevel, visitedUuids);

        printUuidNodeMap();

        logger.debug("Finished loading information. Start creating the response");

        ObjectNode entityNode = JsonNodeFactory.instance.objectNode();
        // For the entity Node, now go and replace the array values with actual
        // objects.
        // The properties could exist anywhere. Refer to the local arrMap.
        expandChildObject(rootNode, 0, visitedUuids);
        if(expandReferenceObj)
            expandReferenceNodes(rootNode);
        entityNode.set(entityType, rootNode);


        // After reading the entire type, now trim the @type property
        trimAttributes(entityNode);
        return entityNode;
    }

    private void expandReferenceNodes(ObjectNode rootNode) {
        rootNode.fields().forEachRemaining(entry -> {
            // Regex pattern to check for a DID
            String pattern = "^"+ DID_TYPE+":[^:]+:[^:]+";
            if(entry.getValue().isValueNode() && entry.getValue().asText().matches(pattern)) {
                String[] dids = entry.getValue().asText().split(":");
                String uuidPropertyValue = RecordIdentifier.parse(dids[2]).getUuid();
                Iterator<Vertex> vertexIterator = graph.traversal().clone().V().hasLabel(dids[1]).has(uuidPropertyName, uuidPropertyValue);
                while (vertexIterator.hasNext()) {
                    Vertex dependent = vertexIterator.next();
                    ObjectNode references;
                    try {
                        references = (ObjectNode) readInternal(dependent);
                        references = (ObjectNode) references.get(references.fieldNames().next());
                        references = (ObjectNode) JSONUtil.removeNodesByPath(references, definitionsManager.getExcludingFieldsForEntity(dids[1]));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if(references != null) {
                        entry.setValue(references);
                    }
                }
            }
        });
    }

    /**
     * Trims out local helper attributes like the type, uuid depending on the
     * ReadConfigurator
     * @param entityNode
     */
    private void trimAttributes(ObjectNode entityNode) {
        if (!configurator.isIncludeTypeAttributes()) {
            JSONUtil.removeNode(entityNode, Constants.TYPE_STR_JSON_LD);
        }

        if (!configurator.isIncludeIdentifiers()) {
            JSONUtil.removeNode(entityNode, uuidPropertyName);
        }
    }

    /**
     * Returns the map of uuid and vertices read.
     * Using this without executing the read function is not advised.
     * @return
     */
    public HashMap<String, Vertex> getUuidVertexMap() {
        return this.uuidVertexMap;
    }
}
