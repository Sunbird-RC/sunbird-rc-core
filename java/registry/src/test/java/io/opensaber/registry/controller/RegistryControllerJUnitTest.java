package io.opensaber.registry.controller;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RegistryService;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=RegistryController.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RegistryControllerJUnitTest {

	
	@Autowired
	RegistryService registryService;
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	private static final String SUBJECT_LABEL = "ex:Picasso";
	
	
	@Test
	public void test_add_entity() throws NullPointerException, DuplicateRecordException {
		Model model = getRdf();
		boolean response = registryService.addEntity(model);
		assertTrue(response);
	}
	
	@Test
	public void test_add_existing_entity() throws NullPointerException, DuplicateRecordException {
		Model model = getRdf();
		expectedEx.expect(DuplicateRecordException.class);
		expectedEx.expectMessage(Constants.DUPLICATE_RECORD_MESSAGE);
		registryService.addEntity(model);
	}
	
	
	public Model getRdf(){
		Resource resource = ResourceFactory.createResource(SUBJECT_LABEL);
		Model model = ModelFactory.createDefaultModel();
		model.add(resource, FOAF.name, "Pablo");
		Node address = NodeFactory.createBlankNode();
		Node painting = NodeFactory.createBlankNode();
		model.add(resource,RDF.type, "ex:Artist");
		model.add(resource,FOAF.depiction, "ex:Image");
		/*Property property = ResourceFactory.createProperty("ex:homeAddress");
		model.add(resource,property, (RDFNode) address);
		property = ResourceFactory.createProperty("ex:creatorOf");
		model.add(resource,property, (RDFNode) painting);*/
		return model;
	}

}
