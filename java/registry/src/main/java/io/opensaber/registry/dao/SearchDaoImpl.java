package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import io.opensaber.pojos.Filter;
import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.middleware.util.*;
import io.opensaber.registry.util.*;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

public class SearchDaoImpl implements SearchDao {
    private IRegistryDao registryDao;

    public SearchDaoImpl(IRegistryDao registryDaoImpl) {
        registryDao = registryDaoImpl;
    }

	public JsonNode search(Graph graphFromStore, SearchQuery searchQuery) {

		GraphTraversalSource dbGraphTraversalSource = graphFromStore.traversal().clone();
		List<Filter> filterList = searchQuery.getFilters();
		GraphTraversal<Vertex, Vertex> resultGraphTraversal = dbGraphTraversalSource.clone().V().hasLabel(searchQuery.getRootLabel());

		List<P> predicates = new ArrayList<>();
		// Ensure the root label is correct
		if (filterList != null) {
			for (Filter filter : filterList) {
				String property = filter.getProperty();
				String genericValue = filter.getValue();
				String operator = filter.getOperator();
				String path = filter.getPath();

				//List valueList = getValueList(value);

				// Defaulting to "equals" operation
				if (operator == null || operator == "=") {
					resultGraphTraversal = resultGraphTraversal.has(property,
							P.eq(genericValue));
				}
				if (path != null) {
					if (resultGraphTraversal.asAdmin().clone().hasNext()) {
						resultGraphTraversal = resultGraphTraversal.asAdmin().clone().outE(path).outV();
					}
				}
			}
		}

		return getResult(graphFromStore, resultGraphTraversal);
	}

	private void updateValueList(Object value, List valueList) {
		valueList.add(value);
	}

	private List getValueList(Object value) {
		List valueList = new ArrayList();
		if (value instanceof List) {
			for (Object o : (List) value) {
				updateValueList(o, valueList);
			}
		} else {
			updateValueList(value, valueList);
		}
		return valueList;
	}

	private JsonNode getResult (Graph graph, GraphTraversal resultTraversal) {
		ArrayNode result = JsonNodeFactory.instance.arrayNode();
		if (resultTraversal != null) {
			while (resultTraversal.hasNext()) {
				Vertex v = (Vertex) resultTraversal.next();
				if ((!v.property(Constants.STATUS_KEYWORD).isPresent() ||
					Constants.STATUS_ACTIVE.equals(v.value(Constants.STATUS_KEYWORD)))) {

					ReadConfigurator configurator = new ReadConfigurator();
					configurator.setIncludeSignatures(false);
					configurator.setIncludeTypeAttributes(false);

					JsonNode answer = null;
					try {
						answer = registryDao.getEntity(graph, v, configurator);
					} catch (Exception e) {
						e.printStackTrace();
					}
					result.add(answer);
				}
			}
		}
		return result;
	}
}
