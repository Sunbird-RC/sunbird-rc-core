# Installation

## Prerequisites

> This guide assumes a some familiarity with basic linux commands. If not,
> [here](https://ubuntu.com/tutorials/command-line-for-beginners#1-overview) is
> a great place to start.

### Terminal emulator

Linux and MacOS will have a terminal installed already. For Windows, it is
recommended that you use `git-bash`, which you can install from
[here](https://git-scm.com/download/win).

Type `echo Hi` in the terminal once it is installed. If installed correctly, you
should see `Hi` appear when you hit enter.

### Git

Installation instructions for Git can be found
[here](https://github.com/git-guides/install-git).

Run `git --version` in the terminal if `git` has been installed correctly:

```sh
$ git --version
git version 2.33.0
```

### NodeJS

Installation instructions for NodeJS can be found
[here](https://nodejs.org/en/download/package-manager/).

Run `node -v` in the terminal if `node` has been installed correctly:

```sh
$ node -v
v16.11.0
```

### Docker

Installation instructions for Docker can be found
[here](https://docs.docker.com/engine/install/).

Run `docker -v` in terminal to check if `docker` has been installed correctly:

```sh
$ docker -v
Docker version 20.10.9, build c2ea9bc90b
```

### Docker Compose

Installation instructions can be found
[here](https://docs.docker.com/engine/install/).

Run `docker-compose -v` in terminal to check if `docker-compose` has been
installed correctly:

```sh
$ docker-compose -v
Docker Compose version 2.0.1
```

## Installing the Registry CLI

To install the Registry CLI, run:

```sh
$ npm install --global registry-cli
```

> In case you encounter a permission denied/access denied error here, prefix the
> command with `sudo`: `sudo npm install --global registry-cli`.

The Registry CLI will be used in this guide for all registry related operations.

# Getting Started

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

This will setup and start a registry using Docker on your machine.

## Understanding schemas

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

## Creating an entity

To create an entity, we need to make the following HTTP request:

```http
POST /api/v1/{entity}/invite HTTP/1.1
Content-Type: application/json


{ "fields...": "values..." }
```

So to create a `Teacher` entity named Pranav Agate who teaches Math at UP Public
School, we would make the following API call:

```sh
curl --location --request POST 'http://localhost:8081/api/v1/Teacher/invite' \
	--header 'Content-Type: application/json' \
	--data-raw '
		{
			"name": "Pranav Agate",
			"phoneNumber": "1234567890",
			"subject": "Math",
			"school": "UP Public School"
		}
	'
```

This should return a JSON object as follows:

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
	"result": { "Teacher": { "osid": "..." } }
}
```

Save the `osid` returned, as we will need it to retrieve or update the entity
later on.

## Authenticating as an entity

Now that we have created an entity, we can authenticate with the server as that
entity to perform further operations like retrieving, searching, updating and
attesting.

To authenticate as an entity, we need to make the following request:

```http
POST /auth/realms/sunbird-rc/protocol/openid-connect/token HTTP/1.1
Content-Type: application/x-www-form-urlencoded

client_id=...&username=...&password=...&grant_type=password
```

So to authenticate as the `Teacher` entity we just created, we would make the
following API call:

```sh
curl --location --request POST 'http://kc:8080/auth/realms/sunbird-rc/protocol/openid-connect/token' \
	--header 'Content-Type: application/x-www-form-urlencoded' \
	--data 'client_id=registry-frontend' \
	--data 'username=1234567890' \
	--data 'password=opensaber@123' \
	--data 'grant_type=password'
```

> Here, `registry-frontend` is the preconfigured client we use to make requests
> to keycloak and `opensaber@123` is the default password for all entities. This
> API call should return a JSON object as follows:

```json
{
	"access_token": "...",
	"expires_in": 600,
	"refresh_expires_in": 600,
	"refresh_token": "...",
	"token_type": "Bearer",
	"not-before-policy": 160238239,
	"session_state": "...",
	"scope": "profile email"
}
```

The `expires_in` field tells us how many seconds we have before the access token
expires and we have to make this request again. Save the access token so we can
use it in future API calls.

## Retrieving an entity

To retrieve an entity, we need to make the following HTTP request:

```http
GET /api/v1/{entity}/{id} HTTP/1.1
Authorization: Bearer {accessToken}
```

So to retrieve the entity we created earlier, we would make the following
request:

```sh
curl --location --request GET 'http://localhost:8081/api/v1/Teacher/...' \
	--header 'Authorization: Bearer ...'
```

> Replace the first `...` above with the entity's `osid` you saved from the
> create entity request. Replace the second `...` with your access token from
> the authentication step.

This will return the entity's JSON representation as follows:

```json
{
	"phoneNumber": "1234567890",
	"school": "UP Public School",
	"subject": "Math",
	"name": "Pranav Agate",
	"osid": "...",
	"osOwner": ["..."]
}
```

The `osOwner` field is the User ID of the entity in Keycloak.

## Updating an entity

To update an entity, we need to make the following HTTP request:

```http
PUT /api/v1/{entity}/{id} HTTP/1.1
Content-Type: application/json
Authorization: Bearer ...

{ "fields...": "values..." }
```

So to update our `Teacher` to teach Biology instead of Math, we would make the
following API call:

```sh
curl --location --request PUT 'http://localhost:8081/api/v1/Teacher/...' \
	--header 'Content-Type: application/json' \
	--header 'Authorization: Bearer ...' \
	--data-raw '
		{
			"name": "Pranav Agate",
			"phoneNumber": "1234567890",
			"subject": "Biology",
			"school": "UP Public School"
		}
	'
```

> Replace the first `...` above with the entity's `osid` you saved from the
> create entity request. Replace the second `...` with your access token from
> the authentication step.

> We need to send the whole entity and not just the updated fields because that
> is how RESTful APIs work. A PUT call should replace the existing record in the
> database with the new object as-is. To know more about this, take a look at
> the accepted answer on
> [this SO question](https://stackoverflow.com/questions/28459418/use-of-put-vs-patch-methods-in-rest-api-real-life-scenarios).
