package dev.sunbirdrc.auth.keycloak;

public class ResponseWrapper {
    private final javax.ws.rs.core.Response javaxResponse;


    public ResponseWrapper(javax.ws.rs.core.Response javaxResponse) {
        this.javaxResponse = javaxResponse;
    }

    public jakarta.ws.rs.core.Response toJakartaResponse() {
        return jakarta.ws.rs.core.Response.status(javaxResponse.getStatus())
                .entity(javaxResponse.getEntity())
                .location(javaxResponse.getLocation())
                .build();
    }
}
