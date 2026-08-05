# Deployment Review — `98.70.36.106` (`https://98.70.36.106.sslip.io`)

**Reviewed:** 31 July 2026
**Branch:** `oid4vc` @ `eea4be2e`
**Deploy directory on host:** `~/sunbird-rc-oid4vc` (user `azureuser`, hostname `rc-dev`)

> **Status update, same day.** R1, R2 and R6 are now **fixed in the repository**
> and awaiting deployment; R3 is partly addressed. See §10 for what changed. The
> findings below are kept as written so the reasoning stays reviewable.

---

## 1. Executive summary

The deployment is **healthy right now**: 13 of 13 containers up and healthy,
every gateway route answering as expected, and a genuine Let's Encrypt
certificate valid until 22 October 2026.

But the host configuration has **drifted from the repository** in one way that
makes the next routine `git pull && docker compose up -d` silently destructive:
the repo's compose file no longer pins the three patched service images, so a
deploy straight from the branch would replace them with the unpatched upstream
images and undo all five OpenID4VCI/VP interop fixes. That is R1 below and is the
one item worth fixing before anything else.

Two secondary structural risks: the demo container that serves the verifier
console's QR images can no longer be rebuilt from source (R2), and nginx's real
upstream dependencies span two compose files without being declared (R3).

---

## 2. What is actually running

| Service | Image | Uptime | Defined in |
|---|---|---|---|
| `db` | `postgres:14` | 7 days | cloud |
| `vault` | `vault:1.13.3` | 7 days | cloud |
| `redis` | `redis:latest` | 7 days | cloud |
| `identity` | `pallakartheekreddy/sunbird-rc-identity-service:credo-fix4` | 2 days | cloud |
| `credential-schema` | `snt1/sunbird-rc-credential-schema:oid4vc` | 2 days | cloud |
| `credential` | `pallakartheekreddy/sunbird-rc-credentials-service:credo-fix4` | 2 days | cloud |
| `oid4vc-service` | `pallakartheekreddy/sunbird-rc-oid4vc-service:credo-fix4` | 2 days | cloud |
| `verifier-app` | `pallakartheekreddy/sunbird-rc-oid4vc-verifier-app:1.2.2` | 25 hours | cloud |
| `test-wallet` | `snt1/sunbird-rc-oid4vc-test-wallet:oid4vc` | 3 days | cloud + demo |
| `nginx` | `ghcr.io/sunbird-rc/sunbird-rc-nginx` | 2 days | cloud |
| `keycloak` | `ghcr.io/sunbird-rc/sunbird-rc-keycloak:latest` | 6 days | **demo** |
| `wallet-backend` | `snt1/sunbird-rc-oid4vc-wallet-backend:oid4vc` | 3 days | **demo** |
| `demo` | `sunbird-rc-oid4vc-demo:latest` *(local build, no registry)* | 2 days | **demo** |

Three of the four Sunbird services run our **patched fork images**
(`pallakartheekreddy/…:credo-fix4`). `credential-schema` is unmodified upstream
and correctly stays on `snt1:oid4vc`.

> **Note:** Keycloak *is* deployed, via `docker-compose.demo.yml`. The header
> comment in `docker-compose.cloud.yml` says Keycloak is excluded — that is true
> of *that file only*, and has been a source of confusion.

### Route health (probed 31 Jul 2026)

| Route | Code | Upstream |
|---|---|---|
| `/health` | 200 | oid4vc-service |
| `/identity-health` | 200 | identity |
| `/schema-health` | 200 | credential-schema |
| `/credential-health` | 200 | credential |
| `/.well-known/openid-credential-issuer` | 200 | oid4vc-service |
| `/.well-known/openid-configuration` | 200 | oid4vc-service |
| `/oid4vc-jwks.json` | 200 | oid4vc-service |
| `/identity-jwks.json` | 200 | identity |
| `/credential-schema/oid4vci-configs` | 200 | credential-schema |
| `/vct/farmer-land-holding-credential` | 200 | oid4vc-service |
| `/verifier-app/` | 200 | verifier-app |
| `/verifier`, `/issuer`, `/qr` | 200 | **demo** |
| `/wallet/` | 200 | test-wallet |
| `/auth` | 303 | keycloak (redirect — correct) |
| `/.well-known/jwks.json` | **404** | *(nothing — see R6)* |
| `/wallet-api` | **404** | wallet-backend *(see R7)* |

### TLS

Genuine Let's Encrypt certificate, not self-signed:

```
subject = CN = 98.70.36.106.sslip.io
issuer  = C = US, O = Let's Encrypt, CN = YE2
valid   = 24 Jul 2026 → 22 Oct 2026
```

Renewal plumbing is mounted: `./nginx/acme-webroot → /var/www/certbot` (HTTP-01
challenge) and `/etc/letsencrypt → /etc/letsencrypt` read-only. Port 80 serves
only the ACME challenge and a 301 to HTTPS.

---

## 3. What we added to `.env`

Eleven keys added since the 24 Jul baseline. Secrets shown as `<redacted>`.

| Key | Value | Purpose |
|---|---|---|
| `OID4VC_FIX_ORG` | `pallakartheekreddy` | Docker Hub org for the three patched service images |
| `OID4VC_FIX_TAG` | `credo-fix4` | Tag carrying the five interop fixes |
| `VERIFIER_APP_TAG` | `1.2.2` | Verifier console image version |
| `OID4VP_SIGN_REQUEST` | `false` | Turns off JAR request-object signing globally (see R4/R5) |
| `WALLET_TRUSTED_VERIFIERS` | JSON with `allowUnsigned:true` | Lets the browser test-wallet accept our unsigned requests |
| `WALLET_JWT_SECRET` | `<redacted>` | wallet-backend session signing |
| `KEYCLOAK_ADMIN_USER` | `<redacted>` | Keycloak bootstrap admin |
| `KEYCLOAK_ADMIN_PASSWORD` | `<redacted>` | Keycloak bootstrap admin |
| `DEMO_PUBLIC_URL` | `https://98.70.36.106.sslip.io` | Absolute links in the demo app |

Carried over unchanged from the baseline:

| Key | Value |
|---|---|
| `OID4VC_PUBLIC_URL` | `https://98.70.36.106.sslip.io` |
| `OID4VC_ISSUER_DID` | `did:web:98.70.36.106.sslip.io:f2572a18-da4b-4adf-b12a-a6412a0aa1ac` |
| `WEB_DID_BASE_URL` | `https://98.70.36.106.sslip.io` |
| `POSTGRES_PASSWORD` | `<redacted>` |
| `VAULT_TOKEN` | `<redacted>` |

**Referenced by compose but NOT set:** `OID4VC_VERIFIER_DID`,
`OID4VC_IMAGE_TAG`, `OID4VC_DRAFT13_COMPAT`, `ENABLE_AUTH`,
`OID4VP_LEGACY_CLIENT_ID_SCHEME`, `VERIFIER_APP_ORG`. All have safe compose-level
defaults except `OID4VC_VERIFIER_DID` — see R4.

---

## 4. What we added to `docker-compose.cloud.yml`

Four changes against the 24 Jul baseline.

**4.1 — Per-service image parameterisation** (`identity`, `credential`,
`oid4vc-service`):

```diff
-    image: docker.io/snt1/sunbird-rc-identity-service:${OID4VC_IMAGE_TAG-oid4vc}
+    image: docker.io/${OID4VC_FIX_ORG:-snt1}/sunbird-rc-identity-service:${OID4VC_FIX_TAG:-oid4vc}
```

Deliberately **not** a single shared variable. `OID4VC_IMAGE_TAG` is read by six
services and only three are patched — an earlier attempt to reuse it retagged
services that had no fork image and broke the stack.

**4.2 — New `verifier-app` service:**

```yaml
verifier-app:
  image: docker.io/${VERIFIER_APP_ORG:-pallakartheekreddy}/sunbird-rc-oid4vc-verifier-app:${VERIFIER_APP_TAG:-1.0.0}
  platform: linux/amd64
  restart: unless-stopped
  healthcheck:
    test: ['CMD-SHELL', 'curl -fsS http://localhost:80/ >/dev/null || exit 1']
```

No `depends_on` — correct. It is a static bundle; the browser talks to the
gateway, so the container has no runtime dependency on any backend.

**4.3 — Three OID4VP env vars on `oid4vc-service`:**

```yaml
- OID4VP_SIGN_REQUEST=${OID4VP_SIGN_REQUEST-true}
- OID4VP_LEGACY_CLIENT_ID_SCHEME=${OID4VP_LEGACY_CLIENT_ID_SCHEME-false}
- VERIFIER_DID=${OID4VC_VERIFIER_DID-}
```

**4.4 — Two nginx volume mounts for TLS:**

```yaml
- ./nginx/acme-webroot:/var/www/certbot:ro
- /etc/letsencrypt:/etc/letsencrypt:ro
```

---

## 5. What we added to `nginx/nginx.cloud.conf`

Routes added since the baseline: `/verifier-app/` and `= /verifier-app`
(redirect), `/vct` and `/.well-known/vct`, the did:web document regex, `/issuer`,
`/verifier`, `/callback`, `/qr`, `/shared`, `/auth`, `/wallet-api`, the ACME
challenge location, and the port-80 → 443 redirect server block.

The did:web route is the one with a syntax trap — `{36}` must sit inside a quoted
regex or nginx refuses to start:

```nginx
location ~ "^/[0-9a-fA-F-]{36}/did\.json$" {
      proxy_pass http://identity:3332;
}
```

Convention throughout: `proxy_pass` carries **no URI part** and `location`
patterns carry **no trailing slash**, so nginx passes the original path through
untouched. The two exceptions are deliberate — `/verifier-app/` and `/wallet/`
use a trailing-slash `proxy_pass` to strip their prefix so each container serves
from its own root.

---

## 6. Service dependencies

```
        ┌──────────┐
        │  vault   │──┐
        └──────────┘  │
        ┌──────────┐  ├──► identity ──┬──► credential-schema ──┐
        │    db    │──┘   (3332)      │        (3333)           │
        └──────────┘                  └──► credential ─────────┤
                                              (3000)           │
        ┌──────────┐                                           ├──► oid4vc-service ──► nginx
        │  redis   │───────────────────────────────────────────┘        (3400)          (80/443)
        └──────────┘

  Declared depends_on (cloud.yml):
    identity          → vault, db
    credential-schema → db, identity
    credential        → db, identity, credential-schema
    oid4vc-service    → credential, identity, credential-schema, redis
    nginx             → oid4vc-service, test-wallet
    verifier-app      → (none)
```

**Internal URLs** (all name-based on the compose network):

```
CREDENTIAL_SERVICE_BASE_URL = http://credential:3000
IDENTITY_BASE_URL           = http://identity:3332
SCHEMA_BASE_URL             = http://credential-schema:3333
REDIS_URL                   = redis://redis:6379
SESSION_STORE               = redis
```

`SESSION_STORE=redis` matters: offer and VP sessions survive an
`oid4vc-service` restart, and the service can be scaled to multiple replicas.

**Undeclared cross-file dependencies** — nginx proxies to three services defined
in `docker-compose.demo.yml`, which `depends_on` does not mention:

```
nginx ──► demo           (/issuer, /verifier, /callback, /shared, /qr)
      ──► keycloak       (/auth)
      ──► wallet-backend (/wallet-api)
```

---

## 7. Risks and remediation

### R1 — CRITICAL: the repo reverted the image parameterisation the host relies on

The repo's `docker-compose.cloud.yml` has gone back to
`docker.io/snt1/…:${OID4VC_IMAGE_TAG-oid4vc}` for `identity`, `credential` and
`oid4vc-service` (upstream merge `678d327a`). `OID4VC_IMAGE_TAG` is not set on the
host, so deploying from the repo file resolves all three to `snt1/…:oid4vc` — the
**unpatched upstream images** — reintroducing every fix in `0d5a138f`:

- did:key / did:jwk proof-of-possession resolution
- `nbf` / `vc.issuanceDate` whole-second equality
- the `authentication` verification relationship on minted DID documents
- `credential_definition` omission on `vc+sd-jwt` configs
- key binding by reference (`cnf.kid`)

This exact class of mistake already caused two incidents during rollout. It is
now latent in the branch rather than in someone's shell history.

**Fix:** restore the per-service `OID4VC_FIX_ORG` / `OID4VC_FIX_TAG`
parameterisation in the repo's compose file (three `image:` lines) and commit, so
host and repo agree. Do not reintroduce a single shared `OID4VC_IMAGE_TAG`.

### R2 — HIGH: `/qr` depends on a container that cannot be rebuilt

Commit `acbd4e38` ("Deleted Test Wallet & Demo app") removed
`services/oid4vc-service/demo/**` — 14 files including `server.mjs` and the
compose file. The `demo` container is still running, from a **locally built,
registry-less** image `sunbird-rc-oid4vc-demo:latest` (built 2026-07-28). It backs
five gateway routes: `/issuer`, `/verifier`, `/callback`, `/shared`, `/qr`.

`/qr` is the consequential one. The verifier console renders every QR through
`qrSrc()` → `${BASE}/qr` (`verifier-app/src/api.ts:167`), and `oid4vc-service`
has **no `/qr` route** — `app.controller.ts` serves only `health`,
`render-templates`, `contexts` and `vct`. If that container is pruned or the VM
rebuilt, the image is unrecoverable and the console loses its QR images.

**Fix, in preference order:**

1. Add `GET /qr?data=` to `oid4vc-service` and repoint the nginx location. Removes
   the dependency; small and self-contained.
2. `docker save` the image as a stopgap and push it to `pallakartheekreddy` so it
   is at least pullable.

Separately, decide the fate of `/issuer` and `/verifier` — the verifier console at
`/verifier-app/` supersedes the verifier half of the old demo.

### R3 — HIGH: nginx's dependencies span two compose files, undeclared

`nginx` is in `docker-compose.cloud.yml` and declares only `oid4vc-service` and
`test-wallet`. It also proxies to `demo`, `keycloak` and `wallet-backend` from
`docker-compose.demo.yml` — a file that **exists only on the host**, since
`acbd4e38` deleted the repo copy.

Consequences:

- `docker compose -f docker-compose.cloud.yml up -d` alone starts nginx pointing
  at absent upstreams → 502 on `/issuer`, `/verifier`, `/qr`, `/auth`.
- nginx resolves upstream names once at startup, so recreating `identity` or
  `credential` needs an explicit `nginx -s reload`. This is exactly the 502 hit
  earlier in the rollout.

**Fix:** commit `docker-compose.demo.yml` to the repo next to the cloud file,
document that both `-f` flags are required for any full deploy, and either add a
`resolver` with variable `proxy_pass` or make `nginx -s reload` an explicit
documented post-deploy step.

### R4 — MEDIUM: `OID4VC_VERIFIER_DID` referenced but never set

Compose maps `VERIFIER_DID=${OID4VC_VERIFIER_DID-}` and defaults
`OID4VP_SIGN_REQUEST` to **`true`**. The host `.env` sets it to `false` and never
defines `OID4VC_VERIFIER_DID`, so `VERIFIER_DID` is empty. Self-consistent today —
but a fresh deploy that omits the `OID4VP_SIGN_REQUEST=false` line gets signing
enabled with no resolvable verifier DID, and `/vp/request` fails loudly.

**Fix:** set `OID4VC_VERIFIER_DID` to the existing did:web (same value as
`OID4VC_ISSUER_DID`) and flip `OID4VP_SIGN_REQUEST=true`. Correct by default
rather than correct by omission — and signed requests are what wallets want.

### R5 — MEDIUM: unsigned request objects are the default for hand-written callers

With `OID4VP_SIGN_REQUEST=false`, any `POST /vp/request` that omits
`"signed": true` produces a request object a Credo/Paradym wallet rejects with
`406 invalid_request_uri`. The verifier console always sends the flag
(`verifier-app/src/api.ts:113`), so the UI is unaffected; curl and Postman callers
are not. Resolved by the R4 fix.

### R6 — LOW: advertised `jwks_uri` returns 404

`/.well-known/openid-configuration` advertises:

```json
"jwks_uri": "https://98.70.36.106.sslip.io/.well-known/jwks.json"
```

which **404s** at the gateway. The keys are served at `/oid4vc-jwks.json`. Any
client that follows issuer metadata to fetch signing keys fails.

**Fix:** add one nginx location for `/.well-known/jwks.json` →
`http://oid4vc-service:3400/.well-known/jwks.json`, mirroring the existing alias.
Keep `/oid4vc-jwks.json` for compatibility.

### R7 — LOW: deploy-directory hygiene

- **`vault-keys.txt` is mode `664`** — Vault unseal keys and root token readable
  by any local account on the VM. Should be `600`.
- **22 `.bak*` files** across `.env`, `docker-compose.cloud.yml` and
  `nginx/nginx.cloud.conf`. They served their purpose during rollout; the repo is
  the real history now.
- **`/wallet-api` returns 404** despite having an nginx location. Either the
  upstream path is wrong or the route is vestigial — confirm before removing.

---

## 8. Verified healthy — no action needed

- 13/13 containers healthy; infrastructure (`db`, `vault`, `redis`) up 7 days.
- Every route in §2 returns as expected.
- Real Let's Encrypt certificate with working renewal plumbing.
- Keycloak deployed and healthy — worth noting for any future
  DigiLocker-style authenticated-issuance work, since an authorization server
  already exists on this box.
- Service source changes are **committed** (`0d5a138f`, `2f0d6e7a`). Only
  `verifier-app/package.json`, `docs/END_TO_END_FLOW.md` and the Postman
  collection remain uncommitted, so images and code are in sync.
- Internal wiring is consistent and name-based; `SESSION_STORE=redis` keeps
  sessions durable across restarts.

---

## 9. Suggested order of work

| # | Item | Effort | Why this order |
|---|---|---|---|
| 1 | R1 — restore image parameterisation in the repo | minutes | Prevents a silent production regression on the next deploy |
| 2 | R3 — commit `docker-compose.demo.yml`, document both `-f` flags | minutes | Makes any deploy reproducible from the repo alone |
| 3 | R7 — `chmod 600 vault-keys.txt` | seconds | Credential exposure, trivially fixed |
| 4 | R4/R5 — set `OID4VC_VERIFIER_DID`, enable signing | ~30 min incl. wallet retest | Removes a footgun for curl/Postman callers |
| 5 | R6 — nginx `/.well-known/jwks.json` alias | minutes | Spec-conformance for any client reading issuer metadata |
| 6 | R2 — move `/qr` into `oid4vc-service` | ~half day | Largest change; unblocks retiring the demo container |

### Verification after each change

1. `docker compose -f docker-compose.cloud.yml -f docker-compose.demo.yml config`
   — the three patched services must still resolve to
   `pallakartheekreddy/…:credo-fix4` and `verifier-app` to `1.2.2` **when using
   the repo file**. This is the R1 regression test.
2. Re-probe every route in §2. `/.well-known/jwks.json` must go 404 → 200 and
   return the same JWKS as `/oid4vc-jwks.json`.
3. Full round trip through `/verifier-app/` for both credential types (Mobile Age,
   Farmer Land Holding): QR renders, Paradym presents, `verified: true` with all
   seven checks OK.
4. `POST /vp/request` **without** `"signed": true` must yield a wallet-acceptable
   signed request — verified by a real Paradym scan, not by status code alone.
5. `docker compose restart identity credential` then `curl -sk …/identity-health`
   → 200 with no manual `nginx -s reload`.
6. `stat -c %a vault-keys.txt` → `600`.

Back up before every host-side edit, as with all prior changes:
`cp .env .env.bak-$(date +%Y%m%d-%H%M%S)`.

---

## 10. Status update — fixes landed in the repository

All changes below are **committed to the working tree only**. Nothing has been
deployed; the host still runs the images and config described above.

| # | Was | Now |
| --- | --- | --- |
| **R1** | Repo compose resolved the three patched services to unpatched `snt1/…:oid4vc` | Per-service `OID4VC_FIX_ORG`/`OID4VC_FIX_TAG` parameterisation restored in `docker-compose.cloud.yml`, with a comment explaining why a single shared tag variable is wrong. Verified both ways with `docker compose config`: with the host's `.env` values the three resolve to `pallakartheekreddy/…:credo-fix4`; with none set they fall back to upstream safely |
| **R2** | `/qr` served only by the unrebuildable `demo` container | `GET /qr` implemented in `oid4vc-service` (`app.controller.ts`) using the `qrcode` dependency it **already had**, so nothing was added to the tree. nginx `/qr` repointed at `oid4vc-service`. Verified live: 240×240 PNG, 400 on empty or oversized input |
| **R3** | nginx's real upstreams span two compose files, undeclared | Partly addressed: `test-wallet` and `verifier-app` — which the repo compose was missing entirely — are now declared, and `nginx` `depends_on` includes `test-wallet`. **Still outstanding:** `docker-compose.demo.yml` exists only on the host, and nginx still proxies to `demo`/`keycloak`/`wallet-backend` from it |
| **R6** | `jwks_uri` advertised a path that 404s | `location = /.well-known/jwks.json` added, proxying the same JWKS as the existing `/oid4vc-jwks.json` alias, which is kept for compatibility |
| **R7** | `vault-keys.txt` mode 664; 22 backup files | **Unchanged** — these are host-side, and nothing has been touched on the host |

Also fixed while in the area, though not part of the original review:

- **The service build was broken by its own front-end code.** `nest build`
  compiled the nested `verifier-app` (and then `issuer-portal`) with the service's
  CommonJS/no-JSX settings, producing 168 `TS17004` errors. Both are now excluded
  in `tsconfig.json` and `tsconfig.build.json`. This predated this work.
- **The TLS mounts** (`acme-webroot`, `/etc/letsencrypt`) present on the host were
  missing from the repo compose; added, so a deploy from the repo keeps HTTPS.

### Deployment order when this does go out

1. `docker compose config` and confirm the three patched services still resolve
   to `pallakartheekreddy/…:credo-fix4`. This is R1's regression test and must
   pass before anything else.
2. `nginx -t` against the new config — **not yet run**, as the Docker daemon was
   unavailable locally. The added blocks use no regex or `{n}` quantifiers, so
   they avoid the trap that previously stopped nginx from starting, but this must
   be checked before a reload.
3. Re-probe every route in §2; `/.well-known/jwks.json` must go 404 → 200 and
   `/qr` must still return a PNG once repointed.
4. Round-trip the verifier console for both credential types.
