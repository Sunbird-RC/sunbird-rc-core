package dev.sunbirdrc.pojos;

public class Response {
	public static final String API_NAME = "sunbird-rc";

	private static String getApiName() {
		if ("true".equals(System.getenv("registry_base_apis_enable")))
			return "open" + "-sa" + "ber"; //legacy compatibility
		return API_NAME;
	}

	private String id;
	private String ver;
	private Long ets;
	private ResponseParams params;
	private String responseCode;
	private Object result;

	public Response() {
		this.ver = "1.0";
		this.ets = System.currentTimeMillis();
	}

	public Response(API_ID apiId, String httpStatus, ResponseParams responseParams) {
		this.ver = "1.0";
		this.ets = System.currentTimeMillis();
		this.id = apiId.getId();
		this.responseCode = httpStatus;
		this.params = responseParams;
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

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public String getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

	public enum API_ID {
		CREATE(getApiPrefix() + ".create"),
		INVITE(getApiPrefix() + ".invite"),
		READ(getApiPrefix() + ".read"),
		UPDATE(getApiPrefix() + ".update"),
		AUDIT(getApiPrefix() + ".audit"),
		HEALTH(getApiPrefix() + ".health"),
		DELETE(getApiPrefix() + ".delete"),

		PUT(getApiPrefix() + ".put"),

		POST(getApiPrefix() + ".post"),

		GET(getApiPrefix() + ".get"),

		PATCH(getApiPrefix() + ".patch"),
		SEARCH(getApiPrefix() + ".search"),

		SIGN(getApiName() + ".utils.sign"),
		VERIFY(getApiName() + ".utils.verify"),
		KEYS(getApiName() + ".utils.keys"),
		ENCRYPT(getApiName() + ".utils.encrypt"),
		DECRYPT(getApiName() + ".utils.decrypt"),
        SEND(getApiName() + ".registry.send"),
		REVOKE(getApiName() + ".utils.revoke"),
		NONE("");
        private String id;

		private API_ID(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}

	private static String getApiPrefix() {
		return getApiName() + ".registry";
	}

	public enum Status {
		SUCCESSFUL, UNSUCCESSFUL;
	}

}
