package io.opensaber.registry.dao.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.tinkerpop.api.Neo4jNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jEdge;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jVertex;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import io.opensaber.registry.config.CassandraConfiguration;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.model.dto.EntityDto;
import io.opensaber.registry.util.GraphDBFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;




@RunWith(SpringRunner.class)
@SpringBootTest(classes={RegistryDaoImpl.class,CassandraConfiguration.class
		,Environment.class,ObjectMapper.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RegistryDaoImplTest {

	@Autowired
	RegistryDao registryDao;

	private static TinkerGraph graph;

	private static String identifier;

	@Before
	public void initializeGraph(){
		graph = TinkerGraph.open();
		
	}

	@Test
	public void test_add_entity_with_single_node(){
		identifier = generateRandomId();
		String label = "teacher";
		getVertexForSubject(label, "identifier", identifier);
		getVertexForSubject(label, "type", "Teacher");
		boolean response = registryDao.addEntity(graph,label);
		assertTrue(response);

	}
	
	@Test
	public void test_add_existing_entity(){
		Vertex vertex = null;
		String label = "teacher";
		getVertexForSubject(label, "identifier", identifier);
		getVertexForSubject(label, "type", "Teacher");
		boolean response = registryDao.addEntity(graph,label);
		assertFalse(response);

	}
	
	@Test
	public void test_add_with_multiple_nodes(){
		identifier = generateRandomId();
		Vertex firstVertex = null;
		Vertex secondVertex = null;
		Vertex thirdVertex = null;
		String firstVertexLabel = "teacher1";
		String secondVertexLabel = "school";
		String thirdVertexLabel = "address";
		getVertexForSubject(firstVertexLabel, "identifier", identifier);
		firstVertex = getVertexForSubject(firstVertexLabel, "type", "Teacher");
		getVertexForSubject(secondVertexLabel, "name", "bhavans");
		secondVertex = getVertexForSubject(secondVertexLabel, "email", "test@bhavans.com");
		firstVertex.addEdge("works in", secondVertex);
		getVertexForSubject(thirdVertexLabel, "building", "#90");
		thirdVertex = getVertexForSubject(thirdVertexLabel, "street", "1st main");
		firstVertex.addEdge("lives in", thirdVertex);
		boolean response = registryDao.addEntity(graph,firstVertexLabel);
		assertTrue(response);

	}
	
	@Test
	public void test_add_with_shared_nodes(){
		identifier = generateRandomId();
		Vertex firstVertex = null;
		Vertex secondVertex = null;
		String firstVertexLabel = "teacher2";
		String secondVertexLabel = "school";
		getVertexForSubject(firstVertexLabel, "identifier", identifier);
		firstVertex = getVertexForSubject(firstVertexLabel, "type", "Teacher");
		getVertexForSubject(secondVertexLabel, "name", "bhavans");
		secondVertex = getVertexForSubject(secondVertexLabel, "email", "test@bhavans.com");
		firstVertex.addEdge("works in", secondVertex);
		boolean response = registryDao.addEntity(graph,firstVertexLabel);
		assertTrue(response);
	}
	
	@Test
	public void test_add_with_shared_nodes_add_new_properties(){
		identifier = generateRandomId();
		Vertex firstVertex = null;
		Vertex secondVertex = null;
		String firstVertexLabel = "teacher3";
		String secondVertexLabel = "school";
		getVertexForSubject(firstVertexLabel, "identifier", identifier);
		firstVertex = getVertexForSubject(firstVertexLabel, "type", "Teacher");
		getVertexForSubject(secondVertexLabel, "name", "bhavans");
		secondVertex = getVertexForSubject(secondVertexLabel, "studentCount", "1500");
		firstVertex.addEdge("works in", secondVertex);
		boolean response = registryDao.addEntity(graph,firstVertexLabel);
		assertTrue(response);
	}
	
	@Test
	public void test_add_with_shared_nodes_update_existing_properties(){
		identifier = generateRandomId();
		Vertex firstVertex = null;
		Vertex secondVertex = null;
		String firstVertexLabel = "teacher3";
		String secondVertexLabel = "school";
		getVertexForSubject(firstVertexLabel, "identifier", identifier);
		firstVertex = getVertexForSubject(firstVertexLabel, "type", "Teacher");
		getVertexForSubject(secondVertexLabel, "name", "bhavans");
		secondVertex = getVertexForSubject(secondVertexLabel, "email", "test@bvb.com");
		firstVertex.addEdge("works in", secondVertex);
		boolean response = registryDao.addEntity(graph,firstVertexLabel);
		assertTrue(response);
	}
	
	

	/*

	@Test
	public void testGetEntity(){
		EntityDto entityDto = new EntityDto();
		entityDto.setId(identifier);
		Object entity = registryDao.getEntityById(entityDto);
		assertFalse((entity!=null));

	}

	@Test
	public void testGetNonExistingEntity(){
		EntityDto entityDto = new EntityDto();
		entityDto.setId(generateRandomId());
		Object entity = registryDao.getEntityById(entityDto);
		assertFalse((entity!=null));

	}

	@Test
	public void testModifyEntity(){
		Vertex vertex = graph.addVertex(
				T.label,"identifier");
		vertex.property("is", "108115c3-320c-43d6-aaa7-7aab72777575");
		graph.addVertex(
				T.label,"type").property("is", "teacher");

		getVertexForSubject("identifier","is", identifier, t);
		getVertexForSubject("type","is", "teacher", t);
		getVertexForSubject("email","is", "consent driven");
		boolean response = registryDao.updateEntity(graph);
		assertFalse(response);
	}

	@Test
	public void testModifyNonExistingEntity(){
		//GraphTraversalSource t = graph.traversal();
		//Vertex vertex = null;
		getVertexForSubject("identifier","is", generateRandomId());
		getVertexForSubject("type","is", "teacher");
		getVertexForSubject("email","is", "consent driven");
		boolean response = registryDao.updateEntity(graph);
		assertFalse(response);
	}


	@Test
	public void testRemoveEntity(){
		EntityDto entityDto = new EntityDto();
		entityDto.setId(identifier);
		boolean response = registryDao.deleteEntity(entityDto);
		assertTrue(response);

	}

	@Test
	public void testRemoveNonExistingEntity(){
		EntityDto entityDto = new EntityDto();
		entityDto.setId(identifier);
		boolean response = registryDao.deleteEntity(entityDto);
		assertTrue(response);

	}*/

	@After
	public void closeGraph() throws Exception{
		if(graph!=null){
			graph.close();
		}
	}

	private static String generateRandomId(){
		return UUID.randomUUID().toString();
	}

	private Vertex getVertexForSubject(String subjectValue, String property, String objectValue){
		Vertex vertex = null;
		GraphTraversalSource t = graph.traversal();
		GraphTraversal<Vertex, Vertex> hasLabel = t.V().hasLabel(subjectValue);
		if(hasLabel.hasNext()){
			vertex = hasLabel.next();
		} else {
			vertex = graph.addVertex(
					T.label,subjectValue);
		}
		vertex.property(property, objectValue);
		return vertex;
	}

}
