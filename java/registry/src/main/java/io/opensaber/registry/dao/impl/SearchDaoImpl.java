package io.opensaber.registry.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.opensaber.pojos.Filter;
import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.dao.SearchDao;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.sink.DatabaseProvider;

@Component
public class SearchDaoImpl implements SearchDao {

	@Autowired
	private DatabaseProvider databaseProvider;

	@Autowired
	private UrlValidator urlValidator;

	@Autowired
	private RegistryDao registryDao;

	@Value("${registry.context.base}")
	private String registryContext;

	public Map<String,Graph> search(SearchQuery searchQuery) throws AuditFailedException, EncryptionException, RecordNotFoundException{

		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource dbGraphTraversalSource = graphFromStore.traversal();
		List<Filter> filterList = searchQuery.getFilters();
		Map<String,Graph> graphMap = new HashMap<String,Graph>();
		GraphTraversal<Vertex,Vertex> resultGraphTraversal = dbGraphTraversalSource.clone().V().hasLabel(searchQuery.getType()).inE(searchQuery.getTypeIRI()).outV();

		if(filterList != null){
			for(Filter filter : filterList){
				String property = filter.getProperty();
				Object value = filter.getValue();
				String operator = filter.getOperator();
				List<String> path = filter.getPath();
				GraphTraversal<Vertex,String> graphLabelTraversal = resultGraphTraversal.asAdmin().clone().label();
				Set<String> labels = new HashSet<String>();
				while(graphLabelTraversal.hasNext()){
					String label = graphLabelTraversal.next();
					labels.add(label);
				}
				List<String> valueIriList = new ArrayList<String>();
				List valueList = new ArrayList();
				if(value instanceof List){
					for(Object o: (List)value){
						updateValueList(o, valueIriList, valueList);
					}
				}else{
					updateValueList(value, valueIriList, valueList);
				}
				//Defaulting to "equals" operation
				if(operator == null){
					if(valueIriList.size() > 0){
						resultGraphTraversal = dbGraphTraversalSource.clone().V().hasLabel(P.within((List)valueIriList)).inE(property).outV().dedup();
					}
					else if(valueList.size() > 0){
						resultGraphTraversal = dbGraphTraversalSource.clone().V().has(property, P.within((List)valueList));
					}
					if(path != null){
						for(String p : path){
							if(resultGraphTraversal.asAdmin().clone().hasNext()){
								resultGraphTraversal = resultGraphTraversal.asAdmin().clone().inE(p).outV();
							}
						}
					}
					if(resultGraphTraversal.asAdmin().clone().hasNext()){
						resultGraphTraversal = resultGraphTraversal.asAdmin().clone().hasLabel(P.within(labels));
					}
				}else{
					// TODO for other operators
				}
			}
			getGraphByTraversal(resultGraphTraversal, graphMap);
		}
		return graphMap;
	}
	 
	private void getGraphByTraversal(GraphTraversal resultTraversal, Map<String,Graph> graphMap) throws AuditFailedException, EncryptionException, RecordNotFoundException{
		if(resultTraversal !=null){
		while(resultTraversal.hasNext()){
			Vertex v = (Vertex)resultTraversal.next();
			if(v!=null && (!v.property(registryContext+Constants.STATUS_KEYWORD).isPresent() || Constants.STATUS_ACTIVE.equals(v.value(registryContext + Constants.STATUS_KEYWORD)))){
				graphMap.put(v.label(),registryDao.getEntityByVertex(v));
			}
		}
		}
	}
	
	private void updateValueList(Object value, List<String> valueIriList, List valueList){
		if(urlValidator.isValid(value.toString())){
			valueIriList.add(value.toString());
		}else{
			valueList.add(value);
		}
	}

}
