# Security Policy

## Supported Versions

Security fixes land on the latest minor release. Older lines get fixes only for
critical issues, and only while an adopter is still on them.

| Version | Supported |
|---------|-----------|
| 2.1.x   | Yes |
| 2.0.x   | Critical fixes only |
| < 2.0   | No |

## Reporting a Vulnerability

**Do not open a public issue for a security problem.**

Use [Security → Report a vulnerability](https://github.com/Sunbird-RC/sunbird-rc-core/security/advisories/new)
to open a private advisory. Only the reporter and the maintainers can see it.

Include whatever you have:

| | |
|---|---|
| **Component** | Which service — registry, claim, identity, credentials, credential-schema, oid4vc, encryption, id-gen, notification, metrics |
| **Version** | Release tag or commit, and the Docker Compose file / env file you ran |
| **Impact** | What an attacker gets — data disclosure, privilege escalation, forged credential, denial of service |
| **Reproduction** | Requests, payloads or configuration that trigger it. Redact real credentials, tokens and keys |

## What to Expect

| Stage | Target |
|---|---|
| Acknowledgement | 5 working days |
| Initial assessment and severity | 10 working days |
| Fix or mitigation plan | Agreed with you on the advisory, driven by severity |
| Disclosure | Coordinated — advisory published with the release that carries the fix, crediting you unless you prefer otherwise |

Please give maintainers a reasonable window to fix and release before disclosing
publicly.

## Scope

In scope: this repository — the registry core, the services in `services/`, and
the deployment configuration shipped here.

Out of scope, report upstream instead:

- Third-party components we deploy but do not maintain — Keycloak, PostgreSQL,
  Elasticsearch, Kafka, ClickHouse, Vault
- Issues in a different Sunbird RC repository — report them in that repository
- Findings against a specific adopter's deployment rather than this codebase —
  contact that operator

The sample applications under `ui-sample/` are demonstration code, not a
supported deployment target.
