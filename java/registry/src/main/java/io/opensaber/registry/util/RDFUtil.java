package io.opensaber.registry.util;

import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;

public class RDFUtil {

    private static Logger logger = LoggerFactory.getLogger(RDFUtil.class);

    @Value("${registry.context.base}")
    private static String registryContext;

    /**
     * This method returns a List of RDFNodes which are blank nodes from a Jena Model
     * @param model
     * @return
     */
    public static List<RDFNode> getBlankNodes(Model model) {
        StmtIterator it = model.listStatements();
        List<RDFNode> blankNodes = new ArrayList<>();
        while (it.hasNext()) {
            Statement stmt = it.next();
            if (stmt.getSubject().isAnon() && !stmt.getObject().isLiteral() && stmt.getSubject().getURI() == null) {
                blankNodes.add(stmt.getObject());
            }
        }
        return blankNodes;
    }

    /**
     * This method updates the Blank node's URI with a random generated UUID. Also, the
     * Subject of the children nodes of the blank node will be updated with the same URI.
     * @param model
     */
    public static void updateIdForBlankNode(Model model) {
        StmtIterator stmtIterator = model.listStatements();

        ArrayList<Statement> updatedStatements = new ArrayList<>();
        while (stmtIterator.hasNext()) {
            Statement node = stmtIterator.next();

            if (node.getSubject().isAnon() && !node.getObject().isLiteral() && node.getSubject().getURI() == null) {

                StmtIterator parent = model.listStatements(null, null, node.getSubject());

                String label = UUID.randomUUID().toString();
                StmtIterator nodeProperties = node.getSubject().listProperties();
                // String namespace = node.getObject().asResource().getNameSpace();

                /*
                 * Update the child node labels
                 */
                while (nodeProperties.hasNext()) {
                    Statement childNode = nodeProperties.next();
                    Statement updated = updateSubjectLabel(childNode, label, registryContext);
                    nodeProperties.remove();
                    updatedStatements.add(updated);
                }

                // Update the parent node label
                // updateSubjectLabel(node, label, namespace);

                while(parent.hasNext()) {
                    Statement parentSt = parent.next();
                    Statement updatedParent = updateResource(parentSt, label, registryContext);
                    parent.remove();
                    updatedStatements.add(updatedParent);
                }
            }
        }
        model.add(updatedStatements.toArray(new Statement[0]));
    }

    private static Statement updateSubjectLabel(Statement statement, String label, String namespace) {
        Resource subject = statement.getSubject();
        Property predicate = statement.getPredicate();
        RDFNode object = statement.getObject();

        Resource subjectCopy = ResourceFactory.createResource(String.format("%s%s", namespace, label));
        BeanWrapper bw = new BeanWrapperImpl(subject);
        BeanUtils.copyProperties(bw.getWrappedInstance(), subjectCopy);

        return ResourceFactory.createStatement(subjectCopy, predicate, object);
    }

    private static Statement updateResource(Statement statement, String label, String namespace) {

        Resource subject = statement.getSubject();
        Property predicate = statement.getPredicate();
        RDFNode object = statement.getObject();

        Resource res = object.asResource();
        Resource objectCopy = ResourceFactory.createResource(String.format("%s%s", namespace, label));
        BeanWrapper bw = new BeanWrapperImpl(object);
        BeanUtils.copyProperties(bw.getWrappedInstance(), objectCopy);

        return ResourceFactory.createStatement(subject, predicate, objectCopy);

    }

}
