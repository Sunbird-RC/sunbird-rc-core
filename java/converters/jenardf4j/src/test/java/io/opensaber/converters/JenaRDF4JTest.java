package io.opensaber.converters;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.junit.Test;

public class JenaRDF4JTest {
	
	@Test
	public void test_jena_to_rdf4j() throws RDFParseException, UnsupportedRDFormatException, IOException{
		ModelBuilder builder = new ModelBuilder();
		String subjectLabel = "ex:Picasso";
		buildRDF4J(builder, subjectLabel);
		Model jenaModel = JenaRDF4J.asJenaModel(builder.build());
		assertNotNull(jenaModel);
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
