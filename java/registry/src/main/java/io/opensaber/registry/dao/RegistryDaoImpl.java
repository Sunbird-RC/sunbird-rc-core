package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.schema.configurator.ISchemaConfigurator;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.EntityParenter;
import io.opensaber.registry.util.ReadConfigurator;
import io.opensaber.registry.util.RecordIdentifier;
import io.opensaber.registry.util.RefLabelHelper;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("tpGraphMain")
public class RegistryDaoImpl implements IRegistryDao {
    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    @Autowired
    EntityParenter entityParenter;

    @Autowired
    private ISchemaConfigurator schemaConfigurator;

    @Autowired
    private Shard shard;

    private List<String> privatePropertyList;

    private Logger logger = LoggerFactory.getLogger(RegistryDaoImpl.class);

    private OpenSaberInstrumentation watch = new OpenSaberInstrumentation(true);

    public List<String> getPrivatePropertyList() {
        return privatePropertyList;
    }

    public void setPrivatePropertyList(List<String> privatePropertyList) {
        this.privatePropertyList = privatePropertyList;
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
            VertexWriter vertexWriter = new VertexWriter(uuidPropertyName, shard);
            parentVertex = vertexWriter.createVertex(graph, parentLabel);
            logger.info("Parent label {} created {}", parentLabel, parentVertex.id().toString());
        } else {
            parentVertex = iterVertex.next();
            logger.info("Parent label {} already existing {}", parentLabel, parentVertex.id().toString());
        }

        return parentVertex;
    }

    /**
     * Retrieves all vertex UUID for given all labels.
     */
    public List<String> getUUIDs(Graph graph, Set<String> labels) {
        List<String> uuids = new ArrayList<>();
        // Temporarily adding all the vertex ids.
        //TODO: get graph traversal by passed labels
        GraphTraversal<Vertex, Vertex> graphTraversal = graph.traversal().V();
        while (graphTraversal.hasNext()) {
            Vertex v = graphTraversal.next();
            uuids.add(v.id().toString());
            logger.debug("vertex info- label :" + v.label() + " id: " + v.id());
        }
        return uuids;
    }

    /**
     * Entry point to the dao layer to write a JsonNode entity.
     *
     * @param rootNode
     * @return
     */
    public String addEntity(Graph graph, JsonNode rootNode) {
        VertexWriter vertexWriter = new VertexWriter(uuidPropertyName, shard);
        String entityId = vertexWriter.writeNodeEntity(graph, rootNode);
        return entityId;
    }

    /**
     * Retrieves a record from the database
     *
     * @param uuid             entity identifier to retrieve
     * @param readConfigurator
     * @return
     */
    public JsonNode getEntity(Graph graph, String uuid, ReadConfigurator readConfigurator) throws Exception {
        if (null == privatePropertyList) {
            privatePropertyList = new ArrayList<>();
            setPrivatePropertyList(schemaConfigurator.getAllPrivateProperties());
        }

        VertexReader vr = new VertexReader(graph, readConfigurator, uuidPropertyName, privatePropertyList);
        JsonNode result = vr.read(uuid);

        // Replace osid with shard details
        String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
        JSONUtil.addPrefix((ObjectNode) result, prefix, new ArrayList<String>(Arrays.asList(uuidPropertyName)));

        return result;
    }


    public JsonNode getEntity(Graph graph, Vertex vertex, ReadConfigurator readConfigurator) {
        if (null == privatePropertyList) {
            privatePropertyList = new ArrayList<>();
            setPrivatePropertyList(schemaConfigurator.getAllPrivateProperties());
        }
        VertexReader vr = new VertexReader(graph, readConfigurator, uuidPropertyName, privatePropertyList);
        JsonNode result = vr.constructObject(vertex);

        // Replace osid with shard details
        String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
        JSONUtil.addPrefix((ObjectNode) result, prefix, new ArrayList<String>(Arrays.asList(uuidPropertyName)));

        return result;
    }


    /**
     * This method update the inputJsonNode related vertices in the database
     *
     * @param rootVertex
     * @param inputJsonNode
     */
    public void updateVertex(Graph graph, Vertex rootVertex, JsonNode inputJsonNode) {
        inputJsonNode.fields().forEachRemaining(subEntityField -> {
            String fieldKey = subEntityField.getKey();
            JsonNode subEntityNode = subEntityField.getValue();
            if (subEntityNode.isValueNode()) {
                rootVertex.property(fieldKey, subEntityField.getValue().asText());
            } else if (subEntityNode.isObject()) {
                parseJsonObject(subEntityNode, graph, rootVertex, fieldKey, false);
            } else if (subEntityNode.isArray()) {
                List<String> osidList = new ArrayList<String>();
                subEntityNode.forEach(arrayElementNode -> {
                    if (arrayElementNode.isObject()) {
                        String updatedOsid = parseJsonObject(arrayElementNode, graph, rootVertex, fieldKey, true);
                        osidList.add(updatedOsid);
                    }
                });
                deleteVertices(graph, rootVertex, fieldKey, osidList);
                String updatedOisdValue = String.join(",", osidList);
                rootVertex.property(RefLabelHelper.getLabel(fieldKey, uuidPropertyName), updatedOisdValue);
            }
        });

    }


    /**
     * Parse the json data to vertex properties, creates new vertex if the node is new else updates the existing vertex
     *
     * @param elementNode
     * @param graph
     * @param rootVertex
     * @param parentNodeLabel
     * @param isArrayElement
     * @return
     */
    private String parseJsonObject(JsonNode elementNode, Graph graph, Vertex rootVertex, String parentNodeLabel,
                                   boolean isArrayElement) {
        if (null == elementNode.get(uuidPropertyName)) {
            if (!isArrayElement) {
                deleteVertices(graph, rootVertex, parentNodeLabel, null);
            }

            VertexWriter vertexWriter = new VertexWriter(uuidPropertyName, shard);

            //Add new vertex
            Vertex newChildVertex = vertexWriter.createVertex(graph, parentNodeLabel);
            updateProperties(elementNode, newChildVertex);
            String nodeOsidLabel = RefLabelHelper.getLabel(parentNodeLabel, uuidPropertyName);
            VertexProperty<Object> vertexProperty = rootVertex.property(nodeOsidLabel);
            if (isArrayElement && vertexProperty.isPresent()) {
                String existingValue = (String) vertexProperty.value();
                rootVertex.property(nodeOsidLabel, existingValue + "," + newChildVertex.id().toString());
            } else {
                rootVertex.property(nodeOsidLabel, newChildVertex.id().toString());
            }
            vertexWriter.addEdge(parentNodeLabel, rootVertex, newChildVertex);
            return newChildVertex.id().toString();
        } else {
            Vertex updateVertex = graph.vertices(elementNode.get(uuidPropertyName).asText()).next();
            updateProperties(elementNode, updateVertex);
            return updateVertex.id().toString();
        }
    }

    /**
     * updates the vertex properties with given json node elements
     *
     * @param elementNode
     * @param vertex
     */
    private void updateProperties(JsonNode elementNode, Vertex vertex) {
        elementNode.fields().forEachRemaining(subElementNode -> {
            JsonNode value = subElementNode.getValue();
            String keyType = subElementNode.getKey();
            if (value.isObject()) {

            } else if (value.isValueNode() && !keyType.equals("@type") && !keyType.equals(uuidPropertyName)) {
                vertex.property(keyType, value.asText());
            }
        });
    }

    /**
     * This method is called while updating the entity. If any non-necessary vertex is there, it will be removed from the database
     * TO-DO need to do soft delete
     *
     * @param graph
     * @param rootVertex
     * @param label
     * @param activeOsid
     */
    private void deleteVertices(Graph graph, Vertex rootVertex, String label, List<String> activeOsid) {
        String[] osidArray = null;
        VertexProperty vp = rootVertex.property(label + "_" + uuidPropertyName);
        String osidPropValue = (String) vp.value();
        if (osidPropValue.contains(",")) {
            osidArray = osidPropValue.split(",");
        } else {
            osidArray = new String[]{osidPropValue};
        }
        Iterator<Vertex> vertices = graph.vertices(osidArray);
        //deleting existing vertices
        vertices.forEachRemaining(deleteVertex -> {
            if (activeOsid == null || (activeOsid != null && !activeOsid.contains(deleteVertex.id()))) {
                deleteVertex.property(Constants.STATUS_KEYWORD, Constants.STATUS_INACTIVE);
                Edge edge = deleteVertex.edges(Direction.IN, label).next();
                edge.remove();
                //deleteVertex.edges(Direction.IN,label).next().remove();
                //deleteVertex.remove();
                //addEdge(label,deleteVertex,rootVertex);

            }
        });
    }
}
