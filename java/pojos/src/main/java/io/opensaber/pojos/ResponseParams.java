package io.opensaber.pojos;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.opensaber.pojos.Response.Status;

public class ResponseParams {
	public String resmsgid;
	private String msgid;
	private String err;
	private Status status = Status.UNSUCCESSFUL;
	private String errmsg;
	private List<Object> resultList;

	public ResponseParams() {
		this.msgid = UUID.randomUUID().toString();
		this.resmsgid = "";
		this.err = "";
		this.errmsg = "";
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