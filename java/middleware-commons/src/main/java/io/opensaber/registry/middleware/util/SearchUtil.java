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
			List<String> path = new ArrayList<String>();
			Object object = null;
			if(!rdfNode.isAnon()){
				if(rdfNode.isLiteral()){
					object = rdfNode.asLiteral().getLexicalForm();
				}else{
					object = rdfNode.toString();
				}
				if(predicate.equals(RDF.type)){
					searchQuery.setTypeIRI(RDF.type.toString());
					searchQuery.setType((String)object);
				}else{
					Filter filter = new Filter();
					filter.setSubject(resource.toString());
					filter.setProperty(predicate.toString());
					if(filterList.contains(filter)){
						filter = filterList.get(filterList.indexOf(filter));
						if(filter.getValue()!=null){
							Object o = filter.getValue();
							List objList = null;
							if(o instanceof List){
								objList = (List)o;
							}else{
								objList = new ArrayList();
								objList.add(object);
							}
							objList.add(o);
							filter.setValue(objList);
						}else{
							filter.setValue(object);
						}
						
					}else{
						path = getNestedPath(inputRdf,resource, path);
						filter.setPath(path);
						filter.setValue(object);
						filterList.add(filter);
					}
				}
			}
		}
		logger.info("Filter list for search query:"+filterList);
		searchQuery.setFilters(filterList);
		return searchQuery;
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
