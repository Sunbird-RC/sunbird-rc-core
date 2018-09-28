package io.opensaber.registry.middleware.util;

import io.opensaber.converters.JenaRDF4J;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RDFUtil {

    private static Logger logger = LoggerFactory.getLogger(RDFUtil.class);
    
    public static Model getRdfModelBasedOnFormat(String jsonldData, String format){
    	Model m = ModelFactory.createDefaultModel();
    	StringReader reader = new StringReader(jsonldData);
    	return m.read(reader, null, format);
    }

    public static Statement updateSubjectLabel(Statement statement, String label) {
        Resource subject = statement.getSubject();
        Property predicate = statement.getPredicate();
        RDFNode object = statement.getObject();

        Resource subjectCopy = ResourceFactory.createResource(label);
        BeanWrapper bw = new BeanWrapperImpl(subject);
        BeanUtils.copyProperties(bw.getWrappedInstance(), subjectCopy);

        return ResourceFactory.createStatement(subjectCopy, predicate, object);
    }

    public static Statement updateResource(Statement statement, String label) {

        Resource subject = statement.getSubject();
        Property predicate = statement.getPredicate();
        RDFNode object = statement.getObject();

        Resource res = object.asResource();
        Resource objectCopy = ResourceFactory.createResource(label);
        BeanWrapper bw = new BeanWrapperImpl(object);
        BeanUtils.copyProperties(bw.getWrappedInstance(), objectCopy);

        return ResourceFactory.createStatement(subject, predicate, objectCopy);

    }

    public static String frameEntity(org.eclipse.rdf4j.model.Model entityModel) throws IOException {
        Model jenaEntityModel = JenaRDF4J.asJenaModel(entityModel);
        DatasetGraph g = DatasetFactory.create(jenaEntityModel).asDatasetGraph();
        JsonLDWriteContext ctx = new JsonLDWriteContext();
        InputStream is = RDFUtil.class.getClassLoader().getResourceAsStream("frame.json");
        String fileString = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
        ctx.setFrame(fileString);
        WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(org.apache.jena.riot.RDFFormat.JSONLD_FRAME_FLAT) ;
        PrefixMap pm = RiotLib.prefixMap(g);
        String base = null;
        StringWriter sWriterJena = new StringWriter();
        w.write(sWriterJena, g, pm, base, ctx) ;
        String jenaJSON = sWriterJena.toString();
        return jenaJSON;
    }

    public static void updateRdfModelNodeId(Model rdfModel, RDFNode object, String label) {
        StmtIterator stmtIterator = rdfModel.listStatements();
        ArrayList<Statement> updatedStatements = new ArrayList<>();

        while(stmtIterator.hasNext()) {
            Statement node = stmtIterator.next();
            if(!node.getObject().isLiteral() && node.getObject().asResource().equals(object)) {
                StmtIterator parentItr = rdfModel.listStatements(null, null, node.getSubject());
                StmtIterator nodeProperties = node.getSubject().listProperties();
                int updatePropertyCount =0;

                while (nodeProperties.hasNext()) {
                    Statement childNode = nodeProperties.next();
                    Statement updated = RDFUtil.updateSubjectLabel(childNode, label);
                    nodeProperties.remove();
                    updatedStatements.add(updated);
                    logger.debug("Updated Rdf statement %s : %s", updatePropertyCount++, updated.toString());
                }

                while (parentItr.hasNext()) {
                    Statement parent = parentItr.next();
                    Statement updatedParent = RDFUtil.updateResource(parent, label);
                    parentItr.remove();
                    updatedStatements.add(updatedParent);
                    logger.debug("Updated corresponding parent statement(s) for RdfModel(s) : "+ updatedParent.toString());
                }
            }
        }
        rdfModel.add(updatedStatements.toArray(new Statement[0]));
    }
    
    
    public static List<Resource> getRootLabels(Model rdfModel){
    	List<Resource> rootLabelList = new ArrayList<Resource>();
    	ResIterator resIter = rdfModel.listSubjects();
		while(resIter.hasNext()){
			Resource resource = resIter.next();
			StmtIterator stmtIter = rdfModel.listStatements(null, null, resource);
			if(!stmtIter.hasNext()){
				rootLabelList.add(resource);
			}
		}
		return rootLabelList;
    }
    
    public static List<String> getTypeForSubject(Model rdfModel, Resource root){
    	List<String> typeIRIs = new ArrayList<String>();
    	NodeIterator nodeIter = rdfModel.listObjectsOfProperty(root, RDF.type);
		while(nodeIter.hasNext()){
			RDFNode rdfNode = nodeIter.next();
			typeIRIs.add(rdfNode.toString());
		}
		return typeIRIs;
    }
    

    public static StmtIterator filterStatement(String subject, Property predicate, String object, Model resultModel){
		Resource subjectResource = subject!=null? ResourceFactory.createResource(subject) : null;
		RDFNode objectResource = object!=null? ResourceFactory.createResource(object) : null;
		StmtIterator iter = resultModel.listStatements(subjectResource, predicate, objectResource);
		return iter;
	}

    public static RDFNode getFirstObject(Resource node, String predicate, Model validationConfig) {
        RDFNode result = null;
        Property property = ResourceFactory.createProperty(predicate);
        List<RDFNode> nodeList = getListOfObjectNodes(node, property, validationConfig);
        if (nodeList.size() != 0) {
            result = nodeList.get(0);
        }
        return result;
    }
    
    public static List<Resource> getListOfSubjects(Property predicate, String object, Model resultModel){
		RDFNode objectResource = object!=null? ResourceFactory.createResource(object) : null;
		ResIterator iter = resultModel.listSubjectsWithProperty(predicate, objectResource);
		return iter.toList();
	}
    
    public static List<RDFNode> getListOfObjectNodes(Resource resource, Property predicate, Model resultModel){
		NodeIterator iter = resultModel.listObjectsOfProperty(resource, predicate);
		return iter.toList();
	}
    
    public static String printRDF(Model validationRdf) {
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, validationRdf, Lang.TTL);
		return sw.toString();
	}


    /**
     * This Method return updated model after adding signature statements if it not there else it will update existing signature value
     * @param model
     * @param registryContext
     * @param signatureDomain
     * @param entitySignMap
     * @param signatureModel
     * @return Model
     */
	public static Model getUpdatedSignedModel(Model model, String registryContext, String signatureDomain, Map entitySignMap, Model signatureModel){
        TypeMapper tm = TypeMapper.getInstance();
        List<Resource> rootLabelList= getRootLabels(model);
        Resource target = rootLabelList.get(0);
        Property signCreator = ResourceFactory.createProperty(registryContext+Constants.SIGN_CREATOR);
        Property signCreated = ResourceFactory.createProperty(registryContext+Constants.SIGN_CREATED_TIMESTAMP);
        Property signValue = ResourceFactory.createProperty(registryContext+Constants.SIGN_SIGNATURE_VALUE);
        RDFDatatype creatorDtype = tm.getSafeTypeByName(signatureDomain+Constants.SIGN_CREATOR);
        RDFDatatype createdDtype = tm.getSafeTypeByName(signatureDomain+Constants.SIGN_CREATED_TIMESTAMP);
        RDFDatatype signValueDtype = tm.getSafeTypeByName(signatureDomain+Constants.SIGN_SIGNATURE_VALUE);
        Double keyId = (Double)entitySignMap.get("keyId");
        String keyUrl =entitySignMap.get("keyUrl").toString()+keyId.intValue();

        if(signatureModel.isEmpty()){
        	Resource  r = ResourceFactory.createResource();
            model.add(target,ResourceFactory.createProperty(registryContext+Constants.SIGNATURES),r);
            model.add(r,RDF.type, ResourceFactory.createResource(signatureDomain+"GraphSignature2012"));
            model.add(r, signCreator, keyUrl, creatorDtype);
            model.add(r, signCreated, String.valueOf(entitySignMap.get("createdDate")),createdDtype);
            model.add(r, ResourceFactory.createProperty(registryContext+Constants.SIGN_NONCE), "",tm.getSafeTypeByName(signatureDomain+Constants.SIGN_NONCE));
            model.add(r, signValue, entitySignMap.get("signatureValue").toString(),signValueDtype);
            model.add(r, ResourceFactory.createProperty(registryContext+Constants.SIGNATURE_FOR),"#",tm.getSafeTypeByName(XMLConstants.W3C_XML_SCHEMA_NS_URI+"#anyURI"));
        }else{
        	StmtIterator stmtIter = signatureModel.listStatements();
        	while(stmtIter.hasNext()){
        		Statement s = stmtIter.next();
        		Property prop = s.getPredicate();
        		Resource subject = s.getSubject();
        		if(prop.equals(signCreator)){
        			 model.add(subject, prop, keyUrl,creatorDtype);
        		}else if(prop.equals(signCreated)){
        			model.add(subject, prop,String.valueOf(entitySignMap.get("createdDate")),createdDtype);
        		}else if(prop.equals(signValue)){
        			model.add(subject, prop, entitySignMap.get("signatureValue").toString(),signValueDtype);
        		}else{
        			model.add(s);
        		}
        	}
        }
        return model;
    }

    /**
     * This Method filter Signature statements from the root model and returns root signature model separately
     * @param model
     * @param registryContext
     * @return model
     */
	public static Model removeAndRetrieveSignature(Model model, String registryContext){
		Model existingSignatureModel = ModelFactory.createDefaultModel();
		TypeMapper tm = TypeMapper.getInstance();
		List<Resource> rootLabelList= getRootLabels(model);
        Resource target = rootLabelList.get(0);
        Literal literal = ResourceFactory.createTypedLiteral(target.toString(), tm.getSafeTypeByName(XMLConstants.W3C_XML_SCHEMA_NS_URI+"#anyURI"));
        ResIterator resIter = model.listSubjectsWithProperty(ResourceFactory.createProperty(registryContext+Constants.SIGNATURE_FOR),literal);
        if(resIter.hasNext()){
        	Resource subject = resIter.next();
        	StmtIterator iter = model.listStatements(subject, null, (RDFNode)null);
        	existingSignatureModel = iter.toModel();
        	model.remove(existingSignatureModel);
        	Statement s = ResourceFactory.createStatement(target,ResourceFactory.createProperty(registryContext+Constants.SIGNATURES),subject);
        	model.remove(s);
        	existingSignatureModel.add(s);
        }
        return existingSignatureModel;
	}

}
