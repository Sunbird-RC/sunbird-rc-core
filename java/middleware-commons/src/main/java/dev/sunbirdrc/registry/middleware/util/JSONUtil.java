package dev.sunbirdrc.registry.middleware.util;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.sunbirdrc.registry.middleware.util.Constants.*;

public class JSONUtil {

	private final static String KEY_NULL_ERROR = "key cannot be null or empty";
	private static final String EMPTY = "";
	private static Logger logger = LoggerFactory.getLogger(JSONUtil.class);
	private static Type stringObjMapType = new TypeToken<Map<String, Object>>() {
	}.getType();
	private static String key = "";
	private static ObjectMapper objectMapper = new ObjectMapper();

	public static Map<String, Object> convertObjectJsonMap(Object object) {
		Gson gson = new Gson();
		String result = gson.toJson(object);
		return gson.fromJson(result, stringObjMapType);
	}

	public static String convertObjectJsonString(Object object) throws JsonProcessingException {
		String result = new ObjectMapper().writeValueAsString(object);
		return result;
	}
	
	public static JsonNode convertStringJsonNode(String jsonStr) throws IOException {
		JsonNode jNode = new ObjectMapper().readTree(jsonStr);
		return jNode;
	}
	
	public static JsonNode convertObjectJsonNode(Object object) throws IOException {
		JsonNode inputNode = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL).valueToTree(object);

		return inputNode;
	}

	public static Map<String, Object> convertJsonNodeToMap(JsonNode object) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> mapObject = new ObjectMapper().convertValue(object, Map.class);
		return mapObject;
	}

	public static String getStringWithReplacedText(String payload, String value, String replacement) {
		Pattern pattern = Pattern.compile(value);
		Matcher matcher = pattern.matcher(payload);
		return matcher.replaceAll(replacement);
	}

	public static Map<String, Object> frameJsonAndRemoveIds(String regex, String json, Gson gson, String frame)
			throws JsonLdError, IOException {
		Map<String, Object> reqMap = gson.fromJson(json, stringObjMapType);
		JsonObject jsonObj = gson.fromJson(json, JsonObject.class);
		String rootType = null;
		if (jsonObj.get("@graph") != null) {
			rootType = jsonObj.get("@graph").getAsJsonArray().get(0).getAsJsonObject().get("@type").getAsString();
		} else {
			rootType = jsonObj.get("@type").getAsString();
		}
		String replacedframe = frame.replace("<@type>", rootType);
		// JsonUtils.fromString(frame)
		JsonLdOptions options = new JsonLdOptions();
		options.setCompactArrays(true);
		Map<String, Object> framedJsonLD = JsonLdProcessor.frame(reqMap, JsonUtils.fromString(replacedframe), options);
		// json = gson.toJson(framedJsonLD);
		String jsonld = JSONUtil.getStringWithReplacedText(gson.toJson(framedJsonLD), regex, EMPTY);
		logger.info("frameJsonAndRemoveIds: json - ", jsonld);
		return gson.fromJson(jsonld, stringObjMapType);
	}

	/**
	 * Returns true if the passed in string is a valid json
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isJsonString(String str) {
		boolean isJson = false;
		try {
			final ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(str);
			// At least one key is expected
			if (node.fieldNames().hasNext()) {
				isJson = true;
			}
		} catch (IOException e) {
			isJson = false;
		}
		return isJson;
	}

	/**
	 * Field value to replace by new text. Replace node by given text to Parent's
	 * hierarchy. Field will not be added if not found existing already
	 * 
	 * @param parent
	 * @param fieldName
	 * @param newValue
	 */
	public static void replaceField(ObjectNode parent, String fieldName, String newValue) {
		if (parent.has(fieldName)) {
			parent.put(fieldName, newValue);
		}
		parent.fields().forEachRemaining(entry -> {
			JsonNode entryValue = entry.getValue();
			if (entryValue.isArray()) {
				for (int i = 0; i < entryValue.size(); i++) {
					if (entry.getValue().get(i).isObject())
						replaceField((ObjectNode) entry.getValue().get(i), fieldName, newValue);
				}
			} else if (entryValue.isObject()) {
				replaceField((ObjectNode) entry.getValue(), fieldName, newValue);
			}
		});
	}

	/**
	 * Trimming a given prefix if present from each TextNode value in parent's
	 * hierarchy (including nested objects).
	 * 
	 * @param parent
	 * @param prefix
	 */
	public static void trimPrefix(ObjectNode parent, String prefix) {

		parent.fields().forEachRemaining(entry -> {
			JsonNode entryValue = entry.getValue();
			if (entryValue.isValueNode() && entryValue.asText("").startsWith(prefix)) {
				parent.put(entry.getKey(), entry.getValue().asText().replaceFirst(prefix, ""));
			} else if (entryValue.isArray()) {
				for (int i = 0; i < entryValue.size(); i++) {
					if (entry.getValue().get(i).isObject())
						trimPrefix((ObjectNode) entry.getValue().get(i), prefix);
				}
			} else if (entryValue.isObject()) {
				trimPrefix((ObjectNode) entry.getValue(), prefix);
			}
		});
	}

	/**
	 * Add prefix to given keys present in the parent's hierarchy.
	 * 
	 * @param parent
	 * @param prefix
	 * @param keys
	 */
	public static void addPrefix(ObjectNode parent, String prefix, List<String> keys) {

		parent.fields().forEachRemaining(entry -> {
			JsonNode entryValue = entry.getValue();
			if (entryValue.isValueNode() && keys.contains(entry.getKey())) {
				String defaultValue = prefix + entryValue.asText();
				parent.put(entry.getKey(), defaultValue);
			} else if (entryValue.isArray()) {
				ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
				for (int i = 0; i < entryValue.size(); i++) {
					if (entryValue.get(i).isTextual() && keys.contains(entry.getKey()))
						arrayNode.add(prefix + entryValue.get(i).asText());
					else if(entryValue.get(i).isObject())
						addPrefix((ObjectNode) entryValue.get(i), prefix, keys);
				}
				if (arrayNode.size() > 0)
					parent.set(entry.getKey(), arrayNode);
			} else if (entryValue.isObject()) {
				addPrefix((ObjectNode) entry.getValue(), prefix, keys);
			}
		});
	}

	/**
	 * Adding a child node to Parent's hierarchy.
	 * 
	 * @param parent
	 * @param childKey
	 * @param child
	 */
	public static void addNode(ObjectNode parent, String childKey, ObjectNode child) {
		parent.fields().forEachRemaining(entry -> {
			JsonNode entryValue = entry.getValue();
			if (entryValue.isObject()) {
				addNode((ObjectNode) entry.getValue(), childKey, child);
			}
			if (entryValue.isArray()) {
				for (int i = 0; i < entryValue.size(); i++) {
					if (entry.getValue().get(i).isObject())
						addNode((ObjectNode) entry.getValue().get(i), childKey, child);
				}
			}

		});
		if (childKey == null || childKey.isEmpty())
			throw new IllegalArgumentException(KEY_NULL_ERROR);
		parent.set(childKey, child);

	}
	/**
	 * Adding field(key, value) to Parent's hierarchy 
	 * 
	 * @param parent
	 * @param childKey      field key
	 * @param child         field value
	 */
    public static void addField(ObjectNode parent, String childKey, String child) {
        parent.fields().forEachRemaining(entry -> {
            JsonNode entryValue = entry.getValue();
            if (entryValue.isObject()) {
                addField((ObjectNode) entry.getValue(), childKey, child);
            }
            if (entryValue.isArray()) {
                for (int i = 0; i < entryValue.size(); i++) {
                    if (entry.getValue().get(i).isObject())
                        addField((ObjectNode) entry.getValue().get(i), childKey, child);
                }
            }
        });
        if (childKey == null || childKey.isEmpty())
            throw new IllegalArgumentException(KEY_NULL_ERROR);
        parent.put(childKey, child);

    }

	public static void addField(ObjectNode parent, String childKey, List<String> child) {
		parent.fields().forEachRemaining(entry -> {
			JsonNode entryValue = entry.getValue();
			if (entryValue.isObject()) {
				addField((ObjectNode) entry.getValue(), childKey, child);
			}
			if (entryValue.isArray()) {
				for (int i = 0; i < entryValue.size(); i++) {
					if (entry.getValue().get(i).isObject())
						addField((ObjectNode) entry.getValue().get(i), childKey, child);
				}
			}
		});
		if (childKey == null || childKey.isEmpty())
			throw new IllegalArgumentException(KEY_NULL_ERROR);
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
		for (String node : child) {
			arrayNode.add(node);
		}
		parent.set(childKey, arrayNode);
	}

	/**
	 * Remove a node of given key from parent's hierarchy(including nested objects)
	 * 
	 * @param parent
	 * @param removeKey
	 */
	public static void removeNode(ObjectNode parent, String removeKey) {
		parent.fields().forEachRemaining(entry -> {

			JsonNode entryValue = entry.getValue();
			if (entryValue.isArray()) {
				for (int i = 0; i < entryValue.size(); i++) {
					if (entry.getValue().get(i).isObject())
						removeNode((ObjectNode) entry.getValue().get(i), removeKey);
				}
			} else if (entryValue.isObject()) {
				removeNode((ObjectNode) entry.getValue(), removeKey);
			}

		});
		parent.remove(removeKey);
	}

	public static JsonNode removeNodesByPath(JsonNode root, Set<String> nodePaths) throws IOException {
		DocumentContext doc = JsonPath.parse(convertObjectJsonString(root));
		for (String jsonPath : nodePaths) {
			try {
				doc.delete(jsonPath);
			} catch (Exception e) {
				logger.error("Path not found {} {}", jsonPath, ExceptionUtils.getStackTrace(e));
			}
		}
		return convertStringJsonNode(doc.jsonString());
	}

	/**
	 * Remove list of nodes given from parent's hierarchy(including nested objects
	 * too)
	 * 
	 * @param parent
	 * @param removeKeys
	 */
	public static void removeNodes(ObjectNode parent, List<String> removeKeys) {
		List<String> removeKeyNames = new ArrayList<String>();
		parent.fields().forEachRemaining(entry -> {
			if (removeKeys.contains(entry.getKey())) {
				removeKeyNames.add(entry.getKey());
			} else {
				JsonNode entryValue = entry.getValue();
				if (entryValue.isArray()) {
					for (int i = 0; i < entryValue.size(); i++) {
						if (entry.getValue().get(i).isObject())
							removeNodes((ObjectNode) entry.getValue().get(i), removeKeys);
					}
				} else if (entryValue.isObject()) {
					removeNodes((ObjectNode) entry.getValue(), removeKeys);
				}
			}
		});
		parent.remove(removeKeyNames);
	}
	/**
	 * Find a key from hierarchy of JsonNode of given value
	 * @param node
	 * @param value
	 * @return
	 */
	public static String findKey(JsonNode node, String value) {
		Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			if (entry.getValue().isTextual() && entry.getValue().textValue().equalsIgnoreCase(value)) {
				key = entry.getKey();
				break;
			} else if (entry.getValue().isObject()) {
				findKey(entry.getValue(), value);
			}
		}
		return key;
	}

	/**
	 * Returns the Json Node associated with the key
	 * It may be that the specified key is not found in the input. If so, searches for
	 * the next higher key parent and returns node.
	 * Example:
	 *  Consider inputNode as {a:{b:{c:d}}}
	 *  key = a/b/c, will return c
	 *  key = a/b/e, will return b, so that callers can set b to include e (addition).
	 *
	 * @param inputNode
	 * @param key
	 * @return
	 */
	private static ObjectNode getNode(ObjectNode inputNode, String key) {
		JsonNode result = inputNode.at(key);
		if (result.isMissingNode()) {
			return getNode(inputNode, key.substring(0, key.lastIndexOf("/")));
		}
		return (ObjectNode) result;
	}

	/**
	 * Iterates the inputNode and merges values with the result node. Takes care to add
	 * new elements that are found in inputNode, but missing in the result.
	 * @param entityTypeJsonPtr - the entity type
	 * @param result - existing
	 * @param inputNode - new to be updated
	 * @param ignoreFields - fields that must not be updated
	 */
	public static void merge(String entityTypeJsonPtr, ObjectNode result, ObjectNode inputNode, List<String> ignoreFields) {
		inputNode.fields().forEachRemaining(prop -> {
			String propKey = prop.getKey();
			JsonNode propValue = prop.getValue();

			if ((propValue.isValueNode() && !ignoreFields.contains(propKey)) ||
					propValue.isArray()) {
				// Must be a value node and not a uuidPropertyName key pair
				//((ObjectNode)result.get(entityType)).set(propKey, propValue);
				getNode(result, entityTypeJsonPtr).set(propKey, propValue);
			} else if (propValue.isObject()) {
				if (result.at(entityTypeJsonPtr + "/" + propKey).isMissingNode()) {
					((ObjectNode) result.at(entityTypeJsonPtr)).set(propKey, propValue);
				} else {
					merge(entityTypeJsonPtr + "/" + propKey, result, (ObjectNode) propValue, ignoreFields);
				}
			}
		});
	}

	/** This method checks difference between 2 json-nodes and filter out patch as json node
	 *
	 * @param existingNode
	 * @param latestNode
	 * @return
	 */
	public static JsonNode diffJsonNode(JsonNode existingNode, JsonNode latestNode) {

		final JsonToken existingNodeToken = existingNode.asToken();
		final JsonToken latestNodeToken = latestNode.asToken();

		if (existingNodeToken == JsonToken.NOT_AVAILABLE) {
			existingNode = null;
		}
		if (latestNodeToken == JsonToken.NOT_AVAILABLE) {
			latestNode = null;
		}
        return JsonDiff.asJson(existingNode, latestNode);
	}


	/**
	 * Trimming a given prefix if present from each TextNode value corresponding to
	 * the fieldName in parent's hierarchy (including nested objects).
	 * 
	 * @param parent
	 * @param prefix
	 */
	public static void trimPrefix(ObjectNode parent, String fieldName, String prefix) {

		parent.fields().forEachRemaining(entry -> {
			JsonNode entryValue = entry.getValue();

			if ( entry.getKey().equals(fieldName) && entryValue.isValueNode() && entryValue.asText("").startsWith(prefix)) {
				parent.put(entry.getKey(), entry.getValue().asText().replaceFirst(prefix, ""));

			} else if (entryValue.isArray()) {
				for (int i = 0; i < entryValue.size(); i++) {
					if (entry.getValue().get(i).isObject())
						trimPrefix((ObjectNode) entry.getValue().get(i), fieldName, prefix);
				}
			} else if (entryValue.isObject()) {
				trimPrefix((ObjectNode) entry.getValue(), fieldName, prefix);
			}

		});
	}

	public static void removeNodes(JsonNode node, List<String> backList) {
		if (node.isArray()) {
			for (JsonNode child: node) {
				removeNodes(child, backList);
			}
		} else if (node.isObject()){
			removeNodes((ObjectNode) node, backList);
		}
	}

	public static void replaceFieldByPointerPath(JsonNode node, String jsonPointer, JsonNode value) {
		if (value != null && jsonPointer != null) {
			jsonPointer = convertToJsonPointerPath(jsonPointer);
			((ObjectNode) node.at(jsonPointer.substring(0, jsonPointer.lastIndexOf("/")))).put(jsonPointer.substring(jsonPointer.lastIndexOf("/") + 1), value);
		}
	}

	public static String convertToJsonPointerPath(String docPath) {
		if(docPath != null && docPath.startsWith("$.")) {
			return docPath.replace("$", "").replaceAll("\\.", "/");
		}
		return docPath;
	}

    public static String readValFromJsonTree(String path, JsonNode input) {
        Configuration alwaysReturnListConfig = Configuration.builder().options(Option.ALWAYS_RETURN_LIST).build();
        List<String> typeList = JsonPath.using(alwaysReturnListConfig).parse(input.toString()).read(path);
        return typeList.get(0);
    }

	public static String getUUIDPropertyValueFromArrNode(String uuidPropertyName, String propertiesUUIDKeyName, JsonNode resultNode, JsonNode requestBody) {
		if (resultNode.isArray()) {
			ArrayNode arrayNode = (ArrayNode) resultNode;
			JsonNode matchingClaim = searchClaimUUIDPropertyValueFromRequestProperties(propertiesUUIDKeyName, arrayNode,requestBody);
			return matchingClaim != null ? matchingClaim.get(uuidPropertyName).asText() : "";
		}
		return "";
	}

	private static JsonNode searchClaimUUIDPropertyValueFromRequestProperties(String propertiesUUIDKeyName, ArrayNode arrayNode, JsonNode requestBody) {
		if (requestBody.get(propertiesUUIDKeyName) != null) {
			Map<String, List<String>> requestBodyProperty = objectMapper.convertValue(requestBody.get(propertiesUUIDKeyName), Map.class);
			Iterator<JsonNode> claimIterator = arrayNode.elements();
			while (claimIterator.hasNext()) {
				JsonNode claimEntry = claimIterator.next();
				if (claimEntry.get(propertiesUUIDKeyName) != null) {
					JsonNode property = claimEntry.get(propertiesUUIDKeyName);
					Map<String, JsonNode> claimEntryProperty = objectMapper.convertValue(property, Map.class);
					if (isRequestBodyPropertyPresentInClaim(requestBodyProperty, claimEntryProperty)) {
						return claimEntry;
					}
				}
			}
		}
		List<JsonNode> nodeList = new ArrayList<>();
		arrayNode.elements().forEachRemaining(nodeList::add);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		nodeList.sort(Comparator.comparing(node -> {
			String dateString = node.get("osCreatedAt").asText();
			LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
			return dateTime;
		}));
		return nodeList.get(nodeList.size() -1 );
	}

	private static boolean isRequestBodyPropertyPresentInClaim(Map<String,List<String>> requestBodyProperty,Map<String,JsonNode> claimEntryProperty){
		for(Map.Entry<String,List<String>> entry: requestBodyProperty.entrySet()){
			List<String> requestBodyPropertyUUID = entry.getValue();
			String key = entry.getKey();
			if(!((List<String>) objectMapper.convertValue(claimEntryProperty.get(key), List.class)).containsAll(requestBodyPropertyUUID)){
				return false;
			}
		}
		return true;
	}

	public static JsonNode extractPropertyDataFromEntity(String uuidPropertyName, JsonNode entityNode, Map<String, String> attestationProperties, Map<String, List<String>> propertiesUUIDMapper) {
		if(attestationProperties == null) {
			return JsonNodeFactory.instance.nullNode();
		}
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		DocumentContext documentContext = JsonPath.parse(entityNode.toString());
		for(Map.Entry<String, String> entry : attestationProperties.entrySet()) {
			String path = entry.getValue();
			String key = entry.getKey();
			Object read = documentContext.read(path);
			JsonNode readNode = new ObjectMapper().convertValue(read, JsonNode.class);
			result.set(key, readNode);
			if(readNode.isArray() && propertiesUUIDMapper != null && propertiesUUIDMapper.containsKey(key)) {
				List<String> uuidPropertyValues = propertiesUUIDMapper.get(key);
				ArrayNode arrayNode = (ArrayNode) readNode;
				ArrayNode filteredArrNode = JsonNodeFactory.instance.arrayNode();

				for(JsonNode node :arrayNode) {
					if(node.has(uuidPropertyName) && uuidPropertyValues.contains(node.get(uuidPropertyName).asText())) {
						filteredArrNode.add(node);
					}
				}
				result.set(key, filteredArrNode);
			}
		}
		return result;
	}

	public static ObjectNode getSearchPageUrls(JsonNode inputNode, long defaultLimit, long defaultOffset, long totalCount, String url) throws IOException {
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		JsonNode searchNode = objectMapper.readTree(inputNode.toString());
		long limit = searchNode.get(LIMIT) == null ? defaultLimit : searchNode.get(LIMIT).asLong(defaultLimit);
		long offset = searchNode.get(OFFSET) == null ? defaultOffset : searchNode.get(OFFSET).asLong(defaultOffset);
		((ObjectNode) searchNode).set(OFFSET, JsonNodeFactory.instance.numberNode(offset - limit));
		String prevPageToken = Base64.getEncoder().encodeToString(searchNode.toString().getBytes(StandardCharsets.UTF_8));
		((ObjectNode) searchNode).set(OFFSET, JsonNodeFactory.instance.numberNode(offset + limit));
		String nextPageToken = Base64.getEncoder().encodeToString(searchNode.toString().getBytes(StandardCharsets.UTF_8));
		if(offset - limit >=0) result.put(PREV_PAGE, url + "?search=" + prevPageToken);
		if(offset + limit < totalCount) result.put(NEXT_PAGE, url + "?search=" + nextPageToken);
		return result;
	}

	public static ObjectNode getRootSearchPageUrls(JsonNode inputNode, long defaultLimit, long defaultOffset, long totalCount, String url) throws IOException {
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		JsonNode searchNode = objectMapper.readTree(inputNode.toString());
		String entityName = searchNode.fieldNames().next();
		long limit = searchNode.get(entityName).get(LIMIT) == null ? defaultLimit : searchNode.get(entityName).get(LIMIT).asLong(defaultLimit);
		long offset = searchNode.get(entityName).get(OFFSET) == null ? defaultOffset : searchNode.get(entityName).get(OFFSET).asLong(defaultOffset);
		((ObjectNode) searchNode.at("/" + entityName)).set(OFFSET, JsonNodeFactory.instance.numberNode(offset - limit));
		String prevPageToken = Base64.getEncoder().encodeToString(searchNode.toString().getBytes(StandardCharsets.UTF_8));
		((ObjectNode) searchNode.at("/" + entityName)).set(OFFSET, JsonNodeFactory.instance.numberNode(offset + limit));
		String nextPageToken = Base64.getEncoder().encodeToString(searchNode.toString().getBytes(StandardCharsets.UTF_8));
		if(offset - limit >=0) result.put(PREV_PAGE, url + "?search=" + prevPageToken);
		if(offset + limit < totalCount) result.put(NEXT_PAGE, url + "?search=" + nextPageToken);
		return result;
	}

	public static ObjectNode parseSearchToken(String endcodedValue) {
		try {
			byte[] decoded = Base64.getDecoder().decode(endcodedValue);
			return (ObjectNode) objectMapper.readTree(decoded);
		} catch (Exception ignored) {
			logger.warn("Unable to parse next page token");
		}
		return null;
	}
}
