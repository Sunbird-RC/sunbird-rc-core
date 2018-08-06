package io.opensaber.registry.middleware.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import io.opensaber.pojos.Filter;
import io.opensaber.pojos.SearchQuery;

public class SearchUtil {

	public static SearchQuery constructSearchQuery(Model inputRdf){
		StmtIterator stmtIterator = inputRdf.listStatements();
		List<Filter> filterList = new ArrayList<Filter>();
		SearchQuery searchQuery = new SearchQuery();
		while(stmtIterator.hasNext()){
			Statement statement = stmtIterator.next();
			Property predicate = statement.getPredicate();
			RDFNode rdfNode = statement.getObject();
			Object object = null;
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
				filter.setProperty(predicate.toString());
				if(filterList.contains(filter)){
					for(Filter f: filterList){
						if(f.getProperty().equals(filter.getProperty())){
							filter = f;
							break;
						}
					}
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
					}
				}

				
				if(filter.getValue() == null){
					filter.setValue(object);
				}
				filterList.add(filter);
			}
		}
		searchQuery.setFilters(filterList);
		return searchQuery;
	}

}
