# Configuring Consent

The Registry CLI can auto-create client scopes and mappers through a
configuration file. Here is a sample configuration file creating a 'school'
scope that grants access to the entity's school attribute:

```json
{
	"scopes": [
		{
			"name": "school",
			"description": "Access the entity's school name",
			"protocol": "openid-connect",
			"mapper": {
				"type": "oidc-usermodel-property-mapper",
				"attribute": {
					"path": "school",
					"type": "String"
				}
			}
		}
	]
}
```

The `scopes` top-level field must exist and must be an array.

Each scope is an object as follows:

```jsonc
{
	// Name of the scope
	"name": "<...>",
	// Description of what the scope grants the client access to
	"description": "<...>",
	// Protocol, preferably keep it openid-connect unless you know what you are
	// doing
	"protocol": "openid-connect",
	// Mapping details - they tell keycloak what to include in the access token
	// once the entity consents to grant access to the client
	"mapper": {
		// What kind of mapper to create
		// Currently only oidc-usermodel-property-mapper is supported
		"type": "oidc-usermodel-property-mapper",
		// Which field this grants the client access to
		"attribute": {
			// Path to the field in the entity schema
			// Separate nested fields with a '.', e.g.: address.pincode
			"path": "<...>",
			// Type of the field
			// Can be one of the following: 'String', 'int', 'long', 'boolean' and
			// 'JSON' if it is an object
			"type": "<...>"
		}
	}
}
```

Enter the path to this file while creating the registry to auto-create the
client scope and mappers.
