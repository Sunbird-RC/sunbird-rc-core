package io.opensaber.registry.dao.impl;

import java.io.IOException;
import java.util.*;

import com.google.common.collect.ImmutableList;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.util.RDFUtil;
import io.opensaber.utils.converters.RDF2Graph;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;

@Component
public class RegistryDaoImpl implements RegistryDao {

    private static Logger logger = LoggerFactory.getLogger(RegistryDaoImpl.class);

	@Autowired
	private DatabaseProvider databaseProvider;

	@Override
	public List getEntityList() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	@Override
	public boolean addEntity(Graph entity, String label) throws DuplicateRecordException {

		logger.debug("Database Provider features: \n" + databaseProvider.getGraphStore().features());

		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource traversalSource = graphFromStore.traversal();
		if (traversalSource.clone().V().hasLabel(label).hasNext()) {
			throw new DuplicateRecordException(Constants.DUPLICATE_RECORD_MESSAGE);
		}

		TinkerGraph graph = (TinkerGraph) entity;
		GraphTraversalSource gts = graph.traversal();
		GraphTraversal<Vertex, Vertex> traversal = gts.V();
		Map<String, List<Object[]>> map = new HashMap<>();

		if (graphFromStore.features().graph().supportsTransactions()) {
			org.apache.tinkerpop.gremlin.structure.Transaction tx;
			tx = graphFromStore.tx();
			tx.onReadWrite(org.apache.tinkerpop.gremlin.structure.Transaction.READ_WRITE_BEHAVIOR.AUTO);
			createEdgeNodes(traversalSource, traversal, map);
			tx.commit();
			tx.close();
		} else {
			createEdgeNodes(traversalSource, traversal, map);
		}

		return true;
	}
	*/

	@Override
	public boolean addEntity(Graph entity, String label) throws DuplicateRecordException {

		logger.debug("Database Provider features: \n" + databaseProvider.getGraphStore().features());
		logger.info("Creating entity with label " + label);
		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource traversalSource = graphFromStore.traversal();
		if (traversalSource.clone().V().hasLabel(label).hasNext()) {
			throw new DuplicateRecordException(Constants.DUPLICATE_RECORD_MESSAGE);
		}

		TinkerGraph graph = (TinkerGraph) entity;
		createOrUpdateEntity(graph, label);
		logger.info("Successfully created entity with label " + label);
		return true;
	}

	/**
	 * This method is commonly used for both create and update entity
	 * @param entity
	 * @param rootLabel
	 */
	private void createOrUpdateEntity(Graph entity, String rootLabel) {
		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource dbGraphTraversalSource = graphFromStore.traversal();

		TinkerGraph graph = (TinkerGraph) entity;
		GraphTraversalSource traversal = graph.traversal();

		if (graphFromStore.features().graph().supportsTransactions()) {
			org.apache.tinkerpop.gremlin.structure.Transaction tx;
			tx = graphFromStore.tx();
			tx.onReadWrite(org.apache.tinkerpop.gremlin.structure.Transaction.READ_WRITE_BEHAVIOR.AUTO);
			addOrUpdateVerticesAndEdges(dbGraphTraversalSource, traversal, rootLabel);
			tx.commit();
			tx.close();
		} else {
			addOrUpdateVerticesAndEdges(dbGraphTraversalSource, traversal, rootLabel);
		}

	}

	/**
	 * This method creates the root node of the entity if it already isn't present in the graph store
	 * or updates the properties of the root node or adds new properties if the properties are not already
	 * present in the node.
	 * @param dbTraversalSource
	 * @param entitySource
	 * @param rootLabel
	 */
	private void addOrUpdateVerticesAndEdges(GraphTraversalSource dbTraversalSource,
											 GraphTraversalSource entitySource, String rootLabel) {

		GraphTraversal<Vertex, Vertex> gts = entitySource.clone().V().hasLabel(rootLabel);

		while (gts.hasNext()) {
			Vertex v = gts.next();
			String label = generateBlankNodeLabel(v.label());
			GraphTraversal<Vertex, Vertex> hasLabel = dbTraversalSource.clone().V().hasLabel(label);

			if (hasLabel.hasNext()) {
				logger.info(String.format("Root node label %s already exists. Updating properties for the root node.", rootLabel));
				Vertex existingVertex = hasLabel.next();
				copyProperties(v, existingVertex);
				addOrUpdateVertexAndEdge(v, existingVertex, dbTraversalSource);
			} else {
				Vertex newVertex = dbTraversalSource.clone().addV(label).next();
				copyProperties(v, newVertex);
				addOrUpdateVertexAndEdge(v, newVertex, dbTraversalSource);
			}
		}
	}

	/**
	 * This method takes the root node of an entity and then recursively creates or updates child vertices
	 * and edges.
	 * @param v
	 * @param dbVertex
	 * @param dbGraph
	 */
	private void addOrUpdateVertexAndEdge(Vertex v, Vertex dbVertex, GraphTraversalSource dbGraph) {
		Iterator<Edge> edges = v.edges(Direction.OUT);
		Stack<Pair<Vertex, Vertex>> parsedVertices = new Stack<>();
		List<Edge> dbEdgesForVertex = ImmutableList.copyOf(dbVertex.edges(Direction.OUT));
		while(edges.hasNext()) {
			Edge e = edges.next();
			Vertex ver = e.inVertex();
			String label = generateBlankNodeLabel(ver.label());
			GraphTraversal<Vertex, Vertex> gt = dbGraph.clone().V().hasLabel(label);
			if (gt.hasNext()) {
				Vertex existingV = gt.next();
				logger.info(String.format("Vertex with label %s already exists. Updating properties for the vertex", existingV.label()));
				copyProperties(ver, existingV);
				Optional<Edge> edgeAlreadyExists =
						dbEdgesForVertex.stream().filter(ed -> ed.label().equalsIgnoreCase(e.label())).findFirst();
				if(!edgeAlreadyExists.isPresent()) {
					logger.info(String.format("Adding edge with label %s for the vertex label %s.", e.label(), existingV.label()));
					dbVertex.addEdge(e.label(), existingV);
				}
				parsedVertices.push(new Pair<>(ver, existingV));
			} else {
				Vertex newV = dbGraph.addV(label).next();
				logger.info(String.format("Adding vertex with label %s and adding properties", newV.label()));
				copyProperties(ver, newV);
				logger.info(String.format("Adding edge with label %s for the vertex label %s.", e.label(), newV.label()));
				dbVertex.addEdge(e.label(), newV);
				parsedVertices.push(new Pair<>(ver, newV));
			}
		}
		parsedVertices.forEach(pv -> addOrUpdateVertexAndEdge(pv.getValue0(), pv.getValue1(), dbGraph));
	}

	/**
	 * Blank nodes are no longer supported. If the input data has a blank node, which is identified
	 * by the node's label which starts with :_, then a random UUID is used as the label for the blank node.
	 * @param label
	 * @return
	 */
	private String generateBlankNodeLabel(String label) {
		if(label.startsWith("_:")) {
			label = String.format("http://example.com/voc/teacher/1.0.0/%s", generateRandomUUID());
		}
		return label;
	}

	public static String generateRandomUUID() {
		return UUID.randomUUID().toString();
	}

	/**
	 * @deprecated
	 * @param dbTraversalSource
	 * @param traversal
	 * @param map
	 */
	private void createEdgeNodes(GraphTraversalSource dbTraversalSource, GraphTraversal<Vertex, Vertex> traversal,
								 Map<String, List<Object[]>> map) {
		Map<String, Vertex> createdNodeMap = new HashMap<>();
		while (traversal.hasNext()) {
			Vertex v = traversal.next();
			Vertex newVertex = getNodeWithProperties(dbTraversalSource, v, createdNodeMap);
			Iterator<Edge> outgoingEdges = v.edges(Direction.OUT);
			Iterator<Edge> incomingEdges = v.edges(Direction.IN);
			createEdgeNodes(outgoingEdges, dbTraversalSource, newVertex, map, Direction.OUT, v, createdNodeMap);
			createEdgeNodes(incomingEdges, dbTraversalSource, newVertex, map, Direction.IN, v, createdNodeMap);
		}
	}

	/**
	 * @deprecated
	 * @param dbTraversalSource
	 * @param v
	 * @param createdNodeMap
	 * @return
	 */
	private Vertex getNodeWithProperties(GraphTraversalSource dbTraversalSource, Vertex v, Map<String, Vertex> createdNodeMap) {

		Vertex newVertex;

		if (createdNodeMap.containsKey(v.label())) {
			newVertex = createdNodeMap.get(v.label());
		} else {
			GraphTraversal nodes = dbTraversalSource.clone().V().hasLabel(v.label());
			if (nodes.hasNext()) {
				newVertex = (Vertex) nodes.next();
			} else {
				newVertex = dbTraversalSource.clone().addV(v.label()).next();
			}
			copyProperties(v, newVertex);
			createdNodeMap.put(v.label(), newVertex);
		}
		return newVertex;
	}

	/**
	 * @deprecated
	 * @param edges
	 * @param dbTraversalSource
	 * @param newVertex
	 * @param map
	 * @param direction
	 * @param traversalVertex
	 * @param createdNodeMap
	 */
	private void createEdgeNodes(Iterator<Edge> edges, GraphTraversalSource dbTraversalSource,
								 Vertex newVertex, Map<String, List<Object[]>> map,
								 Direction direction, Vertex traversalVertex, Map<String, Vertex> createdNodeMap) {

		while (edges.hasNext()) {

			Edge edge = edges.next();
			Vertex vertex = direction.equals(Direction.OUT) ? edge.inVertex() : edge.outVertex();
			boolean nodeAndEdgeExists = validateAddVertex(map, traversalVertex, edge, direction);

			if (!nodeAndEdgeExists) {
				Vertex nextVertex = getNodeWithProperties(dbTraversalSource, vertex, createdNodeMap);

				if (direction.equals(Direction.OUT)) {
					newVertex.addEdge(edge.label(), nextVertex);
				} else {
					nextVertex.addEdge(edge.label(), newVertex);
				}

				addNodeAndEdgeToGraph(map, traversalVertex, vertex, direction);
			}
		}
	}

	/**
	 * Method to add created nodes and edges to the graph
	 * @deprecated
	 * @param vertexMap
	 * @param currentVertex
	 * @param newVertex
	 * @param direction
	 */
	private void addNodeAndEdgeToGraph(Map<String, List<Object[]>> vertexMap, Vertex currentVertex,
									   Vertex newVertex, Direction direction) {
		String currentVertexLabel = direction.equals(Direction.OUT) ? newVertex.label() : currentVertex.label();
		String createdVertexLabel = direction.equals(Direction.OUT) ? currentVertex.label() : newVertex.label();
		Object[] edgeArray = {direction, currentVertexLabel};
		List<Object[]> edgeArrayList = vertexMap.getOrDefault(createdVertexLabel, new ArrayList<>());
		edgeArrayList.add(edgeArray);
		vertexMap.put(createdVertexLabel, edgeArrayList);
	}

	/**
	 * Method to validate if the vertex needs to be added to the graph
	 * @deprecated
	 * @param vertexMap
	 * @param currTraversalVertex
	 * @param edge
	 * @param direction
	 * @return
	 */
	private boolean validateAddVertex(Map<String, List<Object[]>> vertexMap,
									  Vertex currTraversalVertex, Edge edge, Direction direction) {
		boolean addVertex = false;

		Vertex edgeVertex = direction.equals(Direction.OUT) ? edge.inVertex() : edge.outVertex();
		String vertexLabel = direction.equals(Direction.OUT) ? currTraversalVertex.label() : edgeVertex.label();
		String edgeLabel = direction.equals(Direction.OUT) ? edgeVertex.label() : currTraversalVertex.label();

		if (vertexMap.containsKey(vertexLabel)) {
			List<Object[]> edgeArrayList = vertexMap.get(vertexLabel);
			for (Object[] edgeArray : edgeArrayList) {
				if (edgeArray.length == 2 && edgeArray[0].equals(direction.opposite()) && edgeArray[1].equals(edgeLabel)) {
					addVertex = true;
					break;
				}
			}
		}
		return addVertex;

	}

	@Override
	public boolean updateEntity(Graph entityForUpdate, String rootNodeLabel) throws RecordNotFoundException {
		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource dbGraphTraversalSource = graphFromStore.traversal();
		TinkerGraph graphForUpdate = (TinkerGraph) entityForUpdate;

		// Check if the root node being updated exists in the database
		GraphTraversal<Vertex, Vertex> hasRootLabel = dbGraphTraversalSource.clone().V().hasLabel(rootNodeLabel);
		if (!hasRootLabel.hasNext()) {
			throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
		} else {
			createOrUpdateEntity(graphForUpdate, rootNodeLabel);
		}
		return false;
	}

	@Override
	public Graph getEntityById(String label) throws RecordNotFoundException {
		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource traversalSource = graphFromStore.traversal();
		logger.info("FETCH: "+label);
		GraphTraversal<Vertex, Vertex> hasLabel = traversalSource.clone().V().hasLabel(label);
		Graph parsedGraph = TinkerGraph.open();
		if (!hasLabel.hasNext()) {
			throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
		} else {
			Vertex subject = hasLabel.next();
			Vertex newSubject = parsedGraph.addVertex(subject.label());
			copyProperties(subject, newSubject);
			extractGraphFromVertex(parsedGraph,newSubject,subject);
		}
		return parsedGraph;
	}

	private void copyProperties(Vertex subject, Vertex newSubject) {
		Iterator<VertexProperty<Object>> iter = subject.properties();
		while(iter.hasNext()){
			VertexProperty<Object> property = iter.next();
			newSubject.property(property.key(), property.value());
		}
	}

	@Override
	public boolean deleteEntity(Object entity) {
		// TODO Auto-generated method stub
		return false;
	}

	private void extractGraphFromVertex(Graph parsedGraph,Vertex parsedGraphSubject,Vertex s) {
		Iterator<Edge> edgeIter = s.edges(Direction.OUT);
		Edge edge;
		Stack<Vertex> vStack = new Stack<Vertex>();
		Stack<Vertex> parsedVStack = new Stack<Vertex>();
		while(edgeIter.hasNext()){
			edge = edgeIter.next();
			Vertex o = edge.inVertex();
			Vertex newo = parsedGraph.addVertex(o.label());
			copyProperties(o, newo);
			parsedGraphSubject.addEdge(edge.label(), newo);
			vStack.push(o);
			parsedVStack.push(newo);
			dump_graph(parsedGraph,"outgoing.json");
		}
		Iterator<Vertex> vIterator = vStack.iterator();
		Iterator<Vertex> parsedVIterator = parsedVStack.iterator();
		while(vIterator.hasNext()){
			s = vIterator.next();
			parsedGraphSubject = parsedVIterator.next();
			extractGraphFromVertex(parsedGraph,parsedGraphSubject,s);
		}
	}

	private void dump_graph(Graph parsedGraph, String filename) {
		try {
			parsedGraph.io(IoCore.graphson()).writeGraph(filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
