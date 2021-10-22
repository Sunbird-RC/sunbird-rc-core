# Getting Started

Now that you have the Registry CLI installed, we can create a new instance of a
registry!

Run the following command in a directory where you wish to setup the registry.
For this example, we will use `~/Registries/example/`. (`~` is short form for
the user's home directory).

> Before creating the registry, add the following line at the end of
> `/etc/hosts` (on Windows, it is `c:\windows\system32\drivers\etc\hosts`) if
> you haven't already: `127.0.0.1 kc` (excluding quotes).

```sh
# Create and move into the ~/Registries/example directory
$ mkdir -p ~/Registries/example
$ cd ~/Registries/example
# Create a registry instance
$ registry init
```

> Don't copy-paste the `$` signs, they indicate that what follows is a terminal
> command

This will setup and start a registry using Docker on your machine.

> The default setup files used to create a registry can be found
> [here](https://github.com/gamemaker1/registry-setup-files). If you wish to use
> a different set of files to setup your registry, specify the remote git repo's
> URL using the `config` command before running the `init` command:
>
> ```
> $ registry config setup.repo <url-to-setup-repo>
> ```
>
> You will also need to change the container names and images if you have
> changed them in `docker-compose.yaml`:
>
> ```
> $ registry config container.images <comma-separated-image-names>
> $ registry config container.names <comma-separated-container-names>
> ```

## Understanding Schemas

Each registry can store data as entities. The example registry setup by the CLI
already has two entities declared in it: `Teacher` and `Student`. To view the
schemas, run the following:

```sh
$ cat schemas/student.json
$ cat schemas/teacher.json
```

> Or you could view the schemas
> [on Github](https://github.com/gamemaker1/registry-setup-files/tree/main/schemas).

A teacher is represented by 5 fields: `name`, `phoneNumber`, `email`, `subject`
and `school`, out of which `name`, `phoneNumber`, `email` and `school` are
required fields. `subject` can be any one of Math, Hindi, English, History,
Geography, Physics, Chemistry, and Biology.

A student is represented by 4 fields: `name`, `phoneNumber`, `email`, and
`school`, all of which are required fields. When setting `school` field, you are
making a 'claim' (i.e., that the student is from the specified school), which
can be 'attested' (i.e., confirmed to be true) by a teacher from the same
school.

## Creating An Entity

To create an entity, we need to make the following HTTP request:

### Request

```http
POST /api/v1/{entity}/invite
```

| Field          | In     | Type     | Description                  |
| -------------- | ------ | -------- | ---------------------------- |
| `content-type` | header | `string` | Set to `application/json`    |
| `entity`       | path   | `string` | The type of entity to create |
| `...`          | body   | `object` | The entity's data            |

### Examples

So to create a `Teacher` entity named Pranav Agate who teaches Math at UP Public
School, we would make the following API call:

**cURL**

```sh
curl --location \
	--request 'POST' \
	--header 'content-type: application/json' \
	--data-raw '{
		"name": "Pranav Agate",
		"phoneNumber": "1234567890",
		"email": "pranav@upps.in",
		"subject": "Math",
		"school": "UP Public School"
	}' \
	'http://localhost:8081/api/v1/Teacher/invite'
```

**HTTPie**

```sh
echo '{
	"name": "Pranav Agate",
	"phoneNumber": "1234567890",
	"email": "pranav@upps.in",
	"subject": "Math",
	"school": "UP Public School"
}' | http post \
	'http://localhost:8081/api/v1/Teacher/invite' \
	'content-type: application/json'
```

### Response

This will store the entity in the registry and return the following object:

```json
{
	"id": "open-saber.registry.invite",
	"ver": "1.0",
	"ets": 1634198998956,
	"params": {
		"resmsgid": "",
		"msgid": "3ee6a76f-d6c8-4262-a7ee-ddbe66fcb127",
		"err": "",
		"status": "SUCCESSFUL",
		"errmsg": ""
	},
	"responseCode": "OK",
	"result": { "Teacher": { "osid": "1-9d6099fc-2c01-4714-bceb-55ff28c482f9" } }
}
```

Important variables in the response body:

| Field                  | In   | Type     | Description                                                                                    |
| ---------------------- | ---- | -------- | ---------------------------------------------------------------------------------------------- |
| `result.{entity}.osid` | body | `string` | The ID of the create entity in the registry, used for retrieval and modification of the entity |

## Authenticating As An Entity

Now that we have created an entity, we can authenticate with the server as that
entity to perform further operations like retrieving, searching, updating and
attesting.

### Request

To authenticate as an entity, we need to make the following request:

```http
POST /auth/realms/sunbird-rc/protocol/openid-connect/token
```

| Field          | In     | Type     | Description                                                                                 |
| -------------- | ------ | -------- | ------------------------------------------------------------------------------------------- |
| `content-type` | header | `string` | Set to `application/x-www-form-urlencoded`                                                  |
| `client_id`    | body   | `string` | Set to `registry-frontend`                                                                  |
| `username`     | body   | `string` | The `_osConfig.ownershipAttributes.userId` of the entity according to the schema            |
| `password`     | body   | `string` | Set to `opensaber@123` (default password, specified in registry config/docker compose file) |
| `grant_type`   | body   | `string` | Set to `password`                                                                           |

### Examples

So to authenticate as the `Teacher` entity we just created, we would make the
following API call:

**cURL**

```sh
curl --location \
	--request POST \
	--header 'content-type: application/x-www-form-urlencoded' \
	--data 'client_id=registry-frontend' \
	--data 'username=1234567890' \
	--data 'password=opensaber@123' \
	--data 'grant_type=password' \
	'http://kc:8080/auth/realms/sunbird-rc/protocol/openid-connect/token'
```

**HTTPie**

```sh
http --form post \
	'http://kc:8080/auth/realms/sunbird-rc/protocol/openid-connect/token' \
	'content-type: application/x-www-form-urlencoded' \
	'client_id=registry-frontend' \
	'username=1234567890' \
	'password=opensaber@123' \
	'grant_type=password'
```

> Here, `registry-frontend` is the preconfigured client we use to make requests
> to keycloak and `opensaber@123` is the default password for all entities.

### Response

This API call should return a JSON object as follows:

```json
{
	"access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lk...2cSSaBKuB58I2OYDGw",
	"expires_in": 300,
	"not-before-policy": 0,
	"refresh_expires_in": 1800,
	"refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lk...9HulwVv12bBDUdU_nidZXo",
	"scope": "email profile",
	"session_state": "300f8a46-e430-4fd6-92aa-a2d337d7343e",
	"token_type": "Bearer"
}
```

Important variables in the response body:

| Field          | In   | Type     | Description                                                        |
| -------------- | ---- | -------- | ------------------------------------------------------------------ |
| `access_token` | body | `string` | Access token used to retrieve/update entity                        |
| `expires_in`   | body | `number` | Number of seconds before the access token will be declared invalid |
| `token_type`   | body | `string` | Should be `Bearer`, else we have gotten the wrong token            |
| `scope`        | body | `string` | Using this token, what information we can access about the entity  |

## Retrieving An Entity

### Request

To retrieve an entity, we need to make the following HTTP request:

```http
GET /api/v1/{entity}/{id} HTTP/1.1
```

| Field           | In     | Type     | Description                                                                                                    |
| --------------- | ------ | -------- | -------------------------------------------------------------------------------------------------------------- |
| `content-type`  | header | `string` | Set to `application/json`                                                                                      |
| `authorization` | header | `string` | Set to `bearer {access-token}` (substitute the access token for the one retrieved in the authentication step ) |
| `entity`        | path   | `string` | The type of entity to retrieve                                                                                 |
| `id`            | path   | `string` | The ID of the entity to retrieve                                                                               |

### Examples

So to retrieve the entity we created earlier, we would make the following
request:

**cURL**

```sh
curl --location \
	--request GET \
	--header 'content-type: application/json' \
	--header 'authorization: bearer {access-token}' \
	'http://localhost:8081/api/v1/Teacher/{id}'
```

**HTTPie**

```sh
http get \
	'http://localhost:8081/api/v1/Teacher/{id}' \
	'authorization: bearer {access-token}'
```

> Replace the `{id}` above with the entity's `osid` you saved from the create
> entity request. Replace the `{access-token}` with the `Teacher` entity's
> access token from the authentication step.

### Response

This will return the entity's JSON representation as follows:

```json
{
	"phoneNumber": "1234567890",
	"school": "UP Public School",
	"subject": "Math",
	"name": "Pranav Agate",
	"osid": "{id}",
	"osOwner": ["{owner-id}"],
	"_osState/school": "DRAFT"
}
```

Important variables in the response body:

| Field              | In   | Type     | Description                                                                                                                                                                                                                            |
| ------------------ | ---- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `osOwner`          | body | `string` | User ID of the entity in Keycloak.                                                                                                                                                                                                     |
| `_osState/{field}` | body | `string` | State of an attestable field. Can be `DRAFT` (when it has not been sent for attestation), `ATTESTATION_REQUESTED` (when sent for attestation), `PUBLISHED` (when successfully attested) and `REJECTED` (when rejected by the attestor) |

## Update An Entity

To update an entity, we need to make the following HTTP request:

### Request

```http
PUT /api/v1/{entity}/{id}
```

| Field          | In     | Type     | Description                  |
| -------------- | ------ | -------- | ---------------------------- |
| `content-type` | header | `string` | Set to `application/json`    |
| `entity`       | path   | `string` | The type of entity to modify |
| `id`           | id     | `string` | The ID of entity to modify   |
| `...`          | body   | `object` | The entity's data            |

### Examples

So to update the subject our `Teacher` entity Pranav Agate teaches to `Biology`,
we would make the following API call:

**cURL**

```sh
curl --location \
	--request 'PUT' \
	--header 'content-type: application/json' \
	--header 'authorization: bearer {access-token}' \
	--data-raw '{
		"name": "Pranav Agate",
		"phoneNumber": "1234567890",
		"email": "pranav@upps.in",
		"subject": "Biology",
		"school": "UP Public School"
	}' \
	'http://localhost:8081/api/v1/Teacher/{id}'
```

**HTTPie**

```sh
echo '{
	"name": "Pranav Agate",
	"phoneNumber": "1234567890",
	"email": "pranav@upps.in",
	"subject": "Biology",
	"school": "UP Public School"
}' | http put \
	'http://localhost:8081/api/v1/Teacher/{id}' \
	'content-type: application/json' \
	'authorization: bearer {access-token}'
```

> Replace the `{id}` above with the entity's `osid` you saved from the create
> entity request. Replace the `{access-token}` with the `Teacher` entity's
> access token from the authentication step.

> We need to send the whole entity and not just the updated fields because that
> is how RESTful APIs work. A PUT call should replace the existing record in the
> database with the new object as-is. To know more about this, take a look at
> the accepted answer on
> [this SO question](https://stackoverflow.com/questions/28459418/use-of-put-vs-patch-methods-in-rest-api-real-life-scenarios).

### Response

This will update the entity in the registry and return the following object:

```json
{
	"id": "open-saber.registry.update",
	"ver": "1.0",
	"ets": 1634371946769,
	"params": {
		"resmsgid": "",
		"msgid": "d51e6e6a-027d-4a42-84bb-2ce00e31d993",
		"err": "",
		"status": "SUCCESSFUL",
		"errmsg": ""
	},
	"responseCode": "OK"
}
```

## Make A Claim

To make a claim and send it for attestation, we need to make the following
request:

### Request

```http
PUT /api/v1/{entity}/{id}/{field}?send=true
```

| Field          | In     | Type      | Description                                          |
| -------------- | ------ | --------- | ---------------------------------------------------- |
| `content-type` | header | `string`  | Set to `application/json`                            |
| `entity`       | path   | `string`  | The type of entity to modify                         |
| `id`           | path   | `string`  | The ID of entity to modify                           |
| `field`        | path   | `string`  | The field whose value we are sending for attestation |
| `send`         | query  | `boolean` | Set to `true`                                        |
| `...`          | body   | `any`     | The value of the claim                               |

### Examples

First, let us create a `Student` entity named Prashant Joshi who also goes to UP
Public School:

**cURL**

```sh
curl --location \
	--request 'POST' \
	--header 'content-type: application/json' \
	--data-raw '{
		"name": "Prashant Joshi",
		"phoneNumber": "9876543210",
		"email": "prashant@upps.in",
		"school": "UP Public School"
	}' \
	'http://localhost:8081/api/v1/Student/invite'
```

**HTTPie**

```sh
echo '{
	"name": "Prashant Joshi",
	"phoneNumber": "9876543210",
	"email": "prashant@upps.in",
	"school": "UP Public School"
}' | http post \
	'http://localhost:8081/api/v1/Student/invite' \
	'content-type: application/json'
```

Next, we can get an access token for Prashant by making a POST request to the
authentication server. See the
[Authenticating As An Entity](#authenticating-as-an-entity) section to know how
to do that.

Then, we can send the claim (that Prashant is a student at UP Public School) for
attestation by making the following request:

```sh
curl --location \
	--request 'PUT' \
	--header 'content-type: application/json' \
	--header 'authorization: bearer {access-token}' \
	--data-raw '"UP Public School"' \
	'http://localhost:8081/api/v1/Student/{id}/school?send=true'
```

**HTTPie**

```sh
echo '"UP Public School"' | http put \
	'http://localhost:8081/api/v1/Student/{id}/school' \
	'content-type: application/json' \
	'authorization: bearer {access-token}' \
	'send==true'
```

> Replace the `{id}` above with the entity's `osid` you saved from the create
> entity request. Replace the `{access-token}` with the `Student` entity's
> access token from the authentication step.

### Response

This will send the claim for attestation and return the following object:

```json
{
	"id": "open-saber.registry.update",
	"ver": "1.0",
	"ets": 1634371946769,
	"params": {
		"resmsgid": "",
		"msgid": "ksi38Dsl-8dIw-492j-6vlS-84KRe0Csop35",
		"err": "",
		"status": "SUCCESSFUL",
		"errmsg": ""
	},
	"responseCode": "OK"
}
```

If you retrieve the `Student` entity by following the
[Retrieving An Entity section](#retrieving-an-entity), you will get the
following object in response:

```json
{
	"email": "prashant@upps.in",
	"name": "Prashant Joshi",
	"phoneNumber": "9876543210",
	"school": "UP Public School",
	"osid": "{id}",
	"osOwner": ["{owner-id}"],
	"_osClaimId/school": "{claim-id}",
	"_osState/school": "ATTESTATION_REQUESTED"
}
```

Important variables in the response body:

| Field                | In   | Type     | Description                                                                                                                                                                                                                            |
| -------------------- | ---- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `osOwner`            | body | `string` | User ID of the entity in Keycloak.                                                                                                                                                                                                     |
| `_osState/{field}`   | body | `string` | State of an attestable field. Can be `DRAFT` (when it has not been sent for attestation), `ATTESTATION_REQUESTED` (when sent for attestation), `PUBLISHED` (when successfully attested) and `REJECTED` (when rejected by the attestor) |
| `_osClaimId/{field}` | body | `string` | ID of the claim made on a field, required when attesting the claim.                                                                                                                                                                    |

## Attest/Reject A Claim

To attest/reject a claim, we need to make the following request:

### Request

```http
POST /api/v1/{entity}/claims/{claim-id}/attest
```

| Field          | In     | Type     | Description                                                                     |
| -------------- | ------ | -------- | ------------------------------------------------------------------------------- |
| `content-type` | header | `string` | Set to `application/json`                                                       |
| `entity`       | path   | `string` | The attestor entity types                                                       |
| `claim-id`     | path   | `string` | The ID of the claim to attest                                                   |
| `action`       | body   | `string` | Set to `GRANT_CLAIM` to attest the claim and `REJECT_CLAIM` to reject the claim |
| `notes`        | body   | `string` | Attestation related details (optional)                                          |

### Examples

To attest the claim we made in the previous section (that Prashant is a student
at UP Public School), we make the following request:

```sh
curl --location \
	--request 'POST' \
	--header 'content-type: application/json' \
	--header 'authorization: bearer {access-token}' \
	--data-raw '{
		"action": "GRANT_CLAIM"
	}' \
	'http://localhost:8081/api/v1/Teacher/claims/{claim-id}/attest'
```

**HTTPie**

```sh
echo '{
	"action": "GRANT_CLAIM"
}' | http post \
	'http://localhost:8081/api/v1/Teacher/claims/{claim-id}/attest' \
	'content-type: application/json' \
	'authorization: bearer {access-token}'
```

> Replace the `{claim-id}` above with the `_osClaimId/school` you saved from the
> make a claim request. Replace the `{access-token}` with the `Teacher` entity's
> access token from the authentication step.

### Response

This will attest/reject the claim and return a blank HTTP 200 response.
