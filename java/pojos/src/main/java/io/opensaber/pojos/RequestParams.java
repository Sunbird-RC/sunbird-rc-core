package io.opensaber.pojos;

import java.util.UUID;

public class RequestParams {

	private String did;
	private String key;
	private String msgid;

	public RequestParams() {
		this.msgid = UUID.randomUUID().toString();
		this.did = "";
		this.key = "";
	}

	public RequestParams(String did, String key, String msgid) {
		this.msgid = msgid;
		this.did = did;
		this.key = key;
	}

	public String getDid() {
		return did;
	}

	public void setDid(String did) {
		this.did = did;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getMsgid() {
		return msgid;
	}

	public void setMsgid(String msgid) {
		this.msgid = msgid;
	}
}
