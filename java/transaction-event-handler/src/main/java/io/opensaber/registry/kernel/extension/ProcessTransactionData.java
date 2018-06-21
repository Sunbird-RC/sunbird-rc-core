package io.opensaber.registry.kernel.extension;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import io.opensaber.registry.kernel.util.LogGraphEvent;
import io.opensaber.registry.middleware.util.Constants;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.LabelEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class ProcessTransactionData {

	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	
	private static Logger logger = LoggerFactory.getLogger(ProcessTransactionData.class);

	protected String graphId;
	protected GraphDatabaseService graphDb;

	public ProcessTransactionData(String graphId, GraphDatabaseService graphDb) {
		this.graphId = graphId;
		this.graphDb = graphDb;
	}

	public void processTxnData(TransactionData data) {
		try {
			List<Map<String, Object>> kafkaMessages = getMessageObj(data);
			if (kafkaMessages != null && !kafkaMessages.isEmpty()) {
				LogGraphEvent.pushMessageToLogger(kafkaMessages);
			}
		} catch (Exception e) {
			logger.error("Exception: " + e.getMessage(), e);
		}
	}

	private List<Map<String, Object>> getMessageObj(TransactionData data) {
		//User id and request id needs to be set here
		//String userId = "";
		String requestId = "";
		List<Map<String, Object>> messageMap = new ArrayList<>();
		messageMap.addAll(getCretedNodeMessages(data, graphDb, requestId));
		messageMap.addAll(getUpdatedNodeMessages(data, graphDb, requestId));
		messageMap.addAll(getDeletedNodeMessages(data, requestId));
		messageMap.addAll(getAddedRelationShipMessages(data, requestId));
		messageMap.addAll(getRemovedRelationShipMessages(data, requestId));
		return messageMap;
	}

	private List<Map<String, Object>> getCretedNodeMessages(TransactionData data, GraphDatabaseService graphDb, String requestId) {
		List<Map<String, Object>> lstMessageMap = new ArrayList<>();
		try {
			List<Long> createdNodeIds = getCreatedNodeIds(data);
			for (Long nodeId : createdNodeIds) {
				Map<String, Object> transactionData = new HashMap<>();
				Map<String, Object> propertiesMap = getAssignedNodePropertyEntry(nodeId, data);
				if (null != propertiesMap && !propertiesMap.isEmpty()) {
					transactionData.put(Constants.GraphParams.properties.name(), propertiesMap);
					Map<String, Object> map = setMessageData(graphDb, nodeId, requestId,
							Constants.GraphParams.CREATE.name(), transactionData);
					lstMessageMap.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error building created nodes message" + e.getMessage(), e);
		}
		return lstMessageMap;
	}

	private List<Map<String, Object>> getUpdatedNodeMessages(TransactionData data, GraphDatabaseService graphDb, String requestId) {
		List<Map<String, Object>> lstMessageMap = new ArrayList<>();
		try {
			List<Long> updatedNodeIds = getUpdatedNodeIds(data);
			for (Long nodeId : updatedNodeIds) {
				Map<String, Object> transactionData = new HashMap<>();
				Map<String, Object> propertiesMap = getAllPropertyEntry(nodeId, data);
				if (null != propertiesMap && !propertiesMap.isEmpty()) {
					transactionData.put(Constants.GraphParams.properties.name(), propertiesMap);
					Map<String, Object> map = setMessageData(graphDb, nodeId, requestId,
							Constants.GraphParams.UPDATE.name(), transactionData);
					lstMessageMap.add(map);
				}
			}
		} catch (Exception e) {
			logger.error("Error building updated nodes message" + e.getMessage(), e);
		}
		return lstMessageMap;
	}

	@SuppressWarnings("rawtypes")
	private List<Map<String, Object>> getDeletedNodeMessages(TransactionData data, String requestId) {
		List<Map<String, Object>> lstMessageMap = new ArrayList<>();
		try {
			List<Long> deletedNodeIds = getDeletedNodeIds(data);
			for (Long nodeId : deletedNodeIds) {
				Map<String, Object> map = new HashMap<>();
				Map<String, Object> transactionData = new HashMap<>();
				Map<String, Object> removedNodeProp = getRemovedNodePropertyEntry(nodeId, data);
				if (null != removedNodeProp && !removedNodeProp.isEmpty()) {
					transactionData.put(Constants.GraphParams.properties.name(), removedNodeProp);
					map.put(Constants.GraphParams.requestId.name(), requestId);
					/*if (StringUtils.isEmpty(userId)) {
						if (removedNodeProp.containsKey("lastUpdatedBy"))
							// oldvalue of lastUpdatedBy from the transaction
							// data as node is deleted
							userId = (String) ((Map) removedNodeProp.get("lastUpdatedBy")).get("ov");
						else
							userId = "ANONYMOUS";
					}*/
					map.put(Constants.GraphParams.userId.name(), getUserId(removedNodeProp));
					map.put(Constants.GraphParams.operationType.name(), Constants.GraphParams.DELETE.name());
					map.put(Constants.GraphParams.label.name(), getRemovedLabel(nodeId, data));
					map.put(Constants.GraphParams.nodeId.name(), nodeId);
					map.put(Constants.GraphParams.createdAt.name(), new Date().getTime());
					map.put(Constants.GraphParams.ets.name(), System.currentTimeMillis());
					map.put(Constants.GraphParams.transactionData.name(), transactionData);
					lstMessageMap.add(map);
				}
			}
		} catch (Exception e) {
			logger.error("Error building deleted nodes message" + e.getMessage(), e);
		}
		return lstMessageMap;
	}

	private Map<String, Object> getAllPropertyEntry(Long nodeId, TransactionData data) {
		Map<String, Object> map = getAssignedNodePropertyEntry(nodeId, data);
		map.putAll(getRemovedNodePropertyEntry(nodeId, data));
		return map;
	}

	private Map<String, Object> getAssignedNodePropertyEntry(Long nodeId, TransactionData data) {
		Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> assignedNodeProp = data.assignedNodeProperties();
		return getNodePropertyEntry(nodeId, assignedNodeProp);
	}

	private String getLastUpdatedByValue(Long nodeId, TransactionData data) {
		Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> assignedNodeProp = data.assignedNodeProperties();
		for (org.neo4j.graphdb.event.PropertyEntry<Node> pe : assignedNodeProp) {
			if (nodeId == pe.entity().getId() && StringUtils.equalsIgnoreCase("lastUpdatedBy", pe.key())) {
				String lastUpdatedBy = (String) pe.value();
				return lastUpdatedBy;
			}
		}
		return null;
	}

	private Map<String, Object> getRemovedNodePropertyEntry(Long nodeId, TransactionData data) {
		Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> removedNodeProp = data.removedNodeProperties();
		return getNodeRemovedPropertyEntry(nodeId, removedNodeProp);
	}

	private Map<String, Object> getNodePropertyEntry(Long nodeId,
			Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> nodeProp) {
		Map<String, Object> map = new HashMap<>();
		for (org.neo4j.graphdb.event.PropertyEntry<Node> pe : nodeProp) {
			if (nodeId == pe.entity().getId() && !compareValues(pe.previouslyCommitedValue(), pe.value())) {
				Map<String, Object> valueMap = new HashMap<>();
				valueMap.put("ov", pe.previouslyCommitedValue()); // old
				// value
				valueMap.put("nv", pe.value()); // new value
				map.put(pe.key(), valueMap);
			}
		}
		if (map.size() == 1 && null != map.get(Constants.AuditProperties.lastUpdatedAt.name()))
			map = new HashMap<>();
		return map;
	}
	
	private String getRemovedLabel(Long nodeId, TransactionData data){
		Iterable<LabelEntry> removedLabels = data.removedLabels();
		for (LabelEntry le : removedLabels) {
			if(nodeId == le.node().getId()){
				return le.label().toString();
			}
		}
		return "";
	}

	private Map<String, Object> getNodeRemovedPropertyEntry(Long nodeId,
			Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> nodeProp) {
		Map<String, Object> map = new HashMap<>();
		for (org.neo4j.graphdb.event.PropertyEntry<Node> pe : nodeProp) {
			if (nodeId == pe.entity().getId()) {
				Map<String, Object> valueMap = new HashMap<>();
				valueMap.put("ov", pe.previouslyCommitedValue()); // old value
				valueMap.put("nv", null); // new value
				map.put(pe.key(), valueMap);
			}
		}
		if (map.size() == 1 && null != map.get(Constants.AuditProperties.lastUpdatedAt.name()))
			map = new HashMap<String, Object>();
		return map;
	}

	@SuppressWarnings("rawtypes")
	private boolean compareValues(Object o1, Object o2) {
		if (null == o1)
			o1 = "";
		if (null == o2)
			o2 = "";
		if (o1.equals(o2))
			return true;
		else {
			if (o1 instanceof List) {
				if (!(o2 instanceof List))
					return false;
				else
					return compareLists((List) o1, (List) o2);
			} else if (o1 instanceof Object[]) {
				if (!(o2 instanceof Object[]))
					return false;
				else
					return compareArrays((Object[]) o1, (Object[]) o2);
			}
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private boolean compareLists(List l1, List l2) {
		if (l1.size() != l2.size())
			return false;
		for (int i = 0; i < l1.size(); i++) {
			Object v1 = l1.get(i);
			Object v2 = l2.get(i);
			if ((null == v1 && null != v2) || (null != v1 && null == v2))
				return false;
			if (null != v1 && null != v2 && !v1.equals(v2))
				return false;
		}
		return true;
	}

	private boolean compareArrays(Object[] l1, Object[] l2) {
		if (l1.length != l2.length)
			return false;
		for (int i = 0; i < l1.length; i++) {
			Object v1 = l1[i];
			Object v2 = l2[i];
			if ((null == v1 && null != v2) || (null != v1 && null == v2))
				return false;
			if (null != v1 && null != v2 && !v1.equals(v2))
				return false;
		}
		return true;
	}


	private List<Map<String, Object>> getAddedRelationShipMessages(TransactionData data, String requestId) {
		Iterable<Relationship> createdRelations = data.createdRelationships();
		return getRelationShipMessages(createdRelations, Constants.GraphParams.UPDATE.name(), false, requestId, null);
	}

	private List<Map<String, Object>> getRemovedRelationShipMessages(TransactionData data, String requestId) {
		Iterable<Relationship> deletedRelations = data.deletedRelationships();
		Iterable<org.neo4j.graphdb.event.PropertyEntry<Relationship>> removedRelationshipProp = data
				.removedRelationshipProperties();
		return getRelationShipMessages(deletedRelations, Constants.GraphParams.UPDATE.name(), true, requestId,
				removedRelationshipProp);
	}

	private List<Map<String, Object>> getRelationShipMessages(Iterable<Relationship> relations, String operationType,
			boolean delete, String requestId,
			Iterable<org.neo4j.graphdb.event.PropertyEntry<Relationship>> removedRelationshipProp) {
		List<Map<String, Object>> lstMessageMap = new ArrayList<>();
		try {
			if (null != relations) {
				for (Relationship rel : relations) {
					Node startNode = rel.getStartNode();
					Node endNode = rel.getEndNode();
					Map<String, Object> relMetadata = null;
					if (delete)
						relMetadata = getRelationShipPropertyEntry(rel.getId(), removedRelationshipProp);
					else
						relMetadata = rel.getAllProperties();
					String relationTypeName = rel.getType().name();
					// start_node message
					Map<String, Object> map = null;
					Map<String, Object> transactionData = new HashMap<>();
					Map<String, Object> startRelation = new HashMap<>();

					startRelation.put("rel", relationTypeName);
					startRelation.put("dir", "OUT");
					startRelation.put("label", getLabel(endNode));
					startRelation.put("relMetadata", relMetadata);

					/*if (StringUtils.isEmpty(userId)) {
						String startNodeLastUpdate = (String) getPropertyValue(startNode, "lastUpdatedAt");
						String endNodeLastUpdate = (String) getPropertyValue(endNode, "lastUpdatedAt");

						if (startNodeLastUpdate != null && endNodeLastUpdate != null) {
							if (startNodeLastUpdate.compareTo(endNodeLastUpdate) > 0) {
								userId = (String) getPropertyValue(startNode, "lastUpdatedBy");
							} else {
								userId = (String) getPropertyValue(endNode, "lastUpdatedBy");
							}
						}
						if (StringUtils.isBlank(userId))
							userId = "ANONYMOUS";
					}*/
					List<Map<String, Object>> startRelations = new ArrayList<>();
					startRelations.add(startRelation);
					transactionData.put(Constants.GraphParams.properties.name(), new HashMap<String, Object>());
					if (delete) {
						transactionData.put(Constants.GraphParams.removedRelations.name(), startRelations);
						transactionData.put(Constants.GraphParams.addedRelations.name(), new ArrayList<Map<String, Object>>());
					} else {
						transactionData.put(Constants.GraphParams.addedRelations.name(), startRelations);
						transactionData.put(Constants.GraphParams.removedRelations.name(),
								new ArrayList<Map<String, Object>>());
					}

					map = setMessageData(graphDb, startNode.getId(), requestId, operationType, transactionData);
					lstMessageMap.add(map);

					// end_node message
					map = null;
					transactionData = new HashMap<>();
					Map<String, Object> endRelation = new HashMap<>();

					endRelation.put("rel", relationTypeName);
					endRelation.put("dir", "IN");
					endRelation.put("label", getLabel(startNode));
					endRelation.put("relMetadata", relMetadata);
					List<Map<String, Object>> endRelations = new ArrayList<>();
					endRelations.add(endRelation);
					transactionData.put(Constants.GraphParams.properties.name(), new HashMap<String, Object>());
					if (delete) {
						transactionData.put(Constants.GraphParams.removedRelations.name(), endRelations);
						transactionData.put(Constants.GraphParams.addedRelations.name(), new ArrayList<Map<String, Object>>());
					} else {
						transactionData.put(Constants.GraphParams.addedRelations.name(), endRelations);
						transactionData.put(Constants.GraphParams.removedRelations.name(),
								new ArrayList<Map<String, Object>>());
					}

					map = setMessageData(graphDb, endNode.getId(), requestId, operationType, transactionData);
					lstMessageMap.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error building updated relations message" + e.getMessage(), e);
		}
		return lstMessageMap;
	}

	private Map<String, Object> getRelationShipPropertyEntry(Long relId,
			Iterable<org.neo4j.graphdb.event.PropertyEntry<Relationship>> relProp) {
		Map<String, Object> map = new HashMap<>();
		for (org.neo4j.graphdb.event.PropertyEntry<Relationship> pe : relProp) {
			if (relId == pe.entity().getId()) {
				if (pe.previouslyCommitedValue() != null) {
					map.put(pe.key(), pe.previouslyCommitedValue());
				}
			}
		}
		return map;
	}

	private String getLabel(Node node) {
		for(Label label: node.getLabels()){
			return label.toString();
		}
		return "";
	}

	private Object getPropertyValue(Node node, String propertyName) {
		if (node.hasProperty(propertyName))
			return node.getProperty(propertyName);
		return null;
	}

	private List<Long> getUpdatedNodeIds(TransactionData data) {
		List<Long> lstNodeIds = new ArrayList<>();
		List<Long> lstCreatedNodeIds = getCreatedNodeIds(data);
		List<Long> lstDeletedNodeIds = getDeletedNodeIds(data);
		Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> assignedNodeProp = data.assignedNodeProperties();
		for (org.neo4j.graphdb.event.PropertyEntry<Node> pe : assignedNodeProp) {
			if (!lstCreatedNodeIds.contains(pe.entity().getId()) && !lstDeletedNodeIds.contains(pe.entity().getId())) {
				lstNodeIds.add(pe.entity().getId());
			}
		}
		Iterable<org.neo4j.graphdb.event.PropertyEntry<Node>> removedNodeProp = data.removedNodeProperties();
		for (org.neo4j.graphdb.event.PropertyEntry<Node> pe : removedNodeProp) {
			if (!lstCreatedNodeIds.contains(pe.entity().getId()) && !lstDeletedNodeIds.contains(pe.entity().getId())) {
				lstNodeIds.add(pe.entity().getId());
			}
		}
		return new ArrayList<>(new HashSet<>(lstNodeIds));
	}

	private List<Long> getCreatedNodeIds(TransactionData data) {
		List<Long> lstNodeIds = new ArrayList<>();
		if (null != data.createdNodes()) {
			Iterator<Node> nodes = data.createdNodes().iterator();
			while (nodes.hasNext()) {
				lstNodeIds.add(nodes.next().getId());
			}
		}

		return new ArrayList<>(new HashSet<>(lstNodeIds));
	}

	private List<Long> getDeletedNodeIds(TransactionData data) {
		List<Long> lstNodeIds = new ArrayList<>();
		if (null != data.deletedNodes()) {
			Iterator<Node> nodes = data.deletedNodes().iterator();
			while (nodes.hasNext()) {
				lstNodeIds.add(nodes.next().getId());
			}
		}

		return new ArrayList<>(new HashSet<>(lstNodeIds));
	}

	private Map<String, Object> setMessageData(GraphDatabaseService graphDb, Long nodeId, String requestId,
											   String operationType, Map<String, Object> transactionData) {

		Map<String, Object> map = new HashMap<>();
		Node node = graphDb.getNodeById(nodeId);
		map.put(Constants.GraphParams.requestId.name(), requestId);
		map.put(Constants.GraphParams.userId.name(), getUserId(node));
		map.put(Constants.GraphParams.operationType.name(), operationType);
		map.put(Constants.GraphParams.label.name(), getLabel(node));
		// map.put(Constants.GraphParams.createdAt.name(), format(new Date()));
		map.put(Constants.GraphParams.createdAt.name(), new Date().getTime());
		map.put(Constants.GraphParams.ets.name(), System.currentTimeMillis());
		map.put(Constants.GraphParams.nodeId.name(), nodeId);
		map.put(Constants.GraphParams.transactionData.name(), transactionData);
		return map;

	}

	private static String format(Date date) {
		if (null != date) {
			try {
				return sdf.format(date);
			} catch (Exception e) {
			}
		}
		return null;
	}
	
	public String getUserId(Node node){
		List<String> propertyKeys = Lists.newArrayList(node.getPropertyKeys());
		for(String property: propertyKeys){
			if(property.endsWith("lastUpdatedBy")){
				return (String) node.getProperty(property);
			}
		}
		/*if (node.hasProperty("lastUpdatedBy")){
			return (String) node.getProperty("lastUpdatedBy");
		}*/
		return "ANONYMOUS";
	}
	
	public String getUserId(Long nodeId, TransactionData data){
		String lastUpdatedBy = getLastUpdatedByValue(nodeId, data);
		if (StringUtils.isNotBlank(lastUpdatedBy)) {
			return lastUpdatedBy;
		}
		return "ANONYMOUS";
	}
	
	public String getUserId(Map<String, Object> removedNodeProp){
		if (removedNodeProp.containsKey("lastUpdatedBy"))
		{
			return (String) ((Map) removedNodeProp.get("lastUpdatedBy")).get("ov");
		}
		return "ANONYMOUS";
	}
}