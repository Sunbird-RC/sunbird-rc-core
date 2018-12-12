package io.opensaber.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.middleware.util.LogMarkers;
import io.opensaber.registry.service.EncryptionService;
import io.opensaber.registry.sink.DatabaseProvider;
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

import java.io.IOException;
import java.util.*;

public class TPGraphMain {
    private static List<String> privatePropertyLst;
    private static List<String> uuidList;
    private static EncryptionService encryptionService;
    private static Map<String, Object> encMap;
    private static Map<String, Object> encodedMap;
    private DatabaseProvider dbProvider;
    private Vertex parentVertex;

    private Logger logger = LoggerFactory.getLogger(TPGraphMain.class);

    private OpenSaberInstrumentation watch = new OpenSaberInstrumentation(true);

    public TPGraphMain(DatabaseProvider db, Vertex parentVertex, List<String> privatePropertyLst, EncryptionService encryptionService) {
        dbProvider = db;
        this.parentVertex = parentVertex;
        this.privatePropertyLst = privatePropertyLst;
        this.encryptionService = encryptionService;
        uuidList = new ArrayList<String>();
    }

    public String createLabel() {
        return UUID.randomUUID().toString();
    }

    public void createVertex(Graph graph, String label, Vertex parentVertex, JsonNode jsonObject) {
        Vertex vertex = graph.addVertex(label);
        jsonObject.fields().forEachRemaining(entry -> {
            JsonNode entryValue = entry.getValue();
            if (entryValue.isValueNode()) {
                if(privatePropertyLst.contains(entry.getKey())) {
                    String encValue = encodedMap.get(entry.getKey()).toString();
                    vertex.property(entry.getKey(), encValue.substring(encValue.lastIndexOf("|")+1));
                } else {
                    vertex.property(entry.getKey(), entryValue.asText());
                }

            } else if (entryValue.isObject()) {
                createVertex(graph, entry.getKey(), vertex, entryValue);
            } else if(entryValue.isArray()){
                entry.getValue().forEach(jsonNode -> {
                    if(jsonNode.isObject()){
                        createVertex(graph, entry.getKey(), vertex, jsonNode);
                    } else {
                        vertex.property(entry.getKey(),entryValue.toString());
                        return;
                    }
                });
            }
        });
        addEdge(graph, label, parentVertex, vertex);
        String edgeId = UUID.randomUUID().toString();
        vertex.property("osid", edgeId);
        parentVertex.property(vertex.label()+ "id", edgeId);
        System.out.println("osid:"+edgeId);
        uuidList.add(edgeId);
    }

    public Edge addEdge(Graph graph, String label, Vertex v1, Vertex v2) {
        return v1.addEdge(label, v2);
    }

    public static Vertex createParentVertex(Graph graph, String parentLabel) {
        GraphTraversalSource gtRootTraversal = graph.traversal();
        GraphTraversal<Vertex, Vertex> rootVertex = gtRootTraversal.V().hasLabel(parentLabel);
        Vertex parentVertex = null;
        if (!rootVertex.hasNext()) {
            parentVertex = graph.addVertex(parentLabel);
            // TODO: this could be parentVertex.id() after we make our own Neo4jIdProvider
            parentVertex.property("osid", parentLabel + "osid");
        } else {
            parentVertex = rootVertex.next();
        }

        return parentVertex;
    }

    public void processNode(Graph graph, String parentName, Vertex parentVertex, JsonNode node) throws EncryptionException {

        Iterator<Map.Entry<String, JsonNode>> entryIterator = node.fields();
        while (entryIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = entryIterator.next();
            if (entry.getValue().isValueNode()) {
                // Create properties
                System.out.println("Create properties within vertex " + parentName);
                System.out.println(parentName + ":" + entry.getKey() + " --> " + entry.getValue());
                parentVertex.property(entry.getKey(), entry.getValue());
            } else if (entry.getValue().isObject()) {
                createVertex(graph, entry.getKey(), parentVertex, entry.getValue());

            } else if (entry.getValue().isArray()) {
                // TODO
                    entry.getValue().forEach(jsonNode -> {
                        System.out.println(jsonNode);
                        if(jsonNode.isObject()){
                            createVertex(graph, entry.getKey(), parentVertex, jsonNode);
                        } else {
                            parentVertex.property(entry.getKey(),jsonNode);
                            return;
                        }
                    });

            }
        }
    }

    // TODO: This method must exist outside in an EncryptionHelper.
    public JsonNode createEncryptedJson(String jsonString) throws IOException, EncryptionException {
        encMap = new HashMap<String, Object>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonString);
        if(rootNode.isObject()){
            populateObject(rootNode);
        }
        encodedMap = (Map<String, Object>)encryptionService.encrypt(encMap);
        return rootNode;
    }

    private void populateObject(JsonNode rootNode) {
        rootNode.fields().forEachRemaining(entry -> {
            JsonNode entryValue = entry.getValue();
            if(entryValue.isValueNode()) {
                if(privatePropertyLst.contains(entry.getKey()))
                  encMap.put(entry.getKey(),entryValue.asText());
            } else if(entryValue.isObject()) {
                populateObject(entryValue);
            }
        });
    }

    public Map readGraph2Json(String osid) throws IOException, Exception {
        Map map = new HashMap();
        Graph graph = dbProvider.getGraphStore();
        Transaction tx = graph.tx();
        Neo4JGraph neo4JGraph = dbProvider.getRawGraph();
        StatementResult sr = neo4JGraph.execute("match (n) where n.osid='" + osid + "' return n");
        while(sr.hasNext()){
            Record record = sr.single();
            InternalNode internalNode = (InternalNode) record.get("n").asNode();
            String label = internalNode.labels().iterator().next();
            map.put(label,internalNode.asValue().asMap());
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

    public static enum DBTYPE {NEO4J, POSTGRES};


    // Expectation
    // Only one Grouping vertex = "teachers"  (plural of your parent vertex)
    // Multiple Parent vertex = teacher
    // Multiple child vertex = address
    // For every parent vertex and child vertex, there is a single Edge between
    //    teacher -> address
    public void createTPGraph(JsonNode rootNode) throws IOException, EncryptionException, Exception {
        try {
            Graph graph = dbProvider.getGraphStore();

            watch.start("Add Transaction");
            try (Transaction tx = graph.tx()) {
                processNode(graph, "Teacher", parentVertex, rootNode);
                tx.commit();
            }
            new Thread(() -> {
                try {
                    graph.close();
                } catch (Exception e) {
                    logger.error("Can't close the graph");
                }
            }).start();
            watch.stop("Add Transaction");
        } catch (Exception e) {
            logger.error(LogMarkers.FATAL, "Can't close graph");
        }
    }
}
