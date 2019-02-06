package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("registryDao")
public class RegistryDaoImpl implements IRegistryDao {
    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    @Autowired
    EntityParenter entityParenter;

    @Autowired
    private DefinitionsManager definitionsManager;

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
     * Entry point to the dao layer to write a JsonNode entity.
     *
     * @param rootNode
     * @return
     */
    public String addEntity(Graph graph, JsonNode rootNode) {
        VertexWriter vertexWriter = new VertexWriter(graph, shard.getDatabaseProvider(), uuidPropertyName);
        String entityId = vertexWriter.writeNodeEntity(rootNode);
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

        VertexReader vr = new VertexReader(shard.getDatabaseProvider(), graph, readConfigurator, uuidPropertyName, definitionsManager);
        JsonNode result = vr.read(uuid);

        if (!shard.getShardLabel().isEmpty()) {
            // Replace osid with shard details
            String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
            JSONUtil.addPrefix((ObjectNode) result, prefix, new ArrayList<String>(Arrays.asList(uuidPropertyName)));
        }

        return result;
    }


    public JsonNode getEntity(Graph graph, Vertex vertex, ReadConfigurator readConfigurator) {

        VertexReader vr = new VertexReader(shard.getDatabaseProvider(), graph, readConfigurator, uuidPropertyName, definitionsManager);
        JsonNode result = vr.constructObject(vertex);

        if (!shard.getShardLabel().isEmpty()) {
            // Replace osid with shard details
            String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
            JSONUtil.addPrefix((ObjectNode) result, prefix, new ArrayList<String>(Arrays.asList(uuidPropertyName)));
        }

        return result;
    }


    /**
     * This method update the inputJsonNode related vertices in the database
     * This works but a TODO - Re-write required
     * @param rootVertex
     * @param inputJsonNode
     */
    public void updateVertex(Graph graph, Vertex rootVertex, JsonNode inputJsonNode) {
        inputJsonNode.fields().forEachRemaining(subEntityField -> {
            String fieldKey = subEntityField.getKey();
            JsonNode subEntityNode = subEntityField.getValue();
            if (subEntityNode.isValueNode()) {
                rootVertex.property(fieldKey, ValueType.getValue(subEntityField.getValue()));
            } else if (subEntityNode.isObject()) {
                parseJsonObject(subEntityNode, graph, rootVertex, fieldKey, false);
            } else if (subEntityNode.isArray()) {
                Vertex arrayNodeVertex = null;
                if(subEntityNode.get(0).isObject()){
                    if(fieldKey.equalsIgnoreCase(Constants.SIGNATURES_STR)){
                        arrayNodeVertex = rootVertex.vertices(Direction.IN,fieldKey).next();
                    } else {
                        arrayNodeVertex = rootVertex.vertices(Direction.OUT,fieldKey).next();
                    }
                    Set<String> osidSet = new HashSet<String>();
                    for (JsonNode arrayElementNode : subEntityNode) {
                        if (arrayElementNode.isObject()) {
                            String updatedOsid = parseJsonObject(arrayElementNode, graph, arrayNodeVertex, fieldKey, true);
                            osidSet.add(updatedOsid);
                        }
                    }
                    osidSet = deleteVertices(graph, arrayNodeVertex, fieldKey, osidSet);
                    arrayNodeVertex.property(RefLabelHelper.getLabel(fieldKey, uuidPropertyName), osidSet.toString());
                } else {
                    Set<String> valueSet = new HashSet<>();
                    subEntityNode.forEach(textElement -> valueSet.add(textElement.asText()));
                    rootVertex.property(fieldKey, valueSet.toString());
                }

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

            VertexWriter vertexWriter = new VertexWriter(graph, shard.getDatabaseProvider(), uuidPropertyName);

            //Add new vertex
            Vertex newChildVertex = vertexWriter.createVertex(parentNodeLabel);
            newChildVertex.property(Constants.ROOT_KEYWORD,rootVertex.property(Constants.ROOT_KEYWORD).value());
            updateProperties(elementNode, newChildVertex);
            String nodeOsidLabel = RefLabelHelper.getLabel(parentNodeLabel, uuidPropertyName);
            VertexProperty<Object> vertexProperty = rootVertex.property(nodeOsidLabel);
            if (isArrayElement && vertexProperty.isPresent()) {
                String existingValue = ArrayHelper.removeSquareBraces((String) vertexProperty.value());
                existingValue = existingValue + "," + newChildVertex.id().toString();
                String[] newOsidArray = existingValue.split(",");
                rootVertex.property(nodeOsidLabel, ArrayHelper.formatToString(Arrays.asList(newOsidArray)));
            } else {
                rootVertex.property(nodeOsidLabel, newChildVertex.id().toString());
            }
            if(rootVertex.label().equalsIgnoreCase(Constants.ARRAY_NODE_KEYWORD)){
                if(newChildVertex.property(Constants.SIGNATURE_FOR).isPresent()){
                    vertexWriter.addEdge(newChildVertex.property(Constants.SIGNATURE_FOR).value().toString(), rootVertex, newChildVertex);
                } else {
                    vertexWriter.addEdge(parentNodeLabel, rootVertex, newChildVertex);
                }
            }
            return newChildVertex.id().toString();
        } else {
            String shardOsid = elementNode.get(uuidPropertyName).asText();
            RecordIdentifier ri = RecordIdentifier.parse(shardOsid);
            ReadConfigurator configurator = ReadConfiguratorFactory.getOne(false);
            VertexReader vertexReader = new VertexReader(shard.getDatabaseProvider(), graph, configurator,
                    uuidPropertyName, definitionsManager);
            Vertex updateVertex = vertexReader.getVertex(null, ri.getUuid());
            updateProperties(elementNode, updateVertex);
            return updateVertex.id().toString();
        }
    }

    /**
     * updates the vertex properties with given json node elements
     *
     * @param elementNode - the elementNode contains the new values
     * @param vertex - the target vertex where the new values will be copied
     */
    private void updateProperties(JsonNode elementNode, Vertex vertex) {
        elementNode.fields().forEachRemaining(subElementNode -> {
            JsonNode value = subElementNode.getValue();
            String keyType = subElementNode.getKey();
            if (value.isObject()) {

            } else if (value.isValueNode() && !keyType.equals("@type") && !keyType.equals(uuidPropertyName)) {
                vertex.property(keyType, ValueType.getValue(value));
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
    private Set<String> deleteVertices(Graph graph, Vertex rootVertex, String label, Set<String> activeOsid) {
        String[] osidArray = null;
        VertexProperty vp = rootVertex.property(label + "_" + uuidPropertyName);
        String osidPropValue = ArrayHelper.removeSquareBraces((String) vp.value());
        if (osidPropValue.contains(",")) {
            osidArray = osidPropValue.split(",");
        } else {
            osidArray = new String[]{osidPropValue};
        }
        ReadConfigurator configurator = ReadConfiguratorFactory.getOne(true);
        VertexReader vertexReader = new VertexReader(shard.getDatabaseProvider(), graph,
                configurator, uuidPropertyName, definitionsManager);

        for(int itr = 0; itr < osidArray.length; itr++) {
            Vertex deleteVertex = vertexReader.getVertex(null, osidArray[itr]);
            if (activeOsid == null || (activeOsid != null &&
                    !activeOsid.contains(shard.getDatabaseProvider().getId(deleteVertex)) &&
                    deleteVertex.edges(Direction.IN).hasNext())) {
                deleteVertex.property(Constants.STATUS_KEYWORD, Constants.STATUS_INACTIVE);
            } else {
                activeOsid.add(shard.getDatabaseProvider().getId(deleteVertex));
            }
        }
        return activeOsid;
    }

    public void deleteEntity(Vertex vertex) {
        vertex.property(Constants.STATUS_KEYWORD, Constants.STATUS_INACTIVE);
    }
}
