package httpapi.authz

default allow := false

allow {
    some entityName, entityId
    input.method == "POST"
    input.path = ["api", "v1", entityName, entityId]
    user_has_same_entity(entityName)
}

user_has_same_entity (entityName) { token.payload.entity[_] == entityName }

user_email_verified { token.payload.email_verified }

# Helper to get the token payload.
token := {"payload": payload} {
    [header, payload, signature] := io.jwt.decode(input.token)
}
