package dev.sunbirdrc.pojos;

import dev.sunbirdrc.pojos.Response.Status;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
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

}