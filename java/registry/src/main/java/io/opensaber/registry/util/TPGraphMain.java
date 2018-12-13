package io.opensaber.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.sink.DatabaseProvider;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.*;

public class TPGraphMain {
    private DatabaseProvider dbProvider;
    private String parentVertexUuid;

    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName = "testuuid";

    private Logger logger = LoggerFactory.getLogger(TPGraphMain.class);

    private OpenSaberInstrumentation watch = new OpenSaberInstrumentation(true);

    public TPGraphMain() {
    }

    public TPGraphMain(DatabaseProvider db, String parentVertexUuid) {
        dbProvider = db;
        this.parentVertexUuid = parentVertexUuid;
    }

    public Vertex createVertex(Graph graph, String label, Vertex parentVertex, JsonNode jsonObject) {
        Vertex vertex = graph.addVertex(label);
        jsonObject.fields().forEachRemaining(entry -> {
            JsonNode entryValue = entry.getValue();
            logger.debug("Processing {} -> {}", entry.getKey(), entry.getValue());

            if (entryValue.isValueNode()) {
                vertex.property(entry.getKey(), entryValue.asText());
            } else if (entryValue.isObject()) {
                Vertex v = createVertex(graph, entry.getKey(), vertex, entryValue);
                addEdge(entry.getKey(), vertex, v);
                vertex.property(entry.getKey() + uuidPropertyName, v.id());
                logger.debug("Added edge between {} and {}", vertex.label(), v.label());
            } else if (entryValue.isArray()) {
                List<String> arrayEntries = new ArrayList<>();
                entry.getValue().forEach(jsonNode -> {
                    if (jsonNode.isObject()) {
                        Vertex createdV = createVertex(graph, entry.getKey(), vertex, jsonNode);
                        arrayEntries.add(createdV.id().toString());
                        addEdge(entry.getKey(), vertex, createdV);
                    } else {
                        arrayEntries.add(entryValue.toString());
                    }
                });

                if (!arrayEntries.isEmpty()) {
                    String vIds = "[";
                    for (String oneV : arrayEntries) {
                        vIds = vIds + "\"" + oneV + "\",";
                    }
                    vIds = vIds.substring(0, vIds.length() - 1);
                    vIds += ']';

                    vertex.property(entry.getKey() + uuidPropertyName, vIds);
                }
            }
        });
        return vertex;
    }

    public Edge addEdge(String label, Vertex v1, Vertex v2) {
        return v1.addEdge(label, v2);
    }

    public Vertex createParentVertex(Graph graph, String parentLabel) {
        Vertex parentVertex = null;
        GraphTraversalSource gtRootTraversal = graph.traversal();
        GraphTraversal<Vertex, Vertex> rootVertex = gtRootTraversal.clone().V().hasLabel(parentLabel);
        if (!rootVertex.hasNext()) {
            parentVertex = graph.addVertex(parentLabel);
            parentVertex.property(uuidPropertyName, parentVertex.id().toString());
        } else {
            parentVertex = rootVertex.next();
        }

        return parentVertex;
    }

    public String processEntity(Graph graph, String parentName, String parentVertexUuid, JsonNode node) {
        Iterator<Vertex> vertexIterator = graph.vertices(parentVertexUuid);
        if (!vertexIterator.hasNext()) {
            throw new RuntimeException("Seems the uuid is unknown.");
        }

        Vertex resultVertex = null;
        Vertex parentVertex = vertexIterator.next();
        Iterator<Map.Entry<String, JsonNode>> entryIterator = node.fields();
        while (entryIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = entryIterator.next();
            logger.info("Processing {} -> {}", entry.getKey(), entry.getValue());

            if (entry.getValue().isValueNode()) {
                // Create properties
                logger.debug("Create properties within vertex {}", parentName);
                logger.debug(parentName + ":" + entry.getKey() + " --> " + entry.getValue());
                parentVertex.property(entry.getKey(), entry.getValue());
            } else if (entry.getValue().isObject()) {
                resultVertex = createVertex(graph, entry.getKey(), parentVertex, entry.getValue());
                Edge edge = addEdge(entry.getKey(), parentVertex, resultVertex);
                parentVertex.property(resultVertex.label() + uuidPropertyName, resultVertex.id());
            }
        }

        return resultVertex.id().toString();
    }

    /**
     * Retrieves all UUID of a given all labels.
     */
    public List<String> getUUIDs(List<String> parentLabels, DatabaseProvider dbProvider) {
        List<String> uuids = new ArrayList<>();
        Graph graph = dbProvider.getGraphStore();
        GraphTraversal<Vertex, Vertex> graphTraversal = graph.traversal().V();
        for (String label : parentLabels) {
            GraphTraversal<Vertex, Vertex> gvs = graphTraversal.hasLabel(label);
            Vertex v = gvs.hasNext() ? gvs.next() : null;
            if (v != null) {
                uuids.add(v.property(uuidPropertyName).value().toString());
            }
        }
        return uuids;
    }

    public Map readGraph2Json_neo4j(String osid) throws IOException, Exception {
        Map map = new HashMap();
        Graph graph = dbProvider.getGraphStore();
        Transaction tx = graph.tx();
        Neo4JGraph neo4JGraph = dbProvider.getRawGraph();
        StatementResult sr = neo4JGraph.execute("match (n) where n.osid='" + osid + "' return n");
        while (sr.hasNext()) {
            Record record = sr.single();
            InternalNode internalNode = (InternalNode) record.get("n").asNode();
            String label = internalNode.labels().iterator().next();
            map.put(label, internalNode.asValue().asMap());
            //To find connected nodes of the osid node
            /*if(label.equalsIgnoreCase("Teacher")){
                StatementResult sr1 = neo4JGraph.execute("match (s)-[e]->(v) where ID(s) =" + internalNode.id() + " return v");
                List<Record> record1 = sr1.list();
                record1.forEach(node1 -> {
                    InternalNode connectedNode = (InternalNode)node1.get("v").asNode();
                    String connectedLabel = connectedNode.labels().iterator().next();
                    map1.put(connectedLabel,connectedNode.asValue().asMap());
                });
                System.out.println("done");
                *//*record1.stream().forEach(node1 -> {
                    System.out.println((InternalNode)node1.values());
                });*//*
            }*/
        }
        // StatementResult sr = graph.execute("match (n) where n.osid='532e2c1a-1ed0-4b29-8e44-2d5f6f3d711f' return n");
        // System.out.println(sr.hasNext());
        // GraphTraversal<Vertex, Vertex>  gt = gtRootTraversal.clone().V(679077);
        //GraphTraversal<Vertex, Vertex>  gt = gtRootTraversal.clone().V(679077).hasLabel("Teacher");
        //gt.hasNext();
        tx.commit();
        graph.close();
        return map;
    }

    public JsonNode readGraph2Json(Graph graph, String osid) {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        try (Transaction tx = dbProvider.startTransaction(graph)) {
            System.out.println(graph.vertices(osid).hasNext());

            Iterator<Vertex> itrV = graph.vertices(osid);
            if (itrV.hasNext()) {
                Vertex currVertex = itrV.next();
                currVertex.properties().forEachRemaining(prop -> {
                    String propertyName = prop.label();
                    int uuidPropertyIdx = propertyName.indexOf(uuidPropertyName);
                    if (propertyName.compareTo(uuidPropertyName) != 0 &&
                            uuidPropertyIdx != -1) {
                        // This is a child entity. Go retrieve that
                        String childEntityName = propertyName.substring(0, uuidPropertyIdx);
                        Object valueObj = prop.value();
                        if (valueObj.getClass().isArray()) {
                            objectNode.put(childEntityName, "TODO array");
                        } else {
                            objectNode.set(childEntityName, readGraph2Json(graph, prop.value().toString()));
                        }
                    } else {
                        objectNode.put(prop.label(), prop.value().toString());
                    }
                });
            }
            dbProvider.commitTransaction(graph, tx);
        }

        return objectNode;
    }

    public static enum DBTYPE {NEO4J, POSTGRES}

    // Expectation
    // Only one Grouping vertex = "teachers"  (plural of your parent vertex)
    // Multiple Parent vertex = teacher
    // Multiple child vertex = address
    // For every parent vertex and child vertex, there is a single Edge between
    //    teacher -> address
    public String createTPGraph(JsonNode rootNode) throws IOException, EncryptionException, Exception {
        Graph graph = dbProvider.getGraphStore();

        watch.start("Add Transaction");
        Transaction tx = dbProvider.startTransaction(graph);
        // TODO:
        // Could the parentName be derived from rootNode itself?
        String entityId = processEntity(graph, "Teacher", parentVertexUuid, rootNode);
        dbProvider.commitTransaction(graph, tx);

        // Closing the thread async
        new Thread(() -> {
            try {
                graph.close();
            } catch (Exception e) {
                logger.error("Can't close the graph");
            }
        }).start();
        watch.stop("Add Transaction");
        return entityId;
    }
}
