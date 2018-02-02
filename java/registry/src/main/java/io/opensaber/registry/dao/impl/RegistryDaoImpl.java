package io.opensaber.registry.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.datastax.driver.core.querybuilder.Assignment;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.querybuilder.Update;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import io.opensaber.registry.config.CassandraConfiguration;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.model.Teacher;
import io.opensaber.registry.model.dto.EntityDto;
import io.opensaber.registry.util.GraphDBFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

/**
 * 
 * @author jyotsna
 *
 */
@Service
public class RegistryDaoImpl implements RegistryDao{

	@Autowired
	CassandraConfiguration cassandraConfig;

	@Autowired
	ObjectMapper objectMapper;

	@Override
	public List getEntityList(){
		List<Teacher> teacherList = new ArrayList<Teacher>();
		try{
			String query1 = "select userid from user_job_profile where jobname in ('professor') allow filtering";
			List<String> userIdList = cassandraConfig.cassandraTemplate().select(query1,String.class);
			Select selectQuery = QueryBuilder.select().column("id")
					.column("dob").column("email").column("avatar")
					.column("emailverified").column("firstname").column("lastname")
					.column("gender").column("language").column("grade").from("user").allowFiltering();
			Where selectWhere = selectQuery.where();
			Clause whereClause = QueryBuilder.in("id", userIdList);
			selectWhere.and(whereClause);
			teacherList = cassandraConfig.cassandraTemplate().select(selectQuery,Teacher.class);
			return teacherList;
		}catch(Exception e){
			e.printStackTrace();
		}
		return teacherList;
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
	public boolean updateEntity(Object entity){
		try{
			Map<String,Object> insertValues = objectMapper.convertValue(entity,  new TypeReference<HashMap<String, Object>>() {});
			Update updateQuery = QueryBuilder.update("user");
			Clause whereClause = QueryBuilder.eq("id", (String)insertValues.get("id"));
			Update.Where whereQuery = updateQuery.where();
			whereQuery.and(whereClause);
			insertValues.remove("id");
			for(Map.Entry<String, Object> entry : insertValues.entrySet()){
				Assignment assignment = QueryBuilder.set(entry.getKey(), entry.getValue());
				updateQuery.with(assignment);
			}
			cassandraConfig.cassandraTemplate().execute(updateQuery);
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Object getEntityById(Object entity){
		try{
			/*EntityDto entityDto = (EntityDto)entity;
			Select selectQuery = QueryBuilder.select().column("id")
					.column("dob").column("email").column("avatar")
					.column("emailverified").column("firstname").column("lastname")
					.column("gender").column("language").column("grade").from("user").allowFiltering();
			Where selectWhere = selectQuery.where();
			Clause whereClause = QueryBuilder.eq("id", entityDto.getId());
			selectWhere.and(whereClause);
			return cassandraConfig.cassandraTemplate().selectOne(selectQuery,Teacher.class);*/


		}catch(Exception e){
			e.printStackTrace();
		}
		return new Teacher();
	}

	@Override
	public boolean deleteEntity(Object entity){
		try{
			EntityDto entityDto = (EntityDto)entity;
			Delete deleteQuery = QueryBuilder.delete().all().from("user");
			Delete.Where whereQuery = deleteQuery.where();
			Clause whereClause = QueryBuilder.eq("id", entityDto.getId());
			whereQuery.and(whereClause);
			cassandraConfig.cassandraTemplate().execute(deleteQuery);
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}		
		return false;
	}

}
