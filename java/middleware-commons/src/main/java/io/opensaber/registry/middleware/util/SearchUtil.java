package io.opensaber.registry.middleware.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opensaber.pojos.Filter;
import io.opensaber.pojos.SearchQuery;

public class SearchUtil {

	private static Logger logger = LoggerFactory.getLogger(SearchUtil.class);

	public static SearchQuery constructSearchQuery(Model inputRdf){
		StmtIterator stmtIterator = inputRdf.listStatements();
		List<Filter> filterList = new ArrayList<Filter>();
		SearchQuery searchQuery = new SearchQuery();
		while(stmtIterator.hasNext()){
			Statement statement = stmtIterator.next();
			Resource resource = statement.getSubject();			
			Property predicate = statement.getPredicate();
			RDFNode rdfNode = statement.getObject();	
			if(predicate.equals(RDF.type)){
				searchQuery.setTypeIRI(RDF.type.toString());
				searchQuery.setType(rdfNode.toString());
			} else if(!rdfNode.isAnon()){
				filterList = updateFilterList(inputRdf, resource, predicate, rdfNode, filterList);
			}
			//else we are ignoring anonymous rdf nodes
		}
		logger.info(String.format("Number of filters applied: %d",filterList.size()));
		logger.info(String.format("Filter list for search query : %s",filterList));
		searchQuery.setFilters(filterList);
		return searchQuery;
	}

	private static List<Filter> updateFilterList(Model inputRdf, Resource resource, Property predicate,
			RDFNode rdfNode, List<Filter> filterList){
		Object object = null;
		List<String> path = new ArrayList<String>();
		if(rdfNode.isLiteral()){
			object = rdfNode.asLiteral().getLexicalForm();
		}else{
			object = rdfNode.toString();
		}
		Filter filter = new Filter();
		filter.setSubject(resource.toString());
		filter.setProperty(predicate.toString());
		if(filterList.contains(filter)){
			filter = filterList.get(filterList.indexOf(filter));
			Object o = filter.getValue();
			List objList = null;
			if(o instanceof List){
				objList = (List)o;
			}else{
				objList = new ArrayList();
				objList.add(o);
			}
			objList.add(object);
			filter.setValue(objList);
		}else{
			path = getNestedPath(inputRdf,resource, path);
			filter.setPath(path);
			filter.setValue(object);
			filterList.add(filter);
		}
		return filterList;
	}

	private static List<String> getNestedPath(Model inputRdf, Resource resource, List<String> path){
		StmtIterator stmtIter = inputRdf.listStatements(null, null, resource);
		while(stmtIter.hasNext()){
			Statement s = stmtIter.next();
			path.add(s.getPredicate().toString());
			getNestedPath(inputRdf, s.getSubject(), path);
		}
		return path;
	}

}
