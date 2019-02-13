package io.opensaber.pojos;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class Request {

	private String id;
	private String ver;
	private Long ets;
	private RequestParams params;
	@JsonSetter("request")
	private Map<String, Object> requestMap;
	@JsonIgnore
	private String requestMapString;
	@JsonIgnore
	private JsonNode requestMapNode;

	public Request() {
		this.ver = "1.0";
		this.ets = System.currentTimeMillis();
	}

	public Request(RequestParams params, Map<String, Object> requestMap) {
		this.ver = "1.0";
		this.ets = System.currentTimeMillis();
		this.params = params;
		this.requestMap = requestMap;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getVer() {
		return ver;
	}

	public void setVer(String ver) {
		this.ver = ver;
	}

	public Long getEts() {
		return ets;
	}

	public void setEts(Long ets) {
		this.ets = ets;
	}

	public RequestParams getParams() {
		return params;
	}

	public void setParams(RequestParams params) {
		this.params = params;
	}

	@JsonGetter("request")
	public Map<String, Object> getRequestMap() {
		return requestMap;
	}

	/**
	 * Gets the root entity type name for this payload.
	 * @return
	 */
	public String getEntityType() {
		return requestMap.keySet().iterator().next().toString();
	}

	public String getRequestMapAsString() {
		if (requestMapString == null) {
			try {
				requestMapString = new ObjectMapper().writeValueAsString(getRequestMap());
			} catch (JsonProcessingException jpe) {
				requestMapString = "";
			}
		}
		return requestMapString;
	}

	public void setRequestMap(Map<String, Object> requestMap) {
		this.requestMap = requestMap;
	}

	public JsonNode getRequestMapNode() {
		if (requestMapNode == null || requestMapNode.isNull()) {
			requestMapNode = new ObjectMapper().valueToTree(requestMap);
		}
		return requestMapNode;
	}

}
