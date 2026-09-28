## What changed

<!-- A sentence or two on the change itself. -->

**Services touched:** <!-- registry core / identity / credential-schema / credentials / oid4vc / notification / metrics / id-gen / encryption / claim / compose config / docs -->

## Why

Fixes #<!-- issue number -->

- [ ] I commented on the issue to say I was taking it

## How this was tested

- [ ] `make test` passed locally — full build plus both end-to-end configurations
- [ ] Ran part of it only — stated below which, and why
- [ ] Couldn't run it — a maintainer will need to verify

**If you ran the stack:** which compose file and which `test_environments/` file, and what you checked afterwards.

<!--
Container logs help, but redact credentials, tokens, realm secrets and keys first.
-->

## Anything a reviewer should look at closely

<!-- Tag the reviewer or repo owner. Leave blank if nothing stands out. -->

## Contribution Checklist

Link to your filled-in copy: <!-- paste here -->

## Before review

- [ ] Opened against the current working branch, not `main`
- [ ] Tests added or updated for the behaviour changed
- [ ] Documentation updated — anything this made wrong, plus API and configuration docs for anything added
- [ ] No hardcoded hostnames, realm names or ports — those belong in environment configuration
- [ ] Code matches the conventions of the tree it's in (`java/` and `services/` differ)

## Cross-service impact

- [ ] Not applicable — this change is contained to one service
- [ ] API, schema or image changed, and the compose files, `test_environments/` files and dependent services were checked for the same change

<!--
The stack is wired together by docker-compose-v1.yml and the env files under
test_environments/. A changed port, image name or API shape usually needs a
matching edit there.
-->

## Secrets and personal data

- [ ] No `.env`, credentials, tokens, private keys or certificates in the diff
- [ ] No real personal data in fixtures, test schemas or sample registry entries
- [ ] Checked what this change logs, not only what it stores

## Security

- [ ] This change touches authentication, Keycloak configuration, encryption, signing, credential issuance or verification, or personal-data handling in the registry

<!-- If checked, say what below and ask for a security-focused review. -->

<!--
Used AI tools? Declare them on the Contribution Checklist, and add an
`Assisted-by: <tool name>` commit trailer for substantially AI-generated code.
-->