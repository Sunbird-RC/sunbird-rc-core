package dev.sunbirdrc.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.sink.DatabaseProvider;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.registry.util.ReadConfigurator;
import dev.sunbirdrc.registry.util.TypePropertyHelper;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RegistryDaoImpl implements IRegistryDao {
    public String uuidPropertyName;
    private IDefinitionsManager definitionsManager;
    private DatabaseProvider databaseProvider;
    private List<String> privatePropertyList;

    private Logger logger = LoggerFactory.getLogger(RegistryDaoImpl.class);

    private SunbirdRCInstrumentation watch = new SunbirdRCInstrumentation(true);

    public List<String> getPrivatePropertyList() {
        return privatePropertyList;
    }

    public void setPrivatePropertyList(List<String> privatePropertyList) {
        this.privatePropertyList = privatePropertyList;
    }

    public RegistryDaoImpl(DatabaseProvider dbProvider, IDefinitionsManager defnManager, String uuidPropName) {
        databaseProvider = dbProvider;
        definitionsManager = defnManager;
        uuidPropertyName = uuidPropName;
    }

    public DatabaseProvider getDatabaseProvider() {
        return this.databaseProvider;
    }

    /**
     * Entry point to the dao layer to write a JsonNode entity.
     *
     * @param rootNode
     * @return
     */
    public String addEntity(Graph graph, JsonNode rootNode) {
        VertexWriter vertexWriter = new VertexWriter(graph, getDatabaseProvider(), uuidPropertyName);
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
    public JsonNode getEntity(Graph graph, String entityType, String uuid, ReadConfigurator readConfigurator) throws Exception {

        VertexReader vr = new VertexReader(getDatabaseProvider(), graph, readConfigurator, uuidPropertyName, definitionsManager);
        JsonNode result = vr.read(entityType, uuid);

        return result;
    }


    public JsonNode getEntity(Graph graph, Vertex vertex, ReadConfigurator readConfigurator, boolean expandInternal) throws Exception {

        VertexReader vr = new VertexReader(getDatabaseProvider(), graph, readConfigurator, uuidPropertyName, definitionsManager);
        ObjectNode constructObject = vr.constructObject(vertex);
        if (expandInternal) {
            String entityType = (String) ValueType.getValue(constructObject.get(TypePropertyHelper.getTypeName()));
            return vr.readInternal(vertex).get(entityType);
        } else {
            return constructObject;
        }
    }


    /**
     * This method update the inputJsonNode related vertices in the database
     * Notes:
     * This graph object is the same one used for reading the entire record
     *  @param vertex
     * @param inputJsonNode
     * @param parentName
     */
    public void updateVertex(Graph graph, Vertex vertex, JsonNode inputJsonNode, String parentName) {
        if (inputJsonNode.isObject()) {
            String objectName = inputJsonNode.fields().next().getKey();
            logger.debug("Going to update objectName {}", objectName);
            JsonNode osidJsonNode = inputJsonNode.get(uuidPropertyName);
            String osidVal = "";
            if (osidJsonNode != null) {
                osidVal = osidJsonNode.textValue();
            }
            if (databaseProvider.getId(vertex).equals(osidVal)) {
                updateObject(graph, vertex, (ObjectNode) inputJsonNode);
            } else {
                VertexWriter vertexWriter = new VertexWriter(graph, getDatabaseProvider(), uuidPropertyName);
                if(inputJsonNode.get(uuidPropertyName) != null) {
                    vertexWriter.writeSingleNode(vertex, objectName, inputJsonNode.get(objectName));
                } else {
                    // Set parent name as label for new node.
                    vertexWriter.writeSingleNode(vertex, parentName, inputJsonNode);
                }
            }
        } else {
            logger.error("Unexpected input passed here.");
        }
    }



    private void updateObject(Graph graph, Vertex vertex, ObjectNode inputJsonNode) {
        inputJsonNode.fields().forEachRemaining(field -> {
            JsonNode fieldValue = field.getValue();
            String fieldKey = field.getKey();
            if (!fieldKey.equals(uuidPropertyName) &&
                    fieldValue.isValueNode() && !fieldKey.equals(Constants.TYPE_STR_JSON_LD)) {
                if (!fieldValue.isNull()) {
                    vertex.property(fieldKey, ValueType.getValue(fieldValue));
                }
            } else {
                logger.debug("Not updating non-value object types here");
            }
        });
    }


    public void deleteEntity(Vertex vertex) {
        if (null != vertex) {
            vertex.property(Constants.STATUS_KEYWORD, Constants.STATUS_INACTIVE);
            logger.debug("Vertex {} {} marked deleted", vertex.label(), databaseProvider.getId(vertex));
        } else {
            logger.error("Can't mark delete - Null vertex passed");
        }
    }
}
