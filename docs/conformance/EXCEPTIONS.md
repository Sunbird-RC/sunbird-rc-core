# Exceptions and unsupported scenarios

Everything listed here is a known gap in the release gates in [README.md](README.md).
Each entry says what is not covered, why, and what removes it. Remove the entry when it's resolved.

Last reviewed: 2026-11-24.

## Security

### Fixable CRITICAL vulnerabilities (accepted temporarily)

The release image scan (`make scan-images`) blocks on any fixable CRITICAL vulnerability that is not
listed in [`.trivyignore`](../../.trivyignore). To land the gate without blocking releases, every
CRITICAL finding from the image scan on 2026-11-23 is accepted temporarily, and the fixes are deferred
to follow-up work.

**[`.trivyignore`](../../.trivyignore) is the single list of accepted CVEs.** For each one it gives
the package, installed and fixed versions, affected images, the reason, the owner and the expiry date.

- Owner: Kartheek. Expiry: 2027-01-31. After that date the CVEs are reported again and the release
  scan fails.
- Any new CRITICAL finding still blocks the release until it is fixed or added to `.trivyignore`.
- `form-data` 2.3.3 (CVE-2025-7783) and tomcat-embed-core 9.0.116 (CVE-2026-41293, CVE-2026-43512,
  CVE-2026-43515) were reported by a filesystem scan of the repository but are not in any released
  image, so they are not in `.trivyignore`. They should still be fixed in the follow-up.

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
| OpenID4VP Presentation Exchange (`presentation_definition` / `presentation_submission`) | Implemented in oid4vc-service as a DIF Presentation Exchange v2.0 subset (`input_descriptors` field path/filter and format, `submission_requirements` all/pick). Not yet checked against a conformance suite |
| VC Data Model 2.0, Data Integrity EdDSA/ECDSA cryptosuites | Not implemented (SBCOSS-1209) |
| Bitstring Status List verification, three-state result | SBCOSS-1206 |
| Credential suspension / reinstatement | SBCOSS-1207 |
