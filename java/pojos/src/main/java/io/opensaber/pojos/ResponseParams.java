package io.opensaber.pojos;

import java.util.List;
import java.util.Map;

import io.opensaber.pojos.Response.Status;

public class ResponseParams{
		public String resmsgid;
		private String msgid;
		private Status status = Status.UNSUCCESSFUL;
		private String err;
		private String errmsg;
		private Map<String, Object> resultMap;
		private List<Object> resultList;
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
		public Map<String, Object> getResultMap() {
			return resultMap;
		}
		public void setResultMap(Map<String, Object> resultMap) {
			this.resultMap = resultMap;
		}
		public List<Object> getResultList() {
			return resultList;
		}
		public void setResultList(List<Object> resultList) {
			this.resultList = resultList;
		}
	}