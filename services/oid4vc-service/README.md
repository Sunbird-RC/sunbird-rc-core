# oid4vc-service

OpenID4VCI 1.0 (credential issuance) + OpenID4VP 1.0 (credential presentation)
**protocol façade** for Sunbird RC.

It speaks the wallet protocols on the outside and delegates everything else
to the **existing, unchanged** services on the inside:

- **credentials-service** (`/credentials/issue`, `/credentials/verify`) — builds & signs the VC, stores it
- **identity-service** (`/utils/sign`, `/utils/sign-jwt`, `/utils/sign-sd-jwt`, `/utils/sign-mdoc`, `/did/resolve`, `/.well-known/jwks.json`) — all key operations (keys stay in Vault)
- **credential-schema** (`/credential-schema/oid4vci-configs`) — drives the issuer metadata

It holds **no credential keys and no credential storage** — only its own
OAuth/protocol signing key (delegated to identity-service) and short-lived
session state.

This document consolidates everything a new contributor or operator needs:
architecture, supported formats (including `mso_mdoc` and W3C VC Render
Method), the full API/sequence design, configuration reference, local
deployment, manual testing, production hardening, wallet interoperability,
and end-to-end verification evidence.

---

## Table of Contents

1. [Architecture](#1-architecture)
2. [Supported Credential Formats](#2-supported-credential-formats)
3. [API Flow Design](#3-api-flow-design)
4. [Configuration Reference](#4-configuration-reference)
5. [Local Deployment (Quick Start)](#5-local-deployment-quick-start)
6. [Manual Testing Guide](#6-manual-testing-guide)
7. [mso_mdoc & W3C VC Render Method](#7-mso_mdoc--w3c-vc-render-method)
8. [Production Deployment Guide](#8-production-deployment-guide)
9. [Wallet Interoperability](#9-wallet-interoperability)
10. [End-to-End Verification Evidence](#10-end-to-end-verification-evidence)

---

## 1. Architecture

```
 Wallet                oid4vc-service                 identity-service   credentials-service   credential-schema
   |                        |                                |                    |                    |
   |--GET issuer metadata-->|<--------- oid4vci-configs ----------------------------------------------->|
   |                        |
   |  (issuer) POST /oid4vc/offer  ---->  session stored (Redis/memory)
   |<--- credential_offer (QR) ----------|
   |
   |--POST /oid4vc/token (pre-auth code)->|--- mints access token (ES256) ------------------->|
   |<---------- access_token, c_nonce ----|
   |
   |--POST /oid4vc/credential (Bearer + PoP JWT)-->|
   |                        |--verify PoP (holder DID/JWK)-->|
   |                        |--POST /credentials/issue (format) -------------------------->|
   |                        |                                |<--sign (Ed25519 / ES256 / COSE)--|
   |<----- signed VC (format-appropriate envelope) ---------------------------------------|

 Verifier               oid4vc-service                                  Wallet
   |--POST /vp/request (DCQL)-->|
   |<--- request_uri, QR -------|
   |                            |<--- GET /vp/request-object/:id --- Wallet
   |                            |<--- POST /vp/response (vp_token) - Wallet
   |                            |--verify holder/device sig, nonce, embedded VC(s)
   |                            |  (credentials-service /verify or @auth0/mdl Verifier),
   |                            |  status, holder-binding, DCQL satisfaction
   |--GET /vp/status/:id ------>|
   |<--- {verified, checks[]} --|
```

**Key services involved (T2 topology — no Java registry needed):**

| Service | Port | Role |
|---|---|---|
| `db` (Postgres) | 5432 | shared DB, dedicated `identity`/`credential_schema`/`credential` databases |
| `vault` | 8200 | holds all signing keys (Ed25519, EC/ES256, EC P-256 for mdoc) |
| `redis` | 6379 | session store backend (optional; `memory` works for single instance) |
| `identity` | 3332 | DID generation/resolution, `/utils/sign*`, `/.well-known/jwks.json` |
| `credential-schema` | 3333 | schema CRUD, `oid4vciConfig` opt-in, `/credential-schema/oid4vci-configs` |
| `credential` | 3000 | `/credentials/issue`, `/credentials/verify` (format-aware) |
| `oid4vc-service` | 3400 | this service — `/oid4vc/*` (OID4VCI) and `/vp/*` (OID4VP) |

### Session store

Abstract interface (`src/session`) with two backends chosen by `SESSION_STORE`:
- `memory` — single-instance/test (default). Do **not** run multiple replicas with this.
- `redis` — production. Uses native TTL + atomic GETDEL for single-use codes/nonces.

### Draft-13 vs final-1.0

The core logic emits final OID4VCI/OID4VP 1.0 shapes. `DRAFT13_COMPAT_MODE=true`
switches the metadata (`credentials_supported`), the offer object's
`credentials` field, the offer grant (`user_pin_required`), and keeps
`c_nonce` in the token response — the idioms MOSIP Inji Wallet expects today.
This is isolated to `oid4vci.service.ts` so both shapes are covered without
branching the rest of the flow.

### Registry integration (who calls `POST /oid4vc/offer`)

`POST /oid4vc/offer` is designed to be called by an **issuer-side backend**,
not the wallet — the wallet only ever calls `GET /oid4vc/offer/:id`
(dereference), `POST /oid4vc/token`, and `POST /oid4vc/credential`. In this
repo, the Java registry is the intended caller, wired at two fail-open hook
points:

| Hook | File | Trigger |
|---|---|---|
| Entity create/update | `RegistryServiceImpl.java` (`generateCredentials()`) | Fires on both `addEntity()` and `updateEntity()` |
| Claim grant / attestation | `RegistryHelper.java` (`updateState()`, `GRANT_CLAIM` branch) | Fires when an attestation is granted with a `credentialTemplate` |

Both delegate to `java/registry/.../service/OID4VCIService.java`, gated by
`oid4vc.enabled` and `oid4vc.offerUrl` (see [§4](#4-configuration-reference)).
The `credential_configuration_id` passed is the entity's `vertexLabel` (e.g.
`"Teacher"`) for the create/update hook, or `"<sourceEntity>_<policyName>"`
for the claim-grant hook — this **must exactly match** the corresponding
`credential-schema` record's `name` (or its `schemaId`, see
[§4](#4-configuration-reference)), or offer creation 404s.

---

## 2. Supported Credential Formats

All four formats are supported and chosen per credential type via the
schema's `oid4vciConfig.oid4vciFormats`:

| Format | Signed by (identity-service) | Selective disclosure | Claim shape |
|---|---|---|---|
| `ldp_vc` | `/utils/sign` (Ed25519 linked-data proof) | ✖ | W3C `credentialSubject` |
| `jwt_vc_json` | `/utils/sign-jwt` (ES256 JWS, W3C VC-JWT convention) | ✖ | W3C `credentialSubject`, nested under a `vc` claim |
| `vc+sd-jwt` | `/utils/sign-sd-jwt` (ES256, IETF SD-JWT VC) | ✔ | Flat top-level claims, digests in `_sd` |
| `mso_mdoc` | `/utils/sign-mdoc` (ES256 COSE_Sign1, ISO/IEC 18013-5) | ✔ (per-element digests) | `{namespace: {elementIdentifier: value}}` |

`mso_mdoc` is covered in detail in [§7](#7-mso_mdoc--w3c-vc-render-method).

Credentials can also carry a **W3C VC Render Method**
(https://www.w3.org/TR/vc-render-method/) entry for visual rendering by
wallets — see [§7](#7-mso_mdoc--w3c-vc-render-method).

---

## 3. API Flow Design

Sequence diagrams for the two protocol flows this service implements.
Legend: **solid arrow = request**, **dashed arrow = response**.

### OID4VCI — Credential Issuance

![OID4VCI Credential Issuance Flow](docs/images/vci-flow.png)

**Actors (left → right):** Registry / Issuer · oid4vc-service · credential-schema · identity-service · credentials-service · Wallet

#### 3.1 Discovery
1. **Wallet → oid4vc-service**: `GET /.well-known/openid-credential-issuer`
2. **oid4vc-service → credential-schema**: `GET /credential-schema/oid4vci-configs` (live lookup, not cached)
3. **credential-schema → oid4vc-service**: opted-in schemas with `formats`, `display`, `vct`, `docType`/`namespace` (mdoc), `renderMethod`
4. **oid4vc-service → Wallet**: `credential_configurations_supported` — one entry per `<schemaId>_<format>` combination

#### 3.2 Offer creation (issuer-side, pre-authorized_code grant)
5. **Registry/Issuer → oid4vc-service**: `POST /oid4vc/offer` `{credential_configuration_id, format, claims}` — fire-and-forget, fail-open
6. **oid4vc-service → Registry/Issuer**: `{offer_id, credential_offer_uri, credential_offer, qr_data}`

`credential_configuration_id` can be either the schema's stable `schemaId`
(e.g. `did:schema:...` — always unambiguous, recommended) or its display
`name` (kept for convenience; falls back to whichever schema variant
actually supports the requested `format` if multiple schemas share a name).

#### 3.3 Wallet dereferences the offer
7. **Wallet → oid4vc-service**: `GET /oid4vc/offer/:id`
8. **oid4vc-service → Wallet**: `credential_offer` — `{credential_configuration_ids, grants: {"urn:ietf:params:oauth:grant-type:pre-authorized_code": {"pre-authorized_code": ...}}}`

#### 3.4 Token exchange
9. **Wallet → oid4vc-service**: `POST /oid4vc/token` (`grant_type=urn:ietf:params:oauth:grant-type:pre-authorized_code&pre-authorized_code=...`)
10. **oid4vc-service → identity-service**: `POST /utils/sign-jwt` — mints a 5-minute ES256 access token
11. **oid4vc-service → Wallet**: `{access_token, c_nonce, expires_in: 300, c_nonce_expires_in: 300}`

The `pre-authorized_code` is single-use — an atomic store `GETDEL` consumes
it; a replayed code returns `400 invalid_grant: bad or used code`.

#### 3.5 Credential request (proof-of-possession)
12. **Wallet → oid4vc-service**: `POST /oid4vc/credential`, `Authorization: Bearer <access_token>`, body `{proof: {proof_type: "jwt", jwt: <PoP JWT>}}`
13. **oid4vc-service**: verifies the PoP JWT (`aud` == issuer public URL, `nonce` == live single-use `c_nonce`; resolves holder DID via `kid` or accepts an inline `jwk`/`did:jwk`)
14. **oid4vc-service → credentials-service**: `POST /credentials/issue` `{credential, credentialSchemaId, format, holderJwk, docType?, namespaces?}` — holder key bound as `credentialSubject.id` / JWT `sub` / SD-JWT `cnf.jwk` / mdoc `deviceKeyInfo.deviceKey` depending on format
15. **credentials-service → identity-service**: format-appropriate signing call
16. **credentials-service → oid4vc-service**: signed VC (format-aware envelope)
17. **oid4vc-service → Wallet**: `{credential, c_nonce, format}`

### OID4VP — Presentation Verification

![OID4VP Presentation Verification Flow](docs/images/vp-flow.png)

**Actors (left → right):** Verifier · oid4vc-service · identity-service · credentials-service · Wallet

#### 3.6 Verifier creates a request
1. **Verifier → oid4vc-service**: `POST /vp/request` `{dcql_query}`
2. **oid4vc-service → Verifier**: `{transaction_id, request_uri, qr_data}` (`openid4vp://...`)

The request object is served as plain, unsigned JSON (`client_id` = the
`response_uri`, with a separate `client_id_scheme: "redirect_uri"` field) —
this matches the request-object convention real-world wallets (e.g. walt.id)
expect today.

#### 3.7 Wallet fetches and answers the request
3. **Wallet → oid4vc-service**: `GET /vp/request-object/:id`
4. **oid4vc-service → Wallet**: plain JSON containing `nonce`, `state`, `dcql_query`, `client_id`, `response_uri`
5. **Wallet → oid4vc-service**: `POST /vp/response` (`direct_post`) `{state, vp_token}`

For `ldp_vc`/`jwt_vc_json`/`vc+sd-jwt`, `vp_token` is a JWT signed by the
holder's key, embedding `vp.verifiableCredential: [<VC>]` and the request's
`nonce`. For `mso_mdoc`, `vp_token` is instead a base64url-encoded CBOR
`DeviceResponse` (see [§7](#7-mso_mdoc--w3c-vc-render-method)) — a
structurally different presentation shape, since mdoc has its own
device-binding and session-transcript mechanism rather than a JWT wrapper.

#### 3.8 Verification chain
6. **oid4vc-service**: verifies holder/device signature, checks `nonce`/session-transcript freshness, verifies each embedded credential's signature + revocation status + holder binding (via `credentials-service /verify` for the JOSE/JSON-LD formats, or `@auth0/mdl`'s `Verifier` for mdoc), then evaluates the DCQL query against the disclosed claims
7. **oid4vc-service → Wallet**: `{status: "ok"}` (or `403` with a `checks` map showing exactly which stage failed)

#### 3.9 Verifier polls the result
8. **Verifier → oid4vc-service**: `GET /vp/status/:id`
9. **oid4vc-service → Verifier**: `{verified: true, checks: {holderSignature, nonce, credentialSignatures, holderBinding, revocation, dcql}, claims, holderDid}`

All six checks returning `OK` together is the cryptographic proof that (a)
the presenter controls the holder/device key, (b) the presented VC(s) are
genuine, unrevoked, and bound to that same holder, and (c) the disclosed
claims satisfy exactly what the verifier asked for via DCQL.

---

## 4. Configuration Reference

### 4.1 `oid4vc-service`

| Env var | Default | Purpose |
|---|---|---|
| `PORT` | `3400` | listen port |
| `PUBLIC_URL` | `http://localhost:3400` | **must be the externally-reachable HTTPS URL in production** — embedded as `iss`/`aud` in minted tokens, request objects, and PoP checks |
| `CREDENTIAL_SERVICE_BASE_URL` | `http://localhost:3000` | credentials-service |
| `IDENTITY_BASE_URL` | `http://localhost:3332` | identity-service |
| `SCHEMA_BASE_URL` | `http://localhost:3333` | credential-schema |
| `SESSION_STORE` | `memory` | `redis` required for any multi-replica deployment |
| `REDIS_URL` | `redis://localhost:6379` | required when `SESSION_STORE=redis` |
| `ISSUER_DID` | *(blank → auto-generated on boot)* | fallback issuer DID used for schemas that don't declare their own `author`. Should be pinned in production — an auto-generated ephemeral DID changes on every restart. |
| `OID4VP_ENABLED` | `true` | mounts `/vp/*` routes; set `false` to run issuance-only |
| `DRAFT13_COMPAT_MODE` | `false` | emit OID4VCI draft-13 shapes (Inji interop) |
| `ENABLE_AUTH` | `false` | reserved for enabling auth on internal endpoints (see [§8](#8-production-deployment-guide)) |
| `OFFER_TTL` | `600`s | offer session lifetime |
| `NONCE_TTL` | `300`s | `c_nonce` lifetime (single-use regardless) |
| `ACCESS_TOKEN_TTL` | `300`s | façade-minted access token lifetime |
| `DEFERRED_TTL` | `86400`s | deferred-issuance transaction lifetime |
| `VP_TXN_TTL` | `300`s | OID4VP request/response transaction lifetime |

**Per-schema issuer DID:** every credential schema declares an `author` DID
at creation time. `oid4vc-service` signs each credential using that schema's
own `author` DID (falling back to `ISSUER_DID` only if the schema has none) —
so different schemas can be issued by different identities without a
redeploy.

### 4.2 `identity-service`

| Env var | Purpose |
|---|---|
| `DATABASE_URL` | dedicated DB (do not share with the Java registry's schema) |
| `VAULT_ADDR` / `VAULT_API_ADDR` / `VAULT_ADDRESS` | Vault reachability |
| `VAULT_TOKEN` | scoped policy token in production, not the root token |
| `VAULT_BASE_URL` | e.g. `http://vault:8200/v1/` |
| `VAULT_ROOT_PATH` | KV mount path, e.g. `kv` |
| `SIGNING_ALGORITHM` | e.g. `Ed25519Signature2020` — governs the JSON-LD (`ldp_vc`) path; ES256/EC keys for the other formats are generated per-DID on demand |
| `ENABLE_AUTH` | enforced here via `src/auth/auth.guard.ts` |
| `WEB_DID_BASE_URL` | only relevant if issuing `did:web` identifiers |

**DID key types**, chosen via `/did/generate`'s `keyPairType`:

| `keyPairType` | Used for |
|---|---|
| `Ed25519VerificationKey2020` / `2018` | `ldp_vc` |
| `RsaVerificationKey2018` | RSA-based `ldp_vc` |
| `JsonWebKey2020` (EC P-256) | `mso_mdoc` issuance — mandatory, since mdoc's COSE_Sign1 requires ES256 |

ES256 JWK keys for `jwt_vc_json`/`vc+sd-jwt` are provisioned automatically
per-DID on first use; no separate generation step is needed for those two.

### 4.3 `credential-schema`

Per-schema OID4VCI opt-in is set via the schema payload's `oid4vciConfig`:

```json
"oid4vciConfig": {
  "oid4vciEnabled": true,
  "oid4vciFormats": ["ldp_vc", "jwt_vc_json", "vc+sd-jwt", "mso_mdoc"],
  "vct": "...",
  "display": [{ "name": "...", "locale": "en-US" }],
  "mdoc": { "docType": "org.iso.18013.5.1.mDL", "namespace": "org.iso.18013.5.1" },
  "renderMethod": { "type": "SvgRenderingTemplate", "svg": "<svg>...</svg>" }
}
```

`mdoc` and `renderMethod` are detailed in [§7](#7-mso_mdoc--w3c-vc-render-method).
`GET /credential-schema/oid4vci-configs` lists everything currently opted in.

### 4.4 `credentials-service`

| Env var | Purpose |
|---|---|
| `DATABASE_URL` | dedicated DB |
| `IDENTITY_BASE_URL` / `SCHEMA_BASE_URL` | dependency URLs |
| `STATUS_LIST_ENABLED` | turns on `credentialStatus`/revocation-list checks — recommended `true` in production so OID4VP's `revocation` check means something |

### 4.5 Java registry (optional — only if wiring automatic offers)

| Env var | Default | Purpose |
|---|---|---|
| `oid4vc_enabled` | `false` | turns on the offer-creation hooks |
| `oid4vc_offer_url` | `http://localhost:3400/oid4vc/offer` | must be the container/network-reachable URL, e.g. `http://oid4vc-service:3400/oid4vc/offer` |

Also required (pre-existing V2 signing prerequisites): `signature_enabled=true`,
`signature_provider=dev.sunbirdrc.registry.service.impl.SignatureV2ServiceImpl`,
and (for attestation-triggered issuance) `did_enabled=true`, `claims_enabled=true`.

### 4.6 nginx (public gateway)

| Location | Proxies to |
|---|---|
| `/oid4vc` | `oid4vc-service:3400` (offer, token, nonce, credential, deferred, notification) |
| `/vp` | `oid4vc-service:3400` |
| `/.well-known/openid-credential-issuer` etc. | issuer/AS metadata |
| `/contexts` | dynamic JSON-LD context documents (for `ldp_vc` custom claims) |
| `/render-templates` | W3C VC Render Method inline SVG templates |

---

## 5. Local Deployment (Quick Start)

### 5.1 Prerequisites

- Docker Desktop running, `docker compose` v2 available.
- On Apple Silicon / arm64 hosts: `identity`, `credential-schema`, and
  `credential` use Prisma 4.8.1, which has no native arm64-musl (Alpine)
  query engine — build these **with `--platform linux/amd64`**.
  `oid4vc-service` itself has no native deps and builds natively on any arch.

### 5.2 Create `.env` (repo root)

```
RELEASE_VERSION=latest
POSTGRES_PASSWORD=postgres
VAULT_TOKEN=
VAULT_ADDR=http://vault:8200
VAULT_API_ADDR=http://vault:8200
VAULT_ADDRESS=http://vault:8200
VAULT_BASE_URL=http://vault:8200/v1/
VAULT_ROOT_PATH=kv
VAULT_TIMEOUT=5000
VAULT_PROXY=false
IDENTITY_BASE_URL=http://identity:3332
SCHEMA_BASE_URL=http://credential-schema:3333
CREDENTIAL_SERVICE_BASE_URL=http://credential:3000
SIGNING_ALGORITHM=Ed25519Signature2020
WEB_DID_BASE_URL=http://localhost:3332
ENABLE_AUTH=false

OID4VC_PUBLIC_URL=http://localhost:3400
OID4VC_REDIS_URL=redis://redis:6379
OID4VC_SESSION_STORE=memory
OID4VC_ISSUER_DID=
OID4VP_ENABLED=true
OID4VC_DRAFT13_COMPAT=false
```

### 5.3 Build the V2 service images (arm64 hosts: add `--platform linux/amd64`)

```bash
docker build --platform linux/amd64 -t sunbird-rc-identity:local ./services/identity-service
docker build --platform linux/amd64 -t sunbird-rc-credential-schema:local ./services/credential-schema
docker build --platform linux/amd64 -t sunbird-rc-credential:local ./services/credentials-service
```

### 5.4 Bring up the base + V2 stack (no Java registry required)

```bash
docker compose up -d db vault redis
bash enable-v2.sh     # initializes/unseals vault, creates per-service DBs,
                       # starts identity -> credential-schema -> credential in order
docker ps --format '{{.Names}}\t{{.Status}}'   # expect all "healthy"
```

### 5.5 Build and deploy oid4vc-service

```bash
docker compose --profile oid4vc up -d --build oid4vc-service
curl -s http://localhost:3400/health
# {"status":"UP","service":"oid4vc-service"}
```

### 5.6 Run without Docker (dev iteration)

```bash
npm install
npm run build
npm run start:prod     # or start:dev for watch mode
# Swagger UI at /api
```

### 5.7 Cleanup

```bash
docker compose --profile oid4vc down
docker compose down
```

Vault unseal keys/root token are written to `keys.txt` at the repo root by
`enable-v2.sh` — gitignored; delete it when done if you'd rather not keep
dev secrets on disk.

---

## 6. Manual Testing Guide

Step-by-step commands to exercise every flow manually, useful for
onboarding and regression-checking after changes.

### 6.1 Health & discovery

```bash
curl -s http://localhost:3400/health
curl -s http://localhost:3400/.well-known/openid-credential-issuer
curl -s http://localhost:3400/.well-known/openid-configuration
curl -s http://localhost:3400/.well-known/jwks.json
```

`jwks.json` returns `{"keys":[]}` until the façade mints its first access
token — its signing key is generated lazily on first use.

### 6.2 Generate an issuer DID

```bash
curl -s -X POST http://localhost:3332/did/generate -H "Content-Type: application/json" \
  -d '{"content":[{"alsoKnownAs":["oid4vc-pilot-issuer"],"services":[],"method":"rcw"}]}'
```

Save the returned `id` as `$ISSUER_DID`. For `mso_mdoc` schemas, add
`"keyPairType":"JsonWebKey2020"` to generate an EC P-256 key instead.

### 6.3 Opt a schema into OID4VCI

```bash
curl -s -X POST http://localhost:3333/credential-schema -H "Content-Type: application/json" -d '{
  "schema": {
    "type": "https://w3c-ccg.github.io/vc-json-schemas/",
    "version": "1.0.0",
    "id": "<uuid>",
    "name": "OID4VC Pilot Credential",
    "author": "'"$ISSUER_DID"'",
    "authored": "2026-07-22T00:00:00.000Z",
    "schema": {
      "$id": "OID4VC-Pilot-Credential-1.0",
      "$schema": "https://json-schema.org/draft/2019-09/schema",
      "type": "object",
      "properties": {"name": {"type": "string"}},
      "required": ["name"],
      "additionalProperties": true
    }
  },
  "tags": ["oid4vc-pilot"],
  "status": "PUBLISHED",
  "oid4vciConfig": {
    "oid4vciEnabled": true,
    "oid4vciFormats": ["ldp_vc", "jwt_vc_json"],
    "display": [{"name": "OID4VC Pilot Credential", "locale": "en-US"}]
  }
}'
curl -s http://localhost:3333/credential-schema/oid4vci-configs
```

### 6.4 Full pre-authorized_code issuance flow (`jwt_vc_json`)

**1 — create the offer** (use the schema's `schemaId` from step 6.3's
response, or its `name`):
```bash
curl -s -X POST http://localhost:3400/oid4vc/offer -H "Content-Type: application/json" -d '{
  "credential_configuration_id": "OID4VC Pilot Credential",
  "format": "jwt_vc_json",
  "claims": {"name": "Test Holder"}
}'
```
Save `pre-authorized_code` as `$CODE`.

**2 — exchange the code for a token:**
```bash
curl -s -X POST http://localhost:3400/oid4vc/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:pre-authorized_code&pre-authorized_code=$CODE"
```
Save `access_token` as `$TOKEN` and `c_nonce` as `$NONCE`.

**3 — build a holder PoP JWT** (inline JWK, self-contained holder key):
```js
// node REPL, or a small script
const jose = require("jose");
const { publicKey, privateKey } = await jose.generateKeyPair("ES256", { extractable: true });
const publicJwk = await jose.exportJWK(publicKey);
publicJwk.alg = "ES256";
const jwt = await new jose.SignJWT({ aud: "http://localhost:3400", nonce: "<c_nonce>" })
  .setProtectedHeader({ alg: "ES256", typ: "openid4vci-proof+jwt", jwk: publicJwk })
  .setIssuedAt()
  .sign(privateKey);
```

**4 — request the credential:**
```bash
curl -s -X POST http://localhost:3400/oid4vc/credential \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"proof":{"proof_type":"jwt","jwt":"'"$POP_JWT"'"}}'
```
Decoding the returned JWT shows `vc.credentialSubject.id`/`sub` set to the
holder's `did:jwk:...`, proving holder binding.

**5 — verify the issued credential:**
```bash
curl -s -X POST http://localhost:3000/credentials/verify \
  -H "Content-Type: application/json" \
  -d '{"verifiableCredential":"<the credential string from step 4>"}'
# {"checks":[{"expired":"OK","proof":"OK"}]}
```

### 6.5 Full OID4VP verifier flow

**1 — verifier creates a DCQL presentation request:**
```bash
curl -s -X POST http://localhost:3400/vp/request -H "Content-Type: application/json" -d '{
  "dcql_query": {
    "credentials": [
      {"id": "pilot_cred", "format": "jwt_vc_json", "meta": {"type_values": [["VerifiableCredential", "OID4VC Pilot Credential"]]}, "claims": [{"path": ["credentialSubject", "name"]}]}
    ]
  }
}'
```

**2 — wallet fetches the request object:**
```bash
curl -s http://localhost:3400/vp/request-object/<id>
```

**3 — wallet builds and submits the vp_token** (a JWT signed by the same
holder key, embedding `vp.verifiableCredential: [<credential>]` and the
request's `nonce`), then:
```bash
curl -s -X POST http://localhost:3400/vp/response -H "Content-Type: application/json" \
  -d '{"state":"<state>","vp_token":"<signed vp jwt>"}'
```

**4 — verifier polls the result:**
```bash
curl -s http://localhost:3400/vp/status/<transaction_id>
```
```json
{
  "status": "verified",
  "verified": true,
  "checks": {
    "holderSignature": "OK", "nonce": "OK", "credentialSignatures": "OK",
    "holderBinding": "OK", "revocation": "OK", "dcql": "OK"
  },
  "claims": {"pilot_cred": {"name": "Test Holder"}},
  "holderDid": "did:jwk:..."
}
```

### 6.6 Error responses reference

| Scenario | Response |
|---|---|
| Reuse a `pre-authorized_code` | `400 {"message":"invalid_grant: bad or used code"}` |
| Missing `proof.jwt` | `400 {"message":"Missing proof.jwt"}` |
| Invalid/garbage bearer token | `401 {"message":"Invalid access token"}` |
| No `Authorization` header | `401 {"message":"Missing bearer token"}` |
| PoP `nonce` mismatch | `400 {"error":"invalid_or_missing_proof","c_nonce":"<fresh nonce>"}` |
| PoP `aud` mismatch | `400 {"message":"invalid_proof: PoP audience mismatch"}` |
| Unknown `credential_configuration_id` | `404 {"message":"Credential configuration 'X' not enabled for OID4VCI"}` |
| Format not enabled for that schema | `400 {"message":"Format 'X' not supported for this credential"}` |
| Unknown/expired offer id | `404 {"message":"Offer not found or expired"}` |
| Unsupported `grant_type` | `400 {"message":"unsupported_grant_type"}` |
| Tampered credential on verify | `200 {"checks":[{"expired":"OK","proof":"NOK"}]}` |
| VP with unknown/bogus `state` | `400 {"message":"unknown or expired state"}` |
| VP missing `state` | `400 {"message":"missing state"}` |
| Unknown VP transaction | `404 {"message":"VP transaction not found"}` |
| VP token with wrong `nonce` | `403 {"error":"Error: nonce mismatch"}` |
| DCQL claim not present | `403 {"error":"Error: DCQL not satisfied: claim X missing for query Y"}` |
| Holder-binding mismatch (impostor) | `403 {"error":"Error: holder binding failed: subject != presenter"}` |
| Replay an already-verified VP | `400 {"message":"transaction not pending"}` |

Every VP failure returns a partial `checks` map showing exactly which
validations passed before the failing one — useful for pinpointing what a
real wallet/verifier got wrong.

---

## 7. `mso_mdoc` & W3C VC Render Method

### 7.1 `mso_mdoc` (ISO/IEC 18013-5 mobile documents)

Unlike the other three formats, `mso_mdoc` is CBOR-encoded and COSE-signed
(not JOSE/JSON-LD), organizes claims under
`{docType, namespaces: {namespace: {elementIdentifier: value}}}` instead of
a flat `credentialSubject`, and uses its own device-binding/session-transcript
mechanism for presentation rather than a JWT-VP wrapper. Built on
[`@auth0/mdl`](https://github.com/auth0-lab/mdl).

**Prerequisites:**
- The issuing DID must have an EC P-256 (`JsonWebKey2020`) verification
  method — generate one via `/did/generate` with
  `"keyPairType":"JsonWebKey2020"` (see [§4.2](#42-identity-service)).
- Issuance uses a fresh, self-signed X.509 certificate wrapping that key for
  each credential's COSE_Sign1 `issuerAuth` structure (this deployment has no
  external IACA root chain — verification correspondingly trusts the
  deployment's own DID keys directly, consistent with how the other three
  formats work).

**Schema config:**
```json
"oid4vciConfig": {
  "oid4vciEnabled": true,
  "oid4vciFormats": ["mso_mdoc"],
  "mdoc": {
    "docType": "org.iso.18013.5.1.mDL",
    "namespace": "org.iso.18013.5.1",
    "elementMapping": {
      "custom_claim_name": { "namespace": "org.iso.18013.5.1", "elementIdentifier": "given_name" }
    }
  }
}
```
- `docType` / `namespace` are required.
- `elementMapping` is optional — remaps a specific claim name to a different
  `{namespace, elementIdentifier}` pair when it doesn't already match an
  ISO-registered element identifier in the default namespace.

**Issuing and presenting** follow the same OID4VCI/OID4VP endpoints as the
other formats ([§3](#3-api-flow-design)/[§6](#6-manual-testing-guide)), with
two differences:
- The credential response's `credential` field is a base64url-encoded CBOR
  `IssuerSigned` structure instead of a JWT/SD-JWT string.
- The DCQL query for an mdoc credential uses `meta.doctype_value` (a single
  string, not an array) and 2-segment claim paths `[namespace,
  elementIdentifier]` (no `credentialSubject` prefix):
  ```json
  { "id": "mdl_cred", "format": "mso_mdoc", "meta": {"doctype_value": "org.iso.18013.5.1.mDL"}, "claims": [{"path": ["org.iso.18013.5.1", "given_name"]}] }
  ```
- The presentation `vp_token` is a base64url-encoded CBOR `DeviceResponse`
  (built with `@auth0/mdl`'s `DeviceResponse` class on the wallet side), and
  the `/vp/response` request body needs one extra field,
  `mdoc_generated_nonce` — a wallet-generated nonce folded into the mdoc
  session transcript alongside the request's own `nonce`/`client_id`/
  `response_uri`.

### 7.2 W3C VC Render Method

Lets a schema declare how its issued credentials should be visually
rendered by a wallet, per the
[W3C VC Render Method](https://www.w3.org/TR/vc-render-method/) spec.
Applies to `ldp_vc` and `jwt_vc_json` (both carry a full W3C-shaped VC
object); `vc+sd-jwt` and `mso_mdoc` have no analogous mechanism wired here.

**Schema config** — either reference an already-hosted template, or supply
inline SVG for `oid4vc-service` to host itself:
```json
"oid4vciConfig": {
  "renderMethod": {
    "type": "SvgRenderingTemplate",
    "name": "My Credential Card",
    "svg": "<svg xmlns='http://www.w3.org/2000/svg' ...>{{credentialSubject.name}}</svg>",
    "cssMediaQuery": "@media (min-width: 480px)"
  }
}
```
When `svg` is provided, it's served at `GET /render-templates/:schemaId`
and the issued credential's `renderMethod[0].id` points to that URL, with a
`digestMultibase` (multibase/multihash-encoded SHA-256 digest of the SVG
content) for wallets to verify the template hasn't been tampered with. When
`url` is provided instead, it's used as-is with no digest computed.

---

## 8. Production Deployment Guide

### 8.1 Hardening checklist

- **Restrict `POST /oid4vc/offer` to internal callers only.** It's designed
  to be called by the registry/issuer backend, not the public internet —
  put it behind a network policy (internal CIDR allowlist, mTLS, or a
  shared-secret header) rather than the same public nginx location as the
  wallet-facing routes.
- **TLS + rate limiting on the public gateway.** OID4VCI/OID4VP both assume
  HTTPS in their metadata URLs and PoP/request-object audience checks. Add
  rate limiting at minimum on `/oid4vc/token`, `/oid4vc/credential`,
  `/oid4vc/nonce`, `/vp/response`.
- **Pin `ISSUER_DID`** (or ensure every schema declares its own `author`).
  An auto-generated ephemeral DID changes on every restart, invalidating
  every previously-minted access token.
- **`SESSION_STORE=redis`**, pointed at a persistent, access-controlled
  Redis instance — required for multi-replica deployments and recommended
  even for a single replica (survives restarts).
- **Vault production posture:** use a proper storage backend (Raft/Consul)
  and auto-unseal (cloud KMS) rather than file storage + manual unseal keys;
  enable TLS on Vault's listener; scope the token identity-service uses down
  to only the paths it needs.
- **`STATUS_LIST_ENABLED=true`** on `credentials-service` if revocation
  should mean anything for OID4VP's `revocation` check.
- **Key rotation:** decide and document a rotation cadence for the
  underlying DID keys — not implemented automatically.

### 8.2 Deployment procedure

```bash
# 1. .env with production values:
#    PUBLIC_URL=https://your-domain/oid4vc
#    SESSION_STORE=redis, REDIS_URL=<real redis>
#    ISSUER_DID=<pinned>
#    STATUS_LIST_ENABLED=true
#    oid4vc_enabled=true, oid4vc_offer_url=http://oid4vc-service:3400/oid4vc/offer  (registry only)

# 2. Bring up prerequisites
docker compose up -d db vault redis

# 3. Init/unseal vault (first run only), start identity/credential-schema/credential
bash enable-v2.sh

# 4. Build/deploy oid4vc-service
docker compose --profile oid4vc up -d --build oid4vc-service

# 5. Confirm health
curl -sf https://<your-domain>/health
curl -s https://<your-domain>/.well-known/openid-credential-issuer
```

Then apply the network-restriction and TLS/rate-limiting items above at the
nginx layer before exposing publicly, and opt in the schemas you intend to
issue via `oid4vciConfig` ([§4.3](#43-credential-schema)).

---

## 9. Wallet Interoperability

### 9.1 MOSIP Inji Wallet

Two integration paths, not interchangeable:

- **Path A — direct Credential Offer (Issuer-Initiated), pre-authorized_code.**
  This is what `oid4vc-service` implements. MOSIP's `inji-vci-client`
  libraries support this flow directly — scan a QR encoding
  `openid-credential-offer://...`, no pre-registration needed.
- **Path B — the published Inji Wallet app, via its Mimoto backend.** The
  consumer app fetches a fixed issuer list from Mimoto
  (`mimoto-issuers-config.json`); its example issuer config is built around
  an `authorization_code` grant via a separate OIDC/OAuth server, which
  `oid4vc-service` does not implement (it only supports pre-authorized_code
  and is its own authorization server for that grant). Path A is the
  supported, recommended target.

**Steps (Path A):**
1. Set `DRAFT13_COMPAT_MODE=true` and redeploy.
2. Opt a schema into OID4VCI with `ldp_vc` or `jwt_vc_json`.
3. Confirm draft-13 shaped metadata (`credentials_supported`, not
   `credential_configurations_supported`).
4. Create an offer, render its `qr_data` as a QR code, scan with the
   Inji-compatible wallet/client.
5. Confirm `GET /oid4vc/offer/:id` → `POST /oid4vc/token` →
   `POST /oid4vc/credential` land from the wallet's IP, and the credential
   appears in the wallet's UI.

### 9.2 EUDI Reference Wallet

The [EUDI reference wallet](https://github.com/eu-digital-identity-wallet)
(part of the EU's ARF reference implementation) fits this codebase with no
compat-mode flag and no intermediary backend:
- Supports adding **any issuer by URL directly** (dynamic discovery against
  `.well-known/openid-credential-issuer`), no static pre-registration.
- Natively scans the same Credential Offer deep-link format
  `oid4vc-service` produces.
- Targets final OID4VCI/OID4VP (this service's default mode).
- Uses **DCQL** on the presentation side, matching this implementation.

The EU wallet ecosystem is built primarily around `mso_mdoc` and
`vc+sd-jwt` — both are fully supported end-to-end by this service
([§7](#7-mso_mdoc--w3c-vc-render-method)). Recommend testing with one of
those two formats for this wallet rather than `ldp_vc`/`jwt_vc_json`.

**One thing to verify per test:** the EUDI Android wallet's own
documentation describes its presentation support as a specific OID4VP
draft version, which may differ in a few request/response details from
final 1.0 (e.g. encrypted vs. plain `direct_post`). Confirm this against
your specific wallet build during the first test.

**Steps:**
1. Opt a schema into OID4VCI with `"oid4vciFormats": ["vc+sd-jwt"]` (or
   `["mso_mdoc"]`).
2. Confirm final-1.0 shaped metadata (default, no flag needed).
3. Create an offer, scan the `qr_data` QR with the EUDI wallet app.
4. For OID4VP: create a `POST /vp/request` DCQL query matching the
   credential's disclosable claims, scan the resulting `qr_data`
   (`openid4vp://...`), and confirm `GET /vp/status/:id` returns
   `verified: true` with all six checks `OK`.

### 9.3 walt.id Wallet

[walt.id](https://walt.id/wallet) offers a hosted demo wallet
(`wallet.demo.walt.id`) with no local deployment or intermediary backend —
its credential-offer acceptance API takes the same
`openid-credential-offer://...` deep link this service produces, with no
issuer pre-registration required. It supports `jwt_vc_json` and
`vc+sd-jwt` for issuance.

**Known limitation:** walt.id's wallet currently implements Presentation
Exchange (PEX) for presentations, not DCQL — since this service implements
only DCQL, OID4VP/presentation testing against walt.id's wallet is not
possible today. Use it for issuance testing only, and the EUDI reference
wallet (§9.2) for presentation testing.

---

## 10. End-to-End Verification Evidence

A self-driven Node.js script (using `jose` and `@auth0/mdl`) exercised the
complete issuance → verification → DCQL-gated presentation flow for all
four supported formats, directly against a deployed instance — generating
a holder/device key to stand in for a real wallet, so the results are
independent of any third-party wallet's own behavior.

| Format | Offer | Token | Credential Issue | Standalone Verify | VP Submit | Final Status |
|---|---|---|---|---|---|---|
| `ldp_vc` | ✅ 201 | ✅ 200 | ✅ 200 | ✅ proof: OK | ✅ 200 | ✅ **verified: true**, all 6 checks OK |
| `jwt_vc_json` | ✅ 201 | ✅ 200 | ✅ 200 | ✅ proof: OK | ✅ 200 | ✅ **verified: true**, all 6 checks OK |
| `vc+sd-jwt` | ✅ 201 | ✅ 200 | ✅ 200 | ✅ proof: OK | ✅ 200 | ✅ **verified: true**, all 6 checks OK |
| `mso_mdoc` | ✅ 201 | ✅ 200 | ✅ 200 | ✅ proof: OK | ✅ 200 | ✅ **verified: true**, all 6 checks OK |

Sample final status (`GET /vp/status/:id`) for each format:

<details>
<summary><code>ldp_vc</code> / <code>jwt_vc_json</code> / <code>vc+sd-jwt</code> — click to expand</summary>

```json
{
  "status": "verified",
  "verified": true,
  "checks": {
    "holderSignature": "OK", "nonce": "OK", "credentialSignatures": "OK",
    "holderBinding": "OK", "revocation": "OK", "dcql": "OK"
  },
  "claims": { "age_cred": { "name": "Full Flow Test", "age_over_18": true } }
}
```
</details>

<details>
<summary><code>mso_mdoc</code> — click to expand</summary>

```json
{
  "status": "verified",
  "verified": true,
  "checks": {
    "holderSignature": "OK", "nonce": "OK", "credentialSignatures": "OK",
    "holderBinding": "OK", "revocation": "OK", "dcql": "OK"
  },
  "claims": { "mdl_cred": { "org.iso.18013.5.1.given_name": "Jane" } }
}
```
</details>

All four formats complete the full issuance → verification → DCQL-gated
presentation flow correctly, end-to-end.
