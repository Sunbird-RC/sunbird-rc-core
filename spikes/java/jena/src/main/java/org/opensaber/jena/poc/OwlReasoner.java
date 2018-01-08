package org.opensaber.jena.poc;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.util.FileManager;

public class OwlReasoner {
	
	private static final String VALID_INPUT_RDF = "testing/validTeacher.rdf";
	private static final String INVALID_INPUT_RDF1 = "testing/invalidTeacher1.rdf";
	private static final String INVALID_INPUT_RDF2 = "testing/invalidTeacher2.rdf";
	private static final String INPUT_OWL = "testing/teacher.owl";

	
	public static void main(String args[]){
		OwlReasoner owlReasoner = new OwlReasoner();
		owlReasoner.testingValidReasoner();
		owlReasoner.testingInvalidReasoner1();
		owlReasoner.testingInvalidReasoner2();
	}
	
	public void testingValidReasoner(){
		Model inputOwl = FileManager.get().loadModel(INPUT_OWL);
		Reasoner reasoner = ReasonerRegistry.getOWLReasoner().bindSchema(inputOwl.getGraph());
		Model model = FileManager.get().loadModel(VALID_INPUT_RDF);   
		InfModel inference = ModelFactory.createInfModel(reasoner, model);
		ValidityReport reportList = inference.validate();
		System.out.println("Is validTeacher.rdf schema valid as per input owl:"+reportList.isValid());
	}
	
	public void testingInvalidReasoner1(){
		Model inputOwl = FileManager.get().loadModel(INPUT_OWL);
		Reasoner reasoner = ReasonerRegistry.getOWLReasoner().bindSchema(inputOwl.getGraph());
		Model model = FileManager.get().loadModel(INVALID_INPUT_RDF1);   
		InfModel inference = ModelFactory.createInfModel(reasoner, model);
		ValidityReport reportList = inference.validate();
		System.out.println("Is invalidTeacher1.rdf schema valid as per input owl:"+reportList.isValid());
	}
	
	public void testingInvalidReasoner2(){
		Model inputOwl = FileManager.get().loadModel(INPUT_OWL);
		Reasoner reasoner = ReasonerRegistry.getOWLReasoner().bindSchema(inputOwl.getGraph());
		Model model = FileManager.get().loadModel(INVALID_INPUT_RDF2);   
		InfModel inference = ModelFactory.createInfModel(reasoner, model);
		ValidityReport reportList = inference.validate();
		System.out.println("Is invalidTeacher2.rdf schema valid as per input owl:"+reportList.isValid());
	}
}
