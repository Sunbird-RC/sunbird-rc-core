package io.opensaber.registry.util;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RDFUtil {

    private static Logger logger = LoggerFactory.getLogger(RDFUtil.class);

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
            if (stmt.getSubject().isAnon() && stmt.getPredicate().equals(RDF.type) && stmt.getObject().isURIResource()) {
                blankNodes.add(stmt.getObject());
            }
        }
        return blankNodes;
    }

    public static Optional<Statement> updateIdForBlankNode(Statement statement, List<RDFNode> blankNodes) {

        Optional<RDFNode> search;
        Optional<Statement> updatedStatement = Optional.empty();

        if (statement.getObject().isURIResource()) {

            String searchURI = statement.getObject().asResource().getURI();

            search = blankNodes.stream().filter(p -> p.asResource().getURI().equals(searchURI)).findFirst();

            if (search.isPresent()) {
                Resource subject = statement.getSubject();
                Property predicate = statement.getPredicate();
                RDFNode object = statement.getObject();
                String namespace = object.asResource().getNameSpace();

                Resource subjectCopy =
                        ResourceFactory.createResource(String.format("%s%s", namespace,
                                        UUID.randomUUID().toString().replaceAll("-", "")));
                BeanWrapper bw = new BeanWrapperImpl(subject);
                BeanUtils.copyProperties(bw.getWrappedInstance(), subjectCopy);
                updatedStatement = Optional.of(ResourceFactory.createStatement(subjectCopy, predicate, object));
            }
        }

        return updatedStatement;

    }
}
