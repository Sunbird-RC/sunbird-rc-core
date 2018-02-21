package io.opensaber.registry.controller;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.neo4j.cluster.protocol.ConfigurationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;

import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=RegistryController.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RegistryControllerJUnitTest extends RegistryTestBase{
	
	private static final String VALID_JSONLD1 = "school1.jsonld";

	
	@Autowired
	RegistryService registryService;
	
	@Autowired
	private Environment environment;
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	private final String type = environment.getProperty(Constants.SUBJECT_LABEL_TYPE);
	private Model duplicateRdf = null;
	
	
	@Test
	public void test_adding_a_new_record() throws DuplicateRecordException {
		Model model = getNewValidRdf(VALID_JSONLD1, type);
		duplicateRdf = model;
		boolean response = registryService.addEntity(model);
		assertTrue(response);
	}
	
	@Test
	public void test_adding_duplicate_record() throws DuplicateRecordException {
		expectedEx.expect(DuplicateRecordException.class);
		expectedEx.expectMessage(Constants.DUPLICATE_RECORD_MESSAGE);
		registryService.addEntity(duplicateRdf);
	}
	
	@Test
	public void test_adding_record_with_invalid_type() throws DuplicateRecordException {
		Model model = getRdfWithInvalidTpe();
		expectedEx.expect(NullPointerException.class);
		registryService.addEntity(model);
	}



}
