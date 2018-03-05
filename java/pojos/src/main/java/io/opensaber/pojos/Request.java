package io.opensaber.pojos;

import java.util.List;
import java.util.Map;

public class Request {

	private String id;
	private String ver;
	private Long ets;	
	private RequestParams params;
	private Map<String, Object> requestMap;
	private List<Object> requestList;
		
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
	
	public Map<String, Object> getRequestMap() {
		return requestMap;
	}
	public void setRequestMap(Map<String, Object> requestMap) {
		this.requestMap = requestMap;
	}
	public List<Object> getRequestList() {
		return requestList;
	}
	public void setRequestList(List<Object> requestList) {
		this.requestList = requestList;
	}
}
