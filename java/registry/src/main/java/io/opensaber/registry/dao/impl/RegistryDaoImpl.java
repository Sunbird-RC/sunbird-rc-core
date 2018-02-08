package io.opensaber.registry.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.util.GraphDBFactory;

public class RegistryDaoImpl implements RegistryDao {

	@Override
	public List getEntityList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addEntity(Object entity,String label){
		GraphDatabaseService gds = null;
		try{
			gds = GraphDBFactory.getGraphDatabaseService();
			try ( Transaction tx = gds.beginTx() )
			{
				if(gds.findNodes(Label.label(label)).hasNext()){
					tx.success();
					tx.close();
					throw new Exception();
				}

			}
			TinkerGraph graph = (TinkerGraph)entity;
			GraphTraversalSource gts = graph.traversal();
			GraphTraversal<Vertex, Vertex> traversal = gts.V();
			Map<String,List<Object[]>> map = new HashMap<String,List<Object[]>>();
			try ( Transaction tx = gds.beginTx() )
			{
				if(traversal.hasNext()){
					Map<String,Node> createdNodeMap = new HashMap<String,Node>();
					Vertex v = traversal.next();
					System.out.println("Printing label:"+v.label());
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
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

	private Node getNodeWithProperties(GraphDatabaseService gds,Vertex v, boolean dbCheck, Map<String,Node> createdNodeMap){

		Node newNode;

		if(createdNodeMap.containsKey(v.label())){
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


	private void createEdgeNodes(Iterator<Edge> edges, GraphDatabaseService gds, Node newNode, Map<String,List<Object[]>> map, Direction direction, Vertex v, Map<String,Node> createdNodeMap){

		while(edges.hasNext()){
			Vertex vertex = null;
			Edge edge = edges.next();
			boolean continueFlag = false;
			if(direction.equals(Direction.OUT)){
				vertex = edge.inVertex();
				if(map.containsKey(v.label())){
					List<Object[]> edgeArrayList = map.get(v.label());
					for(Object[] edgeArray: edgeArrayList){
						if(edgeArray.length==2 && edgeArray[0].equals(direction.opposite()) && edgeArray[1].equals(vertex.label()) ){
							continueFlag = true;
							break;
						}
					}
				}

			}else{
				vertex = edge.outVertex();
				if(map.containsKey(vertex.label())){
					List<Object[]> edgeArrayList = map.get(vertex.label());
					for(Object[] edgeArray: edgeArrayList){
						if(edgeArray.length==2 && edgeArray[0].equals(direction.opposite()) && edgeArray[1].equals(v.label()) ){
							continueFlag = true;
							break;
						}
					}

				}
			}

			if(continueFlag){
				continue;
			}
			Node nextNode = getNodeWithProperties(gds, vertex, true, createdNodeMap);
			RelationshipType relType = RelationshipType.withName(edge.label());
			if(direction.equals(Direction.OUT)){
				newNode.createRelationshipTo(nextNode, relType);
				Object[] edgeArray = {direction,vertex.label()};
				List<Object[]> edgeArrayList = null;
				if(map.containsKey(v.label())){
				edgeArrayList = map.get(v.label());
				}else{
					edgeArrayList = new ArrayList<Object[]>();
				}
				edgeArrayList.add(edgeArray);
				map.put(v.label(), edgeArrayList);

			}else{
				nextNode.createRelationshipTo(newNode, relType);
				Object[] edgeArray = {direction,v.label()};
				List<Object[]> edgeArrayList = null;
				if(map.containsKey(vertex.label())){
				edgeArrayList = map.get(vertex.label());
				}else{
					edgeArrayList = new ArrayList<Object[]>();
				}
				edgeArrayList.add(edgeArray);
				map.put(vertex.label(), edgeArrayList);
			}
		}
	}

	@Override
	public boolean updateEntity(Object entity) {
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
