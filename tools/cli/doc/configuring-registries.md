# Configuring Registries Through The CLI

The Registry CLI allows you to setup a registry instance interactively on your
machine. You can configure the following while setting up the registry instance:

- name of the registry
- name of the keycloak realm
- keycloak admin client ID
- keycloak frontend client ID
- keycloak admin account credentials
- entity schemas to use
- consent configuration

## Registry Name

The name of the registry.

Defaults to the name of the current directory in
[start case](https://github.com/infinitered/gluegun/blob/master/docs/toolbox-strings.md#startcase).

## Keycloak Realm Name

Name of the realm in Keycloak, the auth service used by Sunbird RC.

Defaults to `sunbird-rc`.

## Keycloak Admin Client ID

Client ID of the client the registry should use to interact with keycloak.

Defaults to `admin-api`.

## Keycloak Frontend Client ID

Client ID of the client a registry client should use to interact with keycloak.

Defaults to `registry-frontend`.

## Keycloak Admin Account Credentials

Username and password of the administrator account in keycloak.

Defaults to username `admin` and password `admin`.

## Entity Schemas To Use

Path to a directory that contains the schemas for entities in the registry.

Defaults to a sample student-teacher schema that you can
[view on Github](https://github.com/Sunbird-RC/sunbird-rc-core/tree/main/tools/cli/src/templates/config/schemas).
These sample schemas are extremely minimal and intended mainly for the getting
started guide, it is highly recommended to use your own schemas. Take a look at
[this](https://docs.sunbirdrc.dev/creating-your-own-schemas) guide to learn how
to create your own schema.

## Consent Configuration

Path to the file that specifies scopes and mappers to create in keycloak.

Defaults to a sample configuration that you can
[view on Github](https://github.com/Sunbird-RC/sunbird-rc-core/blob/main/tools/cli/src/templates/config/consent.json).
This sample configuration is extremely minimal and intended mainly for the
getting started guide, it is highly recommended to use your own configuration.
Take a look at [this](guides/configuring-consent) guide to learn how to
configure the CLI to auto-create client scopes and mappers.
