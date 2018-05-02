
package io.opensaber.registry.dao.impl;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.collect.ImmutableList;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.model.AuditRecord;
import io.opensaber.registry.schema.config.SchemaConfigurator;
import io.opensaber.registry.service.EncryptionService;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.util.RDFUtil;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import org.apache.commons.validator.routines.UrlValidator;

@Component
public class RegistryDaoImpl implements RegistryDao {

	public static final String META = "meta.";
	private static Logger logger = LoggerFactory.getLogger(RegistryDaoImpl.class);
	

	@Autowired
	private DatabaseProvider databaseProvider;

	@Autowired
	private EncryptionService encryptionService;
	
	@Value("${registry.context.base}")
	private String registryContext;
	
	@Autowired
	SchemaConfigurator schemaConfigurator;

	@Autowired
	ApplicationContext appContext;
	
	@Value("${feature.toggling}")
	private Boolean featureToggling;

	@Override
	public List getEntityList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addEntity(Graph entity, String label) throws DuplicateRecordException, NoSuchElementException, EncryptionException, AuditFailedException, RecordNotFoundException{
		logger.debug("Database Provider features: \n" + databaseProvider.getGraphStore().features());
		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource traversalSource = graphFromStore.traversal();
		if (traversalSource.clone().V().hasLabel(label).hasNext()) {
			closeGraph(graphFromStore);
			throw new DuplicateRecordException(Constants.DUPLICATE_RECORD_MESSAGE);
		}

		TinkerGraph graph = (TinkerGraph) entity;
		String rootNodeLabel = createOrUpdateEntity(graph, label, "create");
		logger.info("Successfully created entity with label " + rootNodeLabel);
		closeGraph(graphFromStore);
		return rootNodeLabel;
	}
	
	
	@Override
	public String addEntity(Graph entity, String label, String rootNodeLabel, String property) throws DuplicateRecordException, RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException{
		logger.debug("Database Provider features: \n" + databaseProvider.getGraphStore().features());
		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource traversalSource = graphFromStore.traversal();
		if (rootNodeLabel!=null && property!=null && !traversalSource.clone().V().hasLabel(rootNodeLabel).hasNext()) {
			closeGraph(graphFromStore);
			throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
		} else if (traversalSource.clone().V().hasLabel(label).hasNext()) {
			closeGraph(graphFromStore);
			throw new DuplicateRecordException(Constants.DUPLICATE_RECORD_MESSAGE);
		}

		TinkerGraph graph = (TinkerGraph) entity;
		label = createOrUpdateEntity(graph, label, "create");
		
		if (rootNodeLabel!=null && property!=null){
			connectNodes(rootNodeLabel, label, property);
		}
		logger.info("Successfully created entity with label " + label);
		closeGraph(graphFromStore);
		return label;
	}

	private void closeGraph(Graph graph) {
		try {
			graph.close();
		} catch (Exception ex) {
			logger.error("Exception when closing the database graph", ex);
		}
	}
	
	private void connectNodes(String rootLabel, String label, String property) throws RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException {
		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource traversalSource = graphFromStore.traversal();
		
		if (!traversalSource.clone().V().hasLabel(rootLabel).hasNext()) {
			closeGraph(graphFromStore);
			throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
		}
		
		if (!traversalSource.clone().V().hasLabel(label).hasNext()) {
			closeGraph(graphFromStore);
			throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
		}
		
		if (graphFromStore.features().graph().supportsTransactions()) {
			org.apache.tinkerpop.gremlin.structure.Transaction tx = graphFromStore.tx();
			tx.onReadWrite(org.apache.tinkerpop.gremlin.structure.Transaction.READ_WRITE_BEHAVIOR.AUTO);
			connectRootToEntity(traversalSource, rootLabel, label, property);
			tx.commit();
			// tx.close();
		} else {
			connectRootToEntity(traversalSource, rootLabel, label, property);
		}
	}
	
	private void connectRootToEntity(GraphTraversalSource dbTraversalSource, String rootLabel, String label, String property) throws RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException {
		GraphTraversal<Vertex, Vertex> rootGts = dbTraversalSource.clone().V().hasLabel(rootLabel);
		GraphTraversal<Vertex, Vertex> entityGts = dbTraversalSource.clone().V().hasLabel(label);
		Vertex rootVertex = rootGts.next();
		Vertex entityVertex = entityGts.next();
		rootVertex.addEdge(property, entityVertex);
		AuditRecord record = appContext.getBean(AuditRecord.class);
		record
		.subject(rootVertex.label())
		.predicate(property)
		.oldObject(null)
		.newObject(entityVertex.label())
		.record(databaseProvider);
	}

	/**
	 * This method is commonly used for both create and update entity
	 * @param entity
	 * @param rootLabel
	 * @throws EncryptionException 
	 * @throws NoSuchElementException 
	 */
	private String createOrUpdateEntity(Graph entity, String rootLabel, String methodOrigin) throws NoSuchElementException, EncryptionException, AuditFailedException, RecordNotFoundException{
		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource dbGraphTraversalSource = graphFromStore.traversal();

		TinkerGraph graph = (TinkerGraph) entity;
		GraphTraversalSource traversal = graph.traversal();

		// Root node label will be auto-generate if a new entity is created
		String rootNodeLabel;

		if (graphFromStore.features().graph().supportsTransactions()) {
			org.apache.tinkerpop.gremlin.structure.Transaction tx = graphFromStore.tx();
			tx.onReadWrite(org.apache.tinkerpop.gremlin.structure.Transaction.READ_WRITE_BEHAVIOR.AUTO);
			rootNodeLabel = addOrUpdateVerticesAndEdges(dbGraphTraversalSource, traversal, rootLabel, methodOrigin);
			tx.commit();
			// tx.close();
		} else {
			rootNodeLabel = addOrUpdateVerticesAndEdges(dbGraphTraversalSource, traversal, rootLabel, methodOrigin);
		}

		return rootNodeLabel;

	}

	/**
	 * This method creates the root node of the entity if it already isn't present in the graph store
	 * or updates the properties of the root node or adds new properties if the properties are not already
	 * present in the node.
	 * @param dbTraversalSource
	 * @param entitySource
	 * @param rootLabel
	 * @throws EncryptionException 
	 * @throws NoSuchElementException 
	 */
	private String addOrUpdateVerticesAndEdges(GraphTraversalSource dbTraversalSource,
			GraphTraversalSource entitySource, String rootLabel, String methodOrigin)
					throws NoSuchElementException, EncryptionException, AuditFailedException, RecordNotFoundException {

		GraphTraversal<Vertex, Vertex> gts = entitySource.clone().V().hasLabel(rootLabel);
		String label = rootLabel;

		while (gts.hasNext()) {
			Vertex v = gts.next();
			GraphTraversal<Vertex, Vertex> hasLabel = dbTraversalSource.clone().V().hasLabel(rootLabel);

			if (hasLabel.hasNext()) {
				logger.info(String.format("Root node label %s already exists. Updating properties for the root node.", rootLabel));
				Vertex existingVertex = hasLabel.next();
				//if(!methodOrigin.equalsIgnoreCase("upsert")){
					copyProperties(v, existingVertex, methodOrigin);
				//}
				addOrUpdateVertexAndEdge(v, existingVertex, dbTraversalSource, methodOrigin);
			} else {
				if(methodOrigin.equalsIgnoreCase("update") && featureToggling){
					throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
				}
				label = generateBlankNodeLabel(rootLabel);
				logger.info(String.format("Creating entity with label %s", rootLabel));
				Vertex newVertex = dbTraversalSource.clone().addV(label).next();
				copyProperties(v, newVertex,methodOrigin);
				addOrUpdateVertexAndEdge(v, newVertex, dbTraversalSource,methodOrigin);
			}
		}
		return label;
	}

	/**
	 * This method takes the root node of an entity and then recursively creates or updates child vertices
	 * and edges.
	 * @param v
	 * @param dbVertex
	 * @param dbGraph
	 * @throws EncryptionException 
	 * @throws NoSuchElementException 
	 */
	private void addOrUpdateVertexAndEdge(Vertex v, Vertex dbVertex, GraphTraversalSource dbGraph, String methodOrigin)
			throws NoSuchElementException, EncryptionException, AuditFailedException, RecordNotFoundException{
		Iterator<Edge> edges = v.edges(Direction.OUT);
		Iterator<Edge> edgeList = v.edges(Direction.OUT);
		Stack<Pair<Vertex, Vertex>> parsedVertices = new Stack<>();
		List<Edge> dbEdgesForVertex = ImmutableList.copyOf(dbVertex.edges(Direction.OUT));
		List<Edge> edgeVertexMatchList = new ArrayList<Edge>();
		while(edgeList.hasNext()) {
			Edge e = edgeList.next();
			Vertex ver = e.inVertex();
			String edgeLabel = e.label();
			Optional<Edge> edgeVertexAlreadyExists =
					dbEdgesForVertex.stream().filter(ed -> ed.label().equalsIgnoreCase(edgeLabel) && ed.inVertex().label().equalsIgnoreCase(ver.label())).findFirst();
			if(edgeVertexAlreadyExists.isPresent()){
				edgeVertexMatchList.add(edgeVertexAlreadyExists.get());
			}
		}
		logger.info("Matching list size:"+edgeVertexMatchList.size());
			
		while(edges.hasNext()) {
			Edge e = edges.next();
			Vertex ver = e.inVertex();
			String edgeLabel = e.label();
			GraphTraversal<Vertex, Vertex> gt = dbGraph.clone().V().hasLabel(ver.label());
			Optional<Edge> edgeAlreadyExists =
					dbEdgesForVertex.stream().filter(ed -> ed.label().equalsIgnoreCase(e.label())).findFirst();
			Optional<Edge> edgeVertexAlreadyExists =
					dbEdgesForVertex.stream().filter(ed -> ed.label().equalsIgnoreCase(edgeLabel) && ed.inVertex().label().equalsIgnoreCase(ver.label())).findFirst();
			verifyAndDelete(dbVertex, e, edgeAlreadyExists, edgeVertexMatchList, methodOrigin);
			if (gt.hasNext()) {
				Vertex existingV = gt.next();
				logger.info(String.format("Vertex with label %s already exists. Updating properties for the vertex", existingV.label()));
				copyProperties(ver, existingV,methodOrigin);
				if(!edgeVertexAlreadyExists.isPresent()){
					Edge edgeAdded = dbVertex.addEdge(edgeLabel, existingV);
					edgeVertexMatchList.add(edgeAdded);
					AuditRecord record = appContext.getBean(AuditRecord.class);
					record
					.subject(dbVertex.label())
					.predicate(e.label())
					.oldObject(null)
					.newObject(existingV.label())
					.record(databaseProvider);
				}
				parsedVertices.push(new Pair<>(ver, existingV));
			} else {
				if(methodOrigin.equalsIgnoreCase("update") && !isIRI(ver.label()) && featureToggling){
					throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
				}
				String label = generateBlankNodeLabel(ver.label());
				Vertex newV = dbGraph.addV(label).next();
				logger.info(String.format("Adding vertex with label %s and adding properties", newV.label()));
				copyProperties(ver, newV, methodOrigin);
				logger.info(String.format("Adding edge with label %s for the vertex label %s.", e.label(), newV.label()));

				Edge edgeAdded = dbVertex.addEdge(edgeLabel, newV);
				edgeVertexMatchList.add(edgeAdded);
				AuditRecord record = appContext.getBean(AuditRecord.class);
				record
				.subject(dbVertex.label())
				.predicate(e.label())
				.oldObject(null)
				.newObject(newV.label())
				.record(databaseProvider);

				parsedVertices.push(new Pair<>(ver, newV));
			}
		}
		for(Pair<Vertex, Vertex> pv : parsedVertices) {
			addOrUpdateVertexAndEdge(pv.getValue0(), pv.getValue1(), dbGraph,methodOrigin);
		}


	}
	
	/*private void deleteEdgeAndNode(Vertex dbVertex, Edge e, Optional<Edge> edgeAlreadyExists,List<Edge> edgeVertexMatchList, String methodOrigin)
	 throws AuditFailedException, RecordNotFoundException{
		
		Graph graphFromStore = databaseProvider.getGraphStore();
    	GraphTraversalSource traversalSource = graphFromStore.traversal();
    	GraphTraversal<Vertex, Vertex> dbHasLabel = traversalSource.clone().V().hasLabel(dbVertex.label());
    	if (!dbHasLabel.hasNext()) {
    		throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
    	}
    	boolean isSingleValued = schemaConfigurator.isSingleValued(e.label());
    	if(dbHasLabel.hasNext()){
    		Vertex dbSourceVertex = dbHasLabel.next();
    		if (graphFromStore.features().graph().supportsTransactions()) {
    			org.apache.tinkerpop.gremlin.structure.Transaction tx = graphFromStore.tx();
    			tx.onReadWrite(org.apache.tinkerpop.gremlin.structure.Transaction.READ_WRITE_BEHAVIOR.AUTO);
    			deleteEdgeAndNode(isSingleValued, dbSourceVertex, e, edgeAlreadyExists, edgeVertexMatchList, methodOrigin);
    			tx.commit();
    		}else{
    			deleteEdgeAndNode(isSingleValued, dbSourceVertex, e, edgeAlreadyExists, edgeVertexMatchList, methodOrigin);
    		}
    	}
	}*/
	
	/**
	 * This method checks if deletion of edge and node is required based on criteria and invokes deleteEdgeAndNode method
	 * @param dbSourceVertex
	 * @param e
	 * @param edgeAlreadyExists
	 * @param edgeVertexMatchList
	 * @param methodOrigin
	 * @throws AuditFailedException
	 */
	private void verifyAndDelete(Vertex dbSourceVertex, Edge e, Optional<Edge> edgeAlreadyExists,List<Edge> edgeVertexMatchList, String methodOrigin)
			throws AuditFailedException{
		boolean isSingleValued = schemaConfigurator.isSingleValued(e.label());
		if((edgeAlreadyExists.isPresent() && methodOrigin.equalsIgnoreCase("update")) || isSingleValued){
				Iterator<Edge> edgeIter = dbSourceVertex.edges(Direction.OUT, e.label());
				while(edgeIter.hasNext()){
					Edge edge = edgeIter.next();
					Optional<Edge> existingEdgeVertex =
							edgeVertexMatchList.stream().filter(ed -> ed.label().equalsIgnoreCase(edge.label()) && ed.inVertex().label().equalsIgnoreCase(edge.inVertex().label())).findFirst();
					if(!existingEdgeVertex.isPresent()){
						deleteEdgeAndNode(dbSourceVertex, edge, null);
					}
				}
		}
	}
	
	/**
	 * This method deletes the edge and node if the node is an orphan node and if not, deletes only the edge
	 * @param v
	 * @param dbEdgeToBeRemoved
	 * @param dbVertexToBeDeleted
	 * @throws AuditFailedException
	 */
	private void deleteEdgeAndNode(Vertex v, Edge dbEdgeToBeRemoved, Vertex dbVertexToBeDeleted) throws AuditFailedException{
		logger.info("Deleting edge and node:"+dbEdgeToBeRemoved.label());
    	if(dbVertexToBeDeleted == null){
    		dbVertexToBeDeleted = dbEdgeToBeRemoved.inVertex();
    	}
    	Iterator<Edge> inEdgeIter = dbVertexToBeDeleted.edges(Direction.IN);
		Iterator<Edge> outEdgeIter = dbVertexToBeDeleted.edges(Direction.OUT);
		if((inEdgeIter.hasNext() && IteratorUtils.count(inEdgeIter) > 1) || outEdgeIter.hasNext()){
			logger.info("Deleting edge only:"+dbEdgeToBeRemoved.label());
			dbEdgeToBeRemoved.remove();
			
		}else{
			logger.info("Deleting edge and node:"+dbEdgeToBeRemoved.label()+":"+dbVertexToBeDeleted.label());
			dbVertexToBeDeleted.remove();
			dbEdgeToBeRemoved.remove();
		}
		AuditRecord record = appContext.getBean(AuditRecord.class);
		String tailOfdbVertex=v.label().substring(v.label().lastIndexOf("/") + 1).trim();
		String auditVertexlabel= registryContext+tailOfdbVertex;
		record
		.subject(auditVertexlabel)
		.predicate(dbEdgeToBeRemoved.label())
		.oldObject(dbVertexToBeDeleted.label())
		.newObject(null)
		.record(databaseProvider);
		
    }
    
    	
    	

	/**
	 * Blank nodes are no longer supported. If the input data has a blank node, which is identified
	 * by the node's label which starts with :_, then a random UUID is used as the label for the blank node.
	 * @param label
	 * @return
	 */
	private String generateBlankNodeLabel(String label) {
		if(!isIRI(label)){
			label = String.format("%s%s", registryContext, generateRandomUUID());
		}
		return label;
	}

	private boolean isBlankNode(String label){
		if(label.startsWith("_:")) {
			return true;
		}
		return false;
	}
	
	private boolean isIRI(String label){
		UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
		if(urlValidator.isValid(label)) {
			return true;
		}
		return false;
	}

	public static String generateRandomUUID() {
		return UUID.randomUUID().toString();
	}

	@Override
	public boolean updateEntity(Graph entityForUpdate, String rootNodeLabel, String methodOrigin)
			throws RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException {
		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource dbGraphTraversalSource = graphFromStore.traversal();
		TinkerGraph graphForUpdate = (TinkerGraph) entityForUpdate;

		// Check if the root node being updated exists in the database
		GraphTraversal<Vertex, Vertex> hasRootLabel = dbGraphTraversalSource.clone().V().hasLabel(rootNodeLabel);
		if (!hasRootLabel.hasNext()) {
			closeGraph(graphFromStore);
			throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
		} else {
			createOrUpdateEntity(graphForUpdate, rootNodeLabel, methodOrigin);
			closeGraph(graphFromStore);
		}
		return false;
	}


	@Override
	public Graph getEntityById(String label) throws RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException {
		Graph graphFromStore = databaseProvider.getGraphStore();
		GraphTraversalSource traversalSource = graphFromStore.traversal();
		GraphTraversal<Vertex, Vertex> hasLabel = traversalSource.clone().V().hasLabel(label);
		Graph parsedGraph = TinkerGraph.open();
		if (!hasLabel.hasNext()) {
            closeGraph(graphFromStore);
			throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
		} else {					
			Vertex subject = hasLabel.next();
			Vertex newSubject = parsedGraph.addVertex(subject.label());
			copyProperties(subject, newSubject,"read");
			extractGraphFromVertex(parsedGraph,newSubject,subject);
            closeGraph(graphFromStore);
		}
		return parsedGraph;
	}
	

	private void copyProperties(Vertex subject, Vertex newSubject, String methodOrigin)
			throws NoSuchElementException, EncryptionException, AuditFailedException {
		HashMap<String, HashMap<String, String>> propertyMetaPropertyMap = new HashMap<String, HashMap<String, String>>();
		HashMap<String, String> metaPropertyMap;
		Object propertyValue = null;
		Iterator<VertexProperty<Object>> iter = subject.properties();
		while (iter.hasNext()) {
			VertexProperty<Object> property = iter.next();
			String tailOfPropertyKey = property.key().substring(property.key().lastIndexOf("/") + 1).trim();
			boolean existingEncyptedPropertyKey = tailOfPropertyKey
					.substring(0, Math.min(tailOfPropertyKey.length(), 9)).equalsIgnoreCase("encrypted");
			if ((methodOrigin.equalsIgnoreCase("create") || methodOrigin.equalsIgnoreCase("update")) && existingEncyptedPropertyKey) {
				property.remove();
			}
			if ((methodOrigin.equalsIgnoreCase("create") || methodOrigin.equalsIgnoreCase("update")) && schemaConfigurator.isPrivate(property.key())) {
				propertyValue = encryptionService.encrypt(property.value());
				if (!existingEncyptedPropertyKey) {
					String encryptedKey = "encrypted" + tailOfPropertyKey;
					setProperty(newSubject, property.key().replace(tailOfPropertyKey, encryptedKey), propertyValue, methodOrigin);
				}
			} else if (methodOrigin.equalsIgnoreCase("read") && schemaConfigurator.isEncrypted(tailOfPropertyKey)) {
				propertyValue = encryptionService.decrypt(property.value());
				String decryptedKey = property.key().replace(tailOfPropertyKey, tailOfPropertyKey.substring(9));
				setProperty(newSubject, decryptedKey, propertyValue, methodOrigin);

			} else if (isaMetaProperty(property.key())) {
				buildPropertyMetaMap(propertyMetaPropertyMap, property);
			} else {
				if (!(methodOrigin.equalsIgnoreCase("read")
						&& property.key().contains("@audit"))) {
					setProperty(newSubject, property.key(), property.value(), methodOrigin);
				}
			}
			setMetaProperty(subject, newSubject, property, methodOrigin);
		}
		setMetaPropertyFromMap(newSubject, propertyMetaPropertyMap);
	}

    private boolean isaMetaProperty(String key) {
        return key.startsWith(META);
    }

    private void setProperty(Vertex v, String key, Object newValue, String methodOrigin) throws AuditFailedException {
    	VertexProperty vp = v.property(key);
    	Object oldValue = vp.isPresent() ? vp.value() : null;
    	if(oldValue!=null && !methodOrigin.equalsIgnoreCase("update") && !schemaConfigurator.isSingleValued(key)){
    		List valueList = new ArrayList();
			if(oldValue instanceof List){
				valueList = (List)oldValue;
			} else{
				String valueStr = (String)oldValue;
				valueList.add(valueStr);
			}
			
			if(newValue instanceof List){
				valueList.addAll((List)newValue);
			} else{
				valueList.add(newValue);
			}
			newValue = valueList;
    	}
    	v.property(key, newValue);
    	/*if(schemaConfigurator.isSingleValued(key)){
    		System.out.println("Printing value for single-valued:"+newValue);
    		v.property(key, newValue);
    	}else if(oldValue!=null && !methodOrigin.equalsIgnoreCase("update")){
    			//Object value = vp.value();
    			List valueList = new ArrayList();
    			if(oldValue instanceof List){
    				valueList = (List)oldValue;
    			} else{
    				String valueStr = (String)oldValue;
    				valueList.add(valueStr);
    			}
    			valueList.add(newValue);
    			System.out.println("Printing value list:"+valueList);
    			v.property(key, valueList);
    	}else{
    		System.out.println("Printing value for else case:"+newValue);
    		v.property(key, newValue);
    	}*/
    	if (!isaMetaProperty(key) && !Objects.equals(oldValue, newValue)) {
    		GraphTraversal<Vertex, Vertex> configTraversal =
    				v.graph().traversal().clone().V().has(T.label, Constants.GRAPH_GLOBAL_CONFIG);
    		if (configTraversal.hasNext()
    				&& configTraversal.next().property(Constants.PERSISTENT_GRAPH).value().equals(true)) {

    			AuditRecord record = appContext.getBean(AuditRecord.class);
    			record
    			.subject(v.label())
    			.predicate(key)
    			.oldObject(oldValue)
    			.newObject(newValue)
    			.record(databaseProvider);
    		} else {
    			// System.out.println("NOT AUDITING");
    		}
    	} else {
    		// System.out.println("NO CHANGE!");
    	}
    }

    private void setMetaPropertyFromMap(Vertex newSubject, HashMap<String, HashMap<String, String>> propertyMetaPropertyMap) {
        Iterator propertyIter = propertyMetaPropertyMap.entrySet().iterator();
        while(propertyIter.hasNext()){
            Map.Entry pair = (Map.Entry)propertyIter.next();
			logger.info("PROPERTY <- " + pair.getKey());
            HashMap<String,String> _mpmap = (HashMap<String, String>) pair.getValue();
            Iterator _mpmapIter = _mpmap.entrySet().iterator();
            while(_mpmapIter.hasNext()) {
                Map.Entry _pair = (Map.Entry)_mpmapIter.next();
				logger.info("META PROPERTY <- " + _pair.getKey() + "|" + _pair.getValue());
                newSubject.property(pair.getKey().toString()).property(_pair.getKey().toString(),_pair.getValue().toString());
            }
        }
    }

    private void setMetaProperty(Vertex subject, Vertex newSubject, VertexProperty<Object> property, String methodOrigin) throws AuditFailedException {
        if(subject.graph().features().vertex().supportsMetaProperties()) {
            Iterator<Property<Object>> metaPropertyIter = property.properties();
            while (metaPropertyIter.hasNext()) {
                Property<Object> metaProperty = metaPropertyIter.next();
                if (newSubject.graph().features().vertex().supportsMetaProperties()) {
                    newSubject.property(property.key()).property(metaProperty.key(), metaProperty.value());
                } else {
                    String metaKey = getMetaKey(property, metaProperty);
                    setProperty(newSubject,metaKey, metaProperty.value(), methodOrigin);
                }
            }
        }
    }

    private String getMetaKey(VertexProperty<Object> property, Property<Object> metaProperty) {
        return META + property.key() + "." + metaProperty.key();
    }

    private void buildPropertyMetaMap(HashMap<String, HashMap<String, String>> propertyMetaPropertyMap, VertexProperty<Object> property) {
        HashMap<String, String> metaPropertyMap;
        logger.info("META PROPERTY "+property);
        Pattern pattern = Pattern.compile("meta\\.(.*)\\.(.*)");
        Matcher match = pattern.matcher(property.key().toString());
        if(match.find()){
            String _property = match.group(1);
            String _meta_property = match.group(2);
            logger.info("MATCHED meta property "+match.group(1)+ " "+match.group(2));
            if(propertyMetaPropertyMap.containsKey(property.key())){
                logger.info("FOUND in propertyMetaPropertyMap");
                metaPropertyMap = propertyMetaPropertyMap.get(property.key());
            } else {
                logger.info("CREATING metaPropertyMap in propertyMetaPropertyMap");
                metaPropertyMap = new HashMap<>();
                propertyMetaPropertyMap.put(_property,metaPropertyMap);
            }
            metaPropertyMap.put(_meta_property,property.value().toString());
        }
    }

    /*@Override
    public boolean deleteEntity (Graph entity, String rootLabel) throws RecordNotFoundException,AuditFailedException {
    	Graph graphFromStore = databaseProvider.getGraphStore();
    	GraphTraversalSource traversalSource = graphFromStore.traversal();
    	GraphTraversal<Vertex, Vertex> dbHasLabel = traversalSource.clone().V().hasLabel(rootLabel);
    	if (!dbHasLabel.hasNext()) {
    		throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
    	}
    	
    	GraphTraversal<Vertex, Vertex> hasNestedLabel = traversalSource.clone().V().hasLabel(labelToBeDeleted);
    	if (!hasNestedLabel.hasNext()) {
    		throw new RecordNotFoundException(Constants.ENTITY_NOT_FOUND);
    	}
    	TinkerGraph graph = (TinkerGraph) entity;
		GraphTraversalSource traversal = graph.traversal();
    	GraphTraversal<Vertex, Vertex> gts = traversal.clone().V().hasLabel(rootLabel);
    	
    	if (graphFromStore.features().graph().supportsTransactions()) {
    		org.apache.tinkerpop.gremlin.structure.Transaction tx;
    		tx = graphFromStore.tx();
    		tx.onReadWrite(org.apache.tinkerpop.gremlin.structure.Transaction.READ_WRITE_BEHAVIOR.AUTO);
    		deleteEdgeAndNode(dbHasLabel.next(), gts);
    		tx.commit();
    		tx.close();
    	} else {
    		deleteEdgeAndNode(dbHasLabel.next(), gts);
    	}

    	return false;
    }*/

    /*private void deleteEdgeAndNode(Vertex v, GraphTraversal<Vertex, Vertex> gts) throws AuditFailedException{
    	List<Edge> dbEdgesForVertex = ImmutableList.copyOf(v.edges(Direction.OUT));
    	if(gts.hasNext()){
    		Vertex vertex = gts.next();
    		Iterator<Edge> edgeIter = vertex.edges(Direction.OUT);
    		while(edgeIter.hasNext()){
    			Edge e = edgeIter.next();
    			Optional<Edge> edgeAlreadyExists =
    					dbEdgesForVertex.stream().filter(ed -> ed.label().equalsIgnoreCase(e.label())).findFirst();
    			if(edgeAlreadyExists.isPresent()){
    				Vertex vertexToBeDeleted = e.inVertex();
    				Edge dbEdge = edgeAlreadyExists.get();
    				Vertex dbVertexToBeDeleted = dbEdge.inVertex();
    				if(vertexToBeDeleted.label().equalsIgnoreCase(dbVertexToBeDeleted.label())){
    					Iterator<Edge> inEdgeIter = dbVertexToBeDeleted.edges(Direction.IN);
    					Iterator<Edge> outEdgeIter = dbVertexToBeDeleted.edges(Direction.OUT);
    					if(inEdgeIter.hasNext() || outEdgeIter.hasNext()){
    						dbEdge.remove();
    					}else{
    						dbVertexToBeDeleted.remove();
    						dbEdge.remove();
    					}
    					AuditRecord record = appContext.getBean(AuditRecord.class);
    					String tailOfdbVertex=v.label().substring(v.label().lastIndexOf("/") + 1).trim();
    					String auditVertexlabel= registryContext+tailOfdbVertex;
    					record
    					.subject(auditVertexlabel)
    					.predicate(dbEdge.label())
    					.oldObject(dbVertexToBeDeleted.label())
    					.newObject(null)
    					.record(databaseProvider);
    				}
    			}
    		}
    	}

    }*/
    
    
	private void extractGraphFromVertex(Graph parsedGraph,Vertex parsedGraphSubject,Vertex s)
			throws NoSuchElementException, EncryptionException, AuditFailedException {
		Iterator<Edge> edgeIter = s.edges(Direction.OUT);
		Edge edge;
		Stack<Vertex> vStack = new Stack<Vertex>();
		Stack<Vertex> parsedVStack = new Stack<Vertex>();
		while(edgeIter.hasNext()){
			edge = edgeIter.next();
			Vertex o = edge.inVertex();
			Vertex newo = parsedGraph.addVertex(o.label());
			copyProperties(o, newo,"read");
			parsedGraphSubject.addEdge(edge.label(), newo);
			vStack.push(o);
			parsedVStack.push(newo);
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
			e.printStackTrace();
		}
	}

	private AuthInfo getCurrentUserInfo() {
		return (AuthInfo) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}
}
