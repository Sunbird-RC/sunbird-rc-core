package io.opensaber.pojos;

import io.opensaber.pojos.Response.Status;

public class ResponseParams{
		public String resmsgid;
		private String msgid;
		private Status status = Status.UNSUCCESSFUL;
		private String err;
		private String errmsg;
		private String result;
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
		public String getResult() {
			return result;
		}
		public void setResult(String result) {
			this.result = result;
		}
	}