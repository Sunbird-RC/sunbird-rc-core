# Release validation and standards conformance

Tracks SBCOSS-1211. This page lists the automated checks that run on the V2 credential
services, which of them block a release, and the status of each standards conformance suite.
Known gaps and accepted exceptions are in [EXCEPTIONS.md](EXCEPTIONS.md).

## Quality gates

| Check | Where | Runs on | Blocks |
|---|---|---|---|
| Registry (Java) Karate e2e, V1 stack | `.github/workflows/maven.yml` (`make test`) | PR/push to `main`, `v*`, `release-*`; release | PR, release |
| Node build + unit tests: identity-service, oid4vc-service | `.github/workflows/ci.yml` `node-unit` | PR/push to `main`, `v*`, `release-*`; release | PR, release |
| Node build: credential-schema, credentials-service | `ci.yml` `node-unit` | same | PR, release |
| Integration tests (Postgres + Vault + identity-service): identity-service, credential-schema, credentials-service | `ci.yml` `node-integration` (`make -C services/<svc> test`) | same | PR, release |
| Dependency vulnerability scan (Trivy, `services/`) | `ci.yml` `dependency-scan` | same | No (report only, including CVEs accepted in `.trivyignore`; results in the Security tab and as an artifact; warns 30 days before a `.trivyignore` entry expires) |
| CodeQL (Java, JavaScript) | `.github/workflows/codeql-analysis.yml` | PR/push, weekly | No |
| Image vulnerability scan of every published image (Trivy, fixable CRITICAL) | `manual.yml` → `make scan-images` | release | Release |

`manual.yml` (Build and Publish) runs `maven.yml` and `ci.yml` first. It builds images only if
both pass, and pushes only if the image scan passes. Locally, `make release` runs the same
sequence: `test`, `test-services`, `scan-images`, `push-images`.

### Release inputs (`manual.yml`)

| Input | Default | Effect |
|---|---|---|
| `version` | – (required) | Tag pushed for every image, e.g. `v2.1.2` (overrides `RELEASE_VERSION` in the Makefile) |
| `dry_run` | `false` | Runs all tests, builds and scans every image, but skips registry login and push. Use it to prove a release before publishing. |
| `tag_latest` | `false` | Also pushes `:latest`. Tick it only when releasing the newest release line, so a patch release from an older branch can't overwrite `:latest`. |

Local `make release` pushes only `:<version>` by default, like `manual.yml`. Add `PUSH_LATEST=true` to also push `:latest`.

### Getting changes to `main`

Release work currently lands on the release branch first (`v2.1.1` is ahead of `main`). Proposed flow:

1. Feature PRs target the active release branch (for example `v2.1.1`). CI runs on PRs to `main`, `v*` and `release-*`.
2. After a release is published from that branch, open a PR from the release branch into `main`, and resolve conflicts with any commits that exist only on `main`.
3. Fixes needed on both branches are cherry-picked to the other with `git cherry-pick -x`, so the origin commit is recorded.

### Running the gates locally

```sh
make test-services          # V2 integration tests, needs docker-compose
make build scan-images      # build every image, then scan it (needs trivy)
```

`make test-services` binds ports 3332, 3333 and 8200. Stop any local stack that uses them first.

## Standards conformance suites

| Suite | Status | Blocked by |
|---|---|---|
| W3C VC Data Model 2.0 test suite | Not integrated | SBCOSS-1209 (VCDM 2.0), plus a VC-API adapter so the suite can call RC |
| W3C Data Integrity EdDSA / ECDSA cryptosuite suites | Not integrated | SBCOSS-1209 |
| W3C Bitstring Status List suite | Not integrated | SBCOSS-1206. The suite covers issue/verify only; revocation negatives must be RC's own tests |
| OIDF OpenID4VCI conformance | Not integrated | SBCOSS-1208 (Authorization Code + PKCE, `tx_code`) |
| OIDF OpenID4VP conformance | Not integrated | Planned for the next PR; RC first has to trust the suite's test issuer keys |
| SD-JWT reference vectors (RFC 9901) | Not integrated | No blocker; next step |

When a suite is integrated it is added to the table above with the job that runs it.
Until the feature it tests lands, it runs as report-only.

## Regression tests for fixed defects

Every bug fix to a V2 service must include a spec that fails before the fix and passes after it.
Put it next to the code it covers, in `*.spec.ts`, and reference the issue key in the `it(...)` title, e.g.
`it('SBCOSS-1234: returns 400, not 500, when schema.description is missing', ...)`.
CI runs these specs automatically as part of `node-unit` / `node-integration`.
