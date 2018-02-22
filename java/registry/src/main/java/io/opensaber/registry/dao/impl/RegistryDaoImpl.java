package io.opensaber.registry.dao.impl;

import java.util.*;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.util.GraphDBFactory;

@Component
public class RegistryDaoImpl implements RegistryDao {

    private static Logger logger = LoggerFactory.getLogger(RegistryDaoImpl.class);

	@Autowired
	private GraphDBFactory graphDBFactory;

	@Override
	public List getEntityList() {
		// TODO Auto-generated method stub
		return null;
	}

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
		TinkerGraph graph = (TinkerGraph)entity;
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
	public Object getEntityById(Object entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteEntity(Object entity) {
		// TODO Auto-generated method stub
		return false;
	}

}
