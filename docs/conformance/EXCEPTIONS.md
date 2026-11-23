# Exceptions and unsupported scenarios

Everything listed here is a known gap in the release gates in [README.md](README.md).
Each entry says what is not covered, why, and what removes it. Remove the entry when it's resolved.

Last reviewed: 2026-11-23.

## Security

### Fixable CRITICAL dependency vulnerabilities (release blocker)

A Trivy filesystem scan of the repository on 2026-11-23 found 19 fixable CRITICAL CVEs in 11 packages.

- To be fixed (upgrade, not ignored), 13 CVEs: `handlebars`, `proxy-addr`, `form-data`,
  `org.apache.tomcat.embed:tomcat-embed-core`, `io.netty:netty-handler`. Until these are upgraded,
  `make scan-images` (the release image scan) is expected to fail.
- Accepted temporarily, 6 CVEs: listed in `.trivyignore` and below. Each entry has an `exp:` date;
  after it, Trivy reports the CVE again and the release scan fails.
- A finding is only accepted by adding its ID to `.trivyignore` and adding a row here:

| ID | Package | Image | Reason accepted | Owner | Review by |
|---|---|---|---|---|---|
| CVE-2026-6270 | `@fastify/middie` 8.3.0 (fixed in 9.3.2) | identity-service, credential-schema, credentials-service, oid4vc-service | Major upgrade through the NestJS Fastify adapter; needs its own PR and full test run | Kartheek | 2027-01-31 |
| CVE-2016-1000027 | `spring-web` 5.3.39 (fixed in 6.0.0) | sunbird-rc-core, claim-ms, id-gen-service, encryption-service | Affects `HttpInvoker`, which the repo does not use; fix requires Spring Boot 3 / Java 17 | Kartheek | 2027-01-31 |
| CVE-2026-47884 | `spring-webmvc` 5.3.39 (fixed in 7.0.9) | sunbird-rc-core, claim-ms, id-gen-service, encryption-service | No fix in 5.3.x; needs the Spring Boot 3 / Java 17 migration | Kartheek | 2027-01-31 |
| CVE-2026-22732 | `spring-security-web` 5.7.13 / 5.8.16 (fixed in 6.5.9) | sunbird-rc-core, claim-ms | Needs the Spring Boot 3 / Java 17 migration | Kartheek | 2027-01-31 |
| CVE-2019-20444 | `io.netty:netty` 3.9.9 / 3.10.6 (fixed in 4.0.0) | sunbird-rc-core, claim-ms | Transitive (not declared in any `pom.xml`); find the source and exclude it | Kartheek | 2027-01-31 |
| CVE-2021-34371 | `org.neo4j:neo4j` 3.2.3 (fixed in 3.5.0) | sunbird-rc-core, claim-ms | Transitive via `neo4j-gremlin` (`java/registry/pom.xml:425`); the CVE is in the Neo4j shell server, which is not configured (to confirm) | Kartheek | 2027-01-31 |

## Tests not gated

| Area | Why | Resolution |
|---|---|---|
| credential-schema and credentials-service unit job | Their specs need Postgres (credential-schema: 25 of 81 fail without it) or a live identity-service (credentials-service: 20 of 210). They run in `node-integration` instead. | None needed; covered by integration job |
| metrics (Go) | Has no tests | Add tests |
| notification-service (Go) | `pkg/services/sms_service_test.go:69` uses a config field (`SmsAPI.RequestTemplate`) that no longer exists, so the package does not compile under `go test`. `go vet` also fails. | Fix the test, then add a Go job to `ci.yml` |
| id-gen-service, encryption-service (Java) | Not in CI; the image build runs `mvn clean install` with their tests | Add to CI if they gain meaningful tests |
| Registry tests on V2 stack | `maven.yml` runs the Karate suite against `docker-compose-v1.yml` only | SBCOSS-1213 E2E |
| Adopter Testing Postman collection (`services/adopter-testing.postman_collection.json`) | Not yet run in CI. Several requests have no assertions or depend on run order, so a pass would overstate coverage. | Fix the collection, then gate with Newman against the built images |
| End-to-end revoke → verify fails | No automated test | Part of SBCOSS-1206 / Postman gate |

## Unsupported scenarios

| Scenario | Status |
|---|---|
| OpenID4VCI Authorization Code flow, PKCE, `tx_code` | Not implemented (SBCOSS-1208) |
| OpenID4VP Presentation Exchange (`presentation_definition` / `presentation_submission`) | Not implemented in oid4vc-service; only DCQL |
| VC Data Model 2.0, Data Integrity EdDSA/ECDSA cryptosuites | Not implemented (SBCOSS-1209) |
| Bitstring Status List verification, three-state result | SBCOSS-1206 |
| Credential suspension / reinstatement | SBCOSS-1207 |
