package io.opensaber.pojos;

import io.opensaber.pojos.Response.Status;

import java.util.List;
import java.util.UUID;

public class ResponseParams {
	public String resmsgid;
	private String msgid;
	private String err;
	private Status status;
	private String errmsg;
	private List<Object> resultList;

	public ResponseParams() {
		this.msgid = UUID.randomUUID().toString();
		this.resmsgid = "";
		this.err = "";
		this.errmsg = "";
		this.status = Status.SUCCESSFUL; // When there is no error, treat status as success
	}

	public String getResmsgid() {
		return resmsgid;
	}

	public void setResmsgid(String resmsgid) {
		this.resmsgid = resmsgid;
	}

	public String getMsgid() {
		return msgid;
	}

	public void setMsgid(String msgid) {
		this.msgid = msgid;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getErr() {
		return err;
	}

	public void setErr(String err) {
		this.err = err;
	}

	public String getErrmsg() {
		return errmsg;
	}

	public void setErrmsg(String errmsg) {
		this.errmsg = errmsg;
	}

	public List<Object> getResultList() {
		return resultList;
	}

	public void setResultList(List<Object> resultList) {
		this.resultList = resultList;
	}
}