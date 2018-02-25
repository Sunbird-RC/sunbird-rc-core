package io.opensaber.converters;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Test;

public class JenaRDF4JTest {
	
	@Test
	public void test_jena_to_rdf4j() throws IOException{
		ModelBuilder builder = new ModelBuilder();
		String subjectLabel = "ex:Picasso";
		buildRDF4J(builder, subjectLabel);
		org.eclipse.rdf4j.model.Model rdf4jModel = builder.build();
		Model jenaModel = JenaRDF4J.asJenaModel(rdf4jModel);
		org.eclipse.rdf4j.model.Model _rdf4jModel = JenaRDF4J.asRDF4JModel(jenaModel);
		assertEquals(rdf4jModel, _rdf4jModel);
//		TODO need to write tests - performing visual inspection now
		printJenaStatements(jenaModel);
		printRDF4JStatements(rdf4jModel);
		printRDF4JStatements(_rdf4jModel);
	}

	private void printRDF4JStatements(org.eclipse.rdf4j.model.Model rdf4jModel) {
		for (org.eclipse.rdf4j.model.Statement statement: rdf4jModel) {
			System.out.println(statement);
		}
		
	}

	private void printJenaStatements(Model jenaModel) {
		StmtIterator iterator = jenaModel.listStatements();
		while(iterator.hasNext()){
			Statement statement = iterator.next();
			System.out.println(statement);
		}
	}

	private void buildRDF4J(ModelBuilder builder, String subjectLabel) {
		ValueFactory vf = SimpleValueFactory.getInstance();
		BNode address = vf.createBNode();
		BNode painting = vf.createBNode();
		BNode reaction = vf.createBNode();
		builder
			.setNamespace("ex", "http://example.org/")
			.subject(subjectLabel )
			.add(FOAF.FIRST_NAME, "Pablo")
			.add("ex:homeAddress", address)
			.add("ex:creatorOf", painting)
			.subject(address)
				.add("ex:street", "31 Art Gallery")
				.add("ex:city", "Madrid")
				.add("ex:country", "Spain")
				.add(RDF.TYPE,"ex:PostalAddress")
			.subject(painting)
				.add(RDF.TYPE,"ex:CreativeWork")
				.add("ex:depicts", "cubes")
				.add("ex:reaction", reaction)
				.subject(reaction)
					.add("ex:rating","5")
					.add(RDF.TYPE,"ex:AggregateRating");
	}

	private void printInputStream(InputStream inputStream) throws IOException {
		String readLine;
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		while (((readLine = br.readLine()) != null)) {
			System.out.println(readLine);
		}
	}

}
