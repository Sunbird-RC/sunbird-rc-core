package io.opensaber.registry.dao.impl;

import java.io.IOException;
import java.util.*;

import io.opensaber.registry.sink.DatabaseProvider;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;

@Component
public class RegistryDaoImpl implements RegistryDao {

    private static Logger logger = LoggerFactory.getLogger(RegistryDaoImpl.class);

	// @Autowired
	// private GraphDBFactory graphDBFactory;

	@Autowired
	private DatabaseProvider databaseProvider;

	@Override
	public List getEntityList() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	@Override
	public boolean addEntity(Graph entity,String label) throws DuplicateRecordException{
		GraphDatabaseService gds = graphDBFactory.getGraphDatabaseService();
		try ( Transaction tx = gds.beginTx() )
		{
			if(gds.findNodes(Label.label(label)).hasNext()){
				tx.success();
				tx.close();
				throw new DuplicateRecordException(Constants.DUPLICATE_RECORD_MESSAGE);
			}

		}
		TinkerGraph graph = (TinkerGraph) entity;
		GraphTraversalSource gts = graph.traversal();
		GraphTraversal<Vertex, Vertex> traversal = gts.V();
		Map<String,List<Object[]>> map = new HashMap<>();

		try ( Transaction tx = gds.beginTx() )
		{
			if(traversal.hasNext()){
				Map<String, Node> createdNodeMap = new HashMap<>();
				Vertex v = traversal.next();
				Node newNode = getNodeWithProperties(gds, v, false, createdNodeMap);

				while(traversal.hasNext()){
					v = traversal.next();
					newNode = getNodeWithProperties(gds, v, true, createdNodeMap);
					Iterator<Edge> outgoingEdges = v.edges(Direction.OUT);
					Iterator<Edge> incomingEdges = v.edges(Direction.IN);
					createEdgeNodes(outgoingEdges, gds, newNode, map, Direction.OUT, v, createdNodeMap);
					createEdgeNodes(incomingEdges, gds, newNode, map, Direction.IN, v, createdNodeMap);

				}
			}
			tx.success();
			tx.close();
		}
		return true;

		
	}
	*/

	@Override
	public boolean addEntity(Graph entity, String label) throws DuplicateRecordException {

		logger.debug("Database Provider features: \n" + databaseProvider.getGraphStore().features());

		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource traversalSource = graphFromStore.traversal();
		if (traversalSource.clone().V().hasLabel(label).hasNext()) {
			throw new DuplicateRecordException(Constants.DUPLICATE_RECORD_MESSAGE);
		}

		TinkerGraph graph = (TinkerGraph) entity;
		GraphTraversalSource gts = graph.traversal();
		GraphTraversal<Vertex, Vertex> traversal = gts.V();
		Map<String, List<Object[]>> map = new HashMap<>();
		
		if (graphFromStore.features().graph().supportsTransactions()){
			org.apache.tinkerpop.gremlin.structure.Transaction tx;
			tx = graphFromStore.tx();
			tx.onReadWrite(org.apache.tinkerpop.gremlin.structure.Transaction.READ_WRITE_BEHAVIOR.AUTO);
			createEdgeNodes(traversalSource, traversal, map);
			tx.commit();
			tx.close();
		} else {
			createEdgeNodes(traversalSource, traversal, map);
		}
		
		return true;
	}

	private void createEdgeNodes(GraphTraversalSource traversalSource, GraphTraversal<Vertex, Vertex> traversal,
			Map<String, List<Object[]>> map) {
		if (traversal.hasNext()) {
			Map<String, Vertex> createdNodeMap = new HashMap<>();
			Vertex v = traversal.next();
			Vertex newVertex = getNodeWithProperties(traversalSource, v, createdNodeMap);

			while (traversal.hasNext()) {
				v = traversal.next();
				newVertex = getNodeWithProperties(traversalSource, v, createdNodeMap);
				Iterator<Edge> outgoingEdges = v.edges(Direction.OUT);
				Iterator<Edge> incomingEdges = v.edges(Direction.IN);
				createEdgeNodes(outgoingEdges, traversalSource, newVertex, map, Direction.OUT, v, createdNodeMap);
				createEdgeNodes(incomingEdges, traversalSource, newVertex, map, Direction.IN, v, createdNodeMap);

			}
		}
	}

	private Vertex getNodeWithProperties(GraphTraversalSource traversal, Vertex v, Map<String, Vertex> createdNodeMap) {

		Vertex newVertex;

		if (createdNodeMap.containsKey(v.label())) {
			newVertex = createdNodeMap.get(v.label());
		} else {
			GraphTraversal nodes = traversal.clone().V().hasLabel(v.label());
			if (nodes.hasNext()) {
				newVertex = (Vertex) nodes.next();
			} else {
				newVertex = traversal.clone().addV(v.label()).next();
			}
			copyProperties(v, newVertex);
			createdNodeMap.put(v.label(), newVertex);
		}
		return newVertex;
	}

	/*
	private Node getNodeWithProperties(GraphDatabaseService gds, Vertex v, boolean dbCheck, Map<String, Node> createdNodeMap) {

		Node newNode;

		if(createdNodeMap.containsKey(v.label())) {
			newNode = createdNodeMap.get(v.label());
		}
		else{
			if(dbCheck){
				ResourceIterator<Node> nodes = gds.findNodes(Label.label(v.label()));
				if(nodes.hasNext()){
					newNode = nodes.next();
				}else{
					newNode = gds.createNode(Label.label(v.label()));
				}
			}
			else{
				newNode = gds.createNode(Label.label(v.label()));
			}
			Iterator<VertexProperty<Object>> properties = v.properties();
			while(properties.hasNext()){
				VertexProperty<Object> property = properties.next();
				newNode.setProperty(property.key(), property.value());
			}
			createdNodeMap.put(v.label(), newNode);
		}
		return newNode;
	}
	*/


	private void createEdgeNodes(Iterator<Edge> edges, GraphTraversalSource traversal,
									Vertex newVertex, Map<String, List<Object[]>> map, Direction direction, Vertex traversalVertex,
									Map<String, Vertex> createdNodeMap) {

		while (edges.hasNext()) {

			Edge edge = edges.next();
			Vertex vertex = direction.equals(Direction.OUT) ? edge.inVertex() : edge.outVertex();
			boolean nodeAndEdgeExists = validateAddVertex(map, traversalVertex, edge, direction);

			if (!nodeAndEdgeExists) {

				Vertex nextVertex = getNodeWithProperties(traversal, vertex, createdNodeMap);

				if (direction.equals(Direction.OUT)) {
					newVertex.addEdge(edge.label(), nextVertex);
				} else {
					nextVertex.addEdge(edge.label(), newVertex);
				}

				addNodeAndEdgeToGraph(map, traversalVertex, vertex, direction);
			}
		}
	}

	/*
	private void createEdgeNodes(Iterator<Edge> edges, GraphDatabaseService gds,
								 Node newNode, Map<String,List<Object[]>> map, Direction direction, Vertex traversalVertex,
                                 Map<String, Node> createdNodeMap) {

		while(edges.hasNext()) {

			Edge edge = edges.next();
			Vertex vertex = direction.equals(Direction.OUT) ? edge.inVertex(): edge.outVertex();
			boolean nodeAndEdgeExists = validateAddVertex(map, traversalVertex, edge, direction);

			if(!nodeAndEdgeExists) {

				Node nextNode = getNodeWithProperties(gds, vertex, true, createdNodeMap);
				RelationshipType relType = RelationshipType.withName(edge.label());

				if(direction.equals(Direction.OUT)) {
					newNode.createRelationshipTo(nextNode, relType);
				} else {
					nextNode.createRelationshipTo(newNode, relType);
				}

				addNodeAndEdgeToGraph(map, traversalVertex, vertex, direction);
			}
		}
	}
	*/

	/**
	 * Method to add created nodes and edges to the graph
	 * @param vertexMap
	 * @param currentVertex
	 * @param newVertex
	 * @param direction
	 */
	private void addNodeAndEdgeToGraph(Map<String, List<Object[]>> vertexMap, Vertex currentVertex,
									   Vertex newVertex, Direction direction) {
		String currentVertexLabel = direction.equals(Direction.OUT) ? newVertex.label() : currentVertex.label();
		String createdVertexLabel = direction.equals(Direction.OUT) ? currentVertex.label() : newVertex.label();
		Object[] edgeArray = {direction, currentVertexLabel};
		List<Object[]> edgeArrayList = vertexMap.getOrDefault(createdVertexLabel, new ArrayList<>());
		edgeArrayList.add(edgeArray);
		vertexMap.put(createdVertexLabel, edgeArrayList);
	}

	/**
	 * Method to validate if the vertex needs to be added to the graph
	 * @param vertexMap
	 * @param currTraversalVertex
	 * @param edge
	 * @param direction
	 * @return
	 */
	private boolean validateAddVertex(Map<String, List<Object[]>> vertexMap, Vertex currTraversalVertex, Edge edge, Direction direction) {
		boolean addVertex = false;

		Vertex edgeVertex = direction.equals(Direction.OUT) ? edge.inVertex() : edge.outVertex();
		String vertexLabel = direction.equals(Direction.OUT) ? currTraversalVertex.label() : edgeVertex.label();
		String edgeLabel = direction.equals(Direction.OUT) ? edgeVertex.label() : currTraversalVertex.label();

		if (vertexMap.containsKey(vertexLabel)) {
			List<Object[]> edgeArrayList = vertexMap.get(vertexLabel);
			for (Object[] edgeArray : edgeArrayList) {
				if (edgeArray.length == 2 && edgeArray[0].equals(direction.opposite()) && edgeArray[1].equals(edgeLabel)) {
					addVertex = true;
					break;
				}
			}
		}
		return addVertex;

	}

	@Override
	public boolean updateEntity(Graph entity,String label) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Graph getEntityById(String label) throws RecordNotFoundException {
		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource traversalSource = graphFromStore.traversal();
		TinkerGraph graph = TinkerGraph.open();
		logger.info("FETCH: "+label);
		GraphTraversal<Vertex, Vertex> hasLabel = traversalSource.clone().V().hasLabel(label);
		Graph parsedGraph = TinkerGraph.open();
		if (!hasLabel.hasNext()) {
			throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
		} else {
			Vertex subject = hasLabel.next();
			Vertex newSubject = parsedGraph.addVertex(subject.label());
			copyProperties(subject, newSubject);
			extractGraphFromVertex(parsedGraph,newSubject,subject);
		}
		return parsedGraph;
	}

	private void copyProperties(Vertex subject, Vertex newSubject) {
		Iterator<VertexProperty<Object>> iter = subject.properties();
		while(iter.hasNext()){
			VertexProperty<Object> property = iter.next();
			newSubject.property(property.key(), property.value());
		}
	}

	@Override
	public boolean deleteEntity(Object entity) {
		// TODO Auto-generated method stub
		return false;
	}

	private void extractGraphFromVertex(Graph parsedGraph,Vertex parsedGraphSubject,Vertex s) {
		Iterator<Edge> edgeIter = s.edges(Direction.OUT);
		Edge edge;
		Stack<Vertex> vStack = new Stack<Vertex>();
		Stack<Vertex> parsedVStack = new Stack<Vertex>();
		while(edgeIter.hasNext()){
			edge = edgeIter.next();
			Vertex o = edge.inVertex();
			Vertex newo = parsedGraph.addVertex(o.label());
			copyProperties(o, newo);
			parsedGraphSubject.addEdge(edge.label(), newo);
			vStack.push(o);
			parsedVStack.push(newo);
			dump_graph(parsedGraph,"outgoing.json");
		}
		Iterator<Vertex> vIterator = vStack.iterator();
		Iterator<Vertex> parsedVIterator = parsedVStack.iterator();
		while(vIterator.hasNext()){
			s = vIterator.next();
			parsedGraphSubject = parsedVIterator.next();
			extractGraphFromVertex(parsedGraph,parsedGraphSubject,s);
		}
	}

	private void dump_graph(Graph parsedGraph, String filename) {
		try {
			parsedGraph.io(IoCore.graphson()).writeGraph(filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
