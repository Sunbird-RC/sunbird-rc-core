package dev.sunbirdrc.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
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

    public enum API_ID {
		CREATE(getApiPrefix() + ".create"),
		INVITE(getApiPrefix() + ".invite"),
		READ(getApiPrefix() + ".read"),
		UPDATE(getApiPrefix() + ".update"),
		AUDIT(getApiPrefix() + ".audit"),
		HEALTH(getApiPrefix() + ".health"),
		DELETE(getApiPrefix() + ".delete"),

		GET(getApiPrefix() + ".get"),
		PATCH(getApiPrefix() + ".patch"),
		PUT(getApiPrefix() + ".put"),
		SEARCH(getApiPrefix() + ".search"),

		POST(getApiPrefix() + ".post"),
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
