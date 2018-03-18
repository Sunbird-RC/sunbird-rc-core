package io.opensaber.converters;

import java.util.Vector;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * <p>Utility functions for converting between the Jena and RDF4J API's</p>
 *
 */
public class JenaRDF4J {

	/**
	 * Internal model used to create instances of Jena API objects
	 */
	private static final Model mInternalModel = ModelFactory.createDefaultModel();

	/**
	 * rdf4j value factory for creating instances of rdf4j API objects
	 */
	private static final ValueFactory FACTORY = SimpleValueFactory.getInstance();

	/**
	 * Convert the given Jena Resource into a rdf4j Resource
	 * @param theRes the jena resource to convert
	 * @return the jena resource as a rdf4j resource
	 */
	public static org.eclipse.rdf4j.model.Resource asrdf4jResource(org.apache.jena.rdf.model.Resource theRes) {
		if (theRes == null) {
			return null;
		}
		else if (theRes.canAs(Property.class)) {
			return asrdf4jURI(theRes.as(Property.class));
		}
		else {
			return FACTORY.createBNode(theRes.getId().getLabelString());
		}
	}

	/**
	 * Convert the given Jena Property instance to a rdf4j IRI instance
	 * @param theProperty the Jena Property to convert
	 * @return the Jena property as a rdf4j Instance
	 */
	public static org.eclipse.rdf4j.model.IRI asrdf4jURI(org.apache.jena.rdf.model.Property theProperty) {
		if (theProperty == null) {
			return null;
		}
		else {
			return FACTORY.createIRI(theProperty.getURI());
		}
	}

	/**
	 * Convert the given Jena Literal to a rdf4j Literal
	 * @param theLiteral the Jena Literal to convert
	 * @return the Jena Literal as a rdf4j Literal
	 */
	public static org.eclipse.rdf4j.model.Literal asrdf4jLiteral(org.apache.jena.rdf.model.Literal theLiteral) {
		if (theLiteral == null) {
			return null;
		}
		else if (theLiteral.getLanguage() != null && !theLiteral.getLanguage().equals("")) {
			return FACTORY.createLiteral(theLiteral.getLexicalForm(),
										 theLiteral.getLanguage());
		}
		else if (theLiteral.getDatatypeURI() != null) {
			return FACTORY.createLiteral(theLiteral.getLexicalForm(),
										 FACTORY.createIRI(theLiteral.getDatatypeURI()));
		}
		else {
			return FACTORY.createLiteral(theLiteral.getLexicalForm());
		}
	}

	/**
	 * Convert the given Jena node as a rdf4j Value
	 * @param theNode the Jena node to convert
	 * @return the jena node as a rdf4j Value
	 */
	public static org.eclipse.rdf4j.model.Value asrdf4jValue(org.apache.jena.rdf.model.RDFNode theNode) {
		if (theNode == null) {
			return null;
		}
		else if (theNode.canAs(Literal.class)) {
			return asrdf4jLiteral(theNode.as(org.apache.jena.rdf.model.Literal.class));
		}
		else {
			return asrdf4jResource(theNode.as(org.apache.jena.rdf.model.Resource.class));
		}
	}

	/**
	 * Convert the given rdf4j Resource to a Jena Resource
	 * @param theRes the rdf4j resource to convert
	 * @return the rdf4j resource as a jena resource
	 */
	public static org.apache.jena.rdf.model.Resource asJenaResource(org.eclipse.rdf4j.model.Resource theRes) {
		if (theRes == null) {
			return null;
		}
		else if (theRes instanceof IRI) {
			return asJenaURI( (IRI) theRes);
		}
		else {
			return mInternalModel.createResource(new AnonId(((BNode) theRes).getID()));
		}
	}

	/**
	 * Convert the rdf4j value to a Jena Node
	 * @param theValue the rdf4j value
	 * @return the rdf4j value as a Jena node
	 */
	public static org.apache.jena.rdf.model.RDFNode asJenaNode(org.eclipse.rdf4j.model.Value theValue) {
		if (theValue instanceof org.eclipse.rdf4j.model.Literal) {
			return asJenaLiteral( (org.eclipse.rdf4j.model.Literal) theValue);
		}
		else {
			return asJenaResource( (org.eclipse.rdf4j.model.Resource) theValue);
		}
	}

	/**
	 * Convert the rdf4j URI to a Jena Property
	 * @param theIRI the rdf4j URI
	 * @return the URI as a Jena property
	 */
	public static org.apache.jena.rdf.model.Property asJenaURI(org.eclipse.rdf4j.model.IRI theIRI) {
		if (theIRI == null) {
			return null;
		}
		else {
			return mInternalModel.getProperty(theIRI.toString());
		}
	}

	/**
	 * Convert a rdf4j Literal to a Jena Literal
	 * @param theLiteral the rdf4j literal
	 * @return the rdf4j literal converted to Jena
	 */
	public static org.apache.jena.rdf.model.Literal asJenaLiteral(org.eclipse.rdf4j.model.Literal theLiteral) {
		if (theLiteral == null) {
			return null;
		}
		else if (theLiteral.getLanguage().isPresent()) {
			return mInternalModel.createLiteral(theLiteral.getLabel(),
												theLiteral.getLanguage().orElse(null));
		}
		else if (theLiteral.getDatatype() != null) {
			return mInternalModel.createTypedLiteral(theLiteral.getLabel(),
													 theLiteral.getDatatype().toString());
		}
		else {
			return mInternalModel.createLiteral(theLiteral.getLabel());
		}
	}

	/**
	 * Convert the rdf4j Graph to a Jena Model
	 * @param theModel the Model to convert
	 * @return the set of statements in the rdf4j Model converted and saved in a Jena Model
	 */
	public static org.apache.jena.rdf.model.Model asJenaModel(org.eclipse.rdf4j.model.Model theModel) {
		Model aModel = ModelFactory.createDefaultModel();

		for (final org.eclipse.rdf4j.model.Statement aStmt : theModel) {
			aModel.add(asJenaStatement(aStmt));
		}

		return aModel;
	}

	/**
	 * Convert the Jena Model to a rdf4j Model
	 * @param theModel the model to convert
	 * @return the set of statements in the Jena model saved in a rdf4j Graph
	 */
	public static org.eclipse.rdf4j.model.Model asRDF4JModel(org.apache.jena.rdf.model.Model theModel) {
		org.eclipse.rdf4j.model.Model aModel = new LinkedHashModel();

		StmtIterator sIter = theModel.listStatements();
		while (sIter.hasNext()) {
			aModel.add(asrdf4jStatement(sIter.nextStatement()));
		}
		sIter.close();
		return aModel;
	}

	/**
	 * Convert a Jena Statement to a rdf4j statement
	 * @param theStatement the statement to convert
	 * @return the equivalent rdf4j statement
	 */
	public static org.eclipse.rdf4j.model.Statement asrdf4jStatement(org.apache.jena.rdf.model.Statement theStatement)
	{	
		ValueFactory factory = SimpleValueFactory.getInstance();
		return factory.createStatement(
				asrdf4jResource(theStatement.getSubject()), 
				asrdf4jURI(theStatement.getPredicate()), 
				asrdf4jValue(theStatement.getObject()));
	}

	/**
	 * Convert a rdf4j statement to a Jena statement
	 * @param theStatement the statement to convert
	 * @return the equivalent Jena statement
	 */
	public static Statement asJenaStatement(org.eclipse.rdf4j.model.Statement theStatement) {
		return mInternalModel.createStatement(asJenaResource(theStatement.getSubject()),
											  asJenaURI(theStatement.getPredicate()),
											  asJenaNode(theStatement.getObject()));
	}
	
	// converts vector to string for rdf4j
	public static String convertVectorTordf4jString(Vector <String> inputVector)
	{
		String subjects = "";
		for(int subIndex = 0;subIndex < inputVector.size();subIndex++)
			subjects = subjects + "(<" + inputVector.elementAt(subIndex) + ">)";
		
		return subjects;

	}

	public static String convertVectorToJenaString(Vector <String> inputVector)
	{
		String subjects = "";
		for(int subIndex = 0;subIndex < inputVector.size();subIndex++)
			subjects = subjects + "<" + inputVector.elementAt(subIndex) + ">";		
		return subjects;
	}
}
