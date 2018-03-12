package io.opensaber.pojos;

import java.util.Map;

public class Response {
	private String id;
	private String ver;
	private Long ets;	
	private ResponseParams params;
	public enum Status	
	{
	    SUCCCESSFUL,UNSUCCESSFUL;
	};
	private String responseCode;
	private Map<String, Object> result;	

	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
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
	public ResponseParams getParams() {
		return params;
	}
	public void setParams(ResponseParams params) {
		this.params = params;
	}
	public Map<String, Object> getResult() {
		return result;
	}
	public void setResult(Map<String, Object> result) {
		this.result = result;
	}	
	public String getResponseCode() {
		return responseCode;
	}
}
