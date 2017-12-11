package org.opensaber.jena.poc;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.base.file.Location;

/**
 * 
 * @author jyotsna
 * This class uses apache jena to convert json-ld to RDF
 *
 */
public class JsonLdToRdfConverter {
	
	private static final String OPEN_SABER_GRAPH = "http://open-saber.org/graph1/";
	private static final String TDB_DIRECTORY = "//home//tdb";

	public static void main(String args[]){
		JsonLdToRdfConverter jsonLdToRdfConverter = new JsonLdToRdfConverter();
		jsonLdToRdfConverter.writeAndReadJsonLd();
	}
	
	public void writeAndReadJsonLd() {
        try {
            InputStream inputStream = new FileInputStream("testing/teacher.json");
            Model m = ModelFactory.createDefaultModel();
            m.read(inputStream, null, "JSON-LD");
            writeToTDB(m);
            readFromTDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	public void writeToTDB(Model model){
		Location location = Location.create (TDB_DIRECTORY);
        Dataset dataset = TDBFactory.createDataset(location);
        dataset.begin(ReadWrite.WRITE);
        try {
        	dataset.addNamedModel(OPEN_SABER_GRAPH, model);
            dataset.commit();
        } catch (Exception e) {
        	e.printStackTrace();
            dataset.abort();
        } finally {
            dataset.end();
        }
	}
	
	public void readFromTDB(){

        Location location = Location.create (TDB_DIRECTORY);
       
        String queryString = 
            "SELECT ?x ?y ?z"
            + " WHERE { "
            + "GRAPH <"+OPEN_SABER_GRAPH+"> { "
            + "?x ?y ?z  }}";
        
        Dataset dataset = TDBFactory.createDataset(location);
        dataset.begin(ReadWrite.READ);
        try {
            Query query = QueryFactory.create(queryString);
            QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
            try {
                ResultSet results = qexec.execSelect();
                while ( results.hasNext() ) {
                    QuerySolution soln = results.nextSolution();
                    RDFNode rdfNode1 = soln.get("x");
                    RDFNode rdfNode2 = soln.get("y");
                    RDFNode rdfNode3 = soln.get("z");                    
                    System.out.println("Subject:"+rdfNode1 + " " + "Predicate:"+rdfNode2 + " " + "Object:"+rdfNode3);
                    System.out.println();
                }
            } finally {
                qexec.close();
            }
        } finally {
            dataset.end();
        }
	}

}
