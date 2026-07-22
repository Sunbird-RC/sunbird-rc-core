# oid4vc-service — Manual Test Guide

This document is a step-by-step guide to deploy `oid4vc-service` via Docker and
manually exercise every OID4VCI / OID4VP flow it supports, including negative
paths. It reflects a real run performed against this branch (`oid4vc`) and
every request/response shown below is a **verified, actual result** — not a
hypothetical example.

Use it to (a) stand the service up locally, (b) understand the protocol flow
end-to-end, and (c) regression-test after making changes.

---

## 1. Architecture / Flow Overview

`oid4vc-service` is a protocol façade. It does not sign or store credentials
itself — it speaks OID4VCI/OID4VP on the outside and delegates to existing
services on the inside:

```
 Wallet                oid4vc-service                 identity-service   credentials-service   credential-schema
   |                        |                                |                    |                    |
   |--GET issuer metadata-->|<--------- oid4vci-configs ----------------------------------------------->|
   |                        |
   |  (issuer) POST /oid4vc/offer  ---->  session stored (Redis/memory)
   |<--- credential_offer (QR) ----------|
   |
   |--POST /oid4vc/token (pre-auth code)->|--- mints access token (ES256, signed via) ------>|
   |<---------- access_token, c_nonce ----|
   |
   |--POST /oid4vc/credential (Bearer + PoP JWT)-->|
   |                        |--verify PoP (holder DID/JWK)-->|
   |                        |--POST /credentials/issue (format) -------------------------->|
   |                        |                                |<--sign (Ed25519 or ES256)---|
   |<----- signed VC (ldp_vc JSON-LD or jwt_vc_json compact JWT) -------------------------|

 Verifier               oid4vc-service                                  Wallet
   |--POST /vp/request (DCQL)-->|--sign JAR (identity-service)-->
   |<--- request_uri, QR -------|
   |                            |<--- GET /vp/request-object/:id --- Wallet
   |                            |<--- POST /vp/response (vp_token) - Wallet
   |                            |--verify holder sig, nonce, embedded VC (credentials-service /verify),
   |                            |  status, holder-binding, DCQL satisfaction
   |--GET /vp/status/:id ------>|
   |<--- {verified, checks[]} --|
```

**Key services involved (T2 topology — no Java registry needed):**

| Service | Port | Role |
|---|---|---|
| `db` (Postgres) | 5432 | shared DB, dedicated `identity`/`credential_schema`/`credential` databases |
| `vault` | 8200 | holds all signing keys (Ed25519 + ES256) |
| `redis` | 6379 | session store backend (optional; `memory` works for single instance) |
| `identity` | 3332 | DID generation/resolution, `/utils/sign`, `/utils/sign-jwt`, `/.well-known/jwks.json` |
| `credential-schema` | 3333 | schema CRUD, `oid4vciConfig` opt-in, `/credential-schema/oid4vci-configs` |
| `credential` | 3000 | `/credentials/issue`, `/credentials/verify` (format-aware: `ldp_vc` / `jwt_vc_json`) |
| `oid4vc-service` | 3400 | this service — `/oid4vc/*` (OID4VCI) and `/vp/*` (OID4VP) |

---

## 2. Prerequisites

- Docker Desktop running, `docker compose` v2 available.
- Repo checked out on the branch that contains the OID4VC implementation.
- On Apple Silicon / arm64 hosts: `identity`, `credential-schema`, and
  `credential` use Prisma 4.8.1, which has no native arm64-musl (Alpine) query
  engine. Build these **with `--platform linux/amd64`** or they fail at
  `npx prisma generate` with `Error loading shared library ld-linux-aarch64.so.1`.
  `oid4vc-service` itself has no native deps and builds natively on any arch.

---

## 3. Deploy the stack

### 3.1 Create `.env` (repo root)

No `.env` ships in the repo (only `.env.example`, which is gitignored-adjacent
and not meant to be read/committed with real secrets). Create `.env` with at
least:

```
RELEASE_VERSION=latest
POSTGRES_PASSWORD=postgres
ELASTIC_SEARCH_PASSWORD=elastic123
KEYCLOAK_ADMIN_USER=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
KEYCLOAK_DEFAULT_USER_PASSWORD=admin123
KEYCLOAK_SECRET=
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123
DIGILOCKER_HMAC_AUTHKEY=changeme

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
JWKS_URI=
QR_TYPE=W3C_VC

OID4VC_PUBLIC_URL=http://localhost:3400
OID4VC_REDIS_URL=redis://redis:6379
OID4VC_SESSION_STORE=memory
OID4VC_ISSUER_DID=
OID4VP_ENABLED=true
OID4VC_DRAFT13_COMPAT=false
```

### 3.2 Build the V2 service images (arm64 hosts: add `--platform linux/amd64`)

```bash
docker build --platform linux/amd64 -t sunbird-rc-identity:local ./services/identity-service
docker build --platform linux/amd64 -t sunbird-rc-credential-schema:local ./services/credential-schema
docker build --platform linux/amd64 -t sunbird-rc-credential:local ./services/credentials-service
```

### 3.3 Bring up the base + V2 stack (no Java registry required)

```bash
docker compose up -d db vault redis
bash enable-v2.sh     # initializes/unseals vault, creates per-service DBs,
                       # starts identity -> credential-schema -> credential in order,
                       # then tries claim-ms/registry/keycloak — Ctrl-C or ignore
                       # failures there if you don't need the Java registry (T2 topology)
```

Confirm all four are healthy:

```bash
docker ps --format '{{.Names}}\t{{.Status}}'
# expect: db, vault, identity, credential-schema, credential all "healthy"
```

### 3.4 Build and deploy oid4vc-service

```bash
docker compose --profile oid4vc up -d --build oid4vc-service
docker ps --format '{{.Names}}\t{{.Status}}' | grep oid4vc
# expect: sunbird-rc-core-oid4vc-service-1   Up ... (healthy)
```

---

## 4. Positive Test Cases

### P1 — Health check
```bash
curl -s http://localhost:3400/health
```
**Expected:** `{"status":"UP","service":"oid4vc-service"}`

### P2 — Discovery endpoints
```bash
curl -s http://localhost:3400/.well-known/openid-credential-issuer
curl -s http://localhost:3400/.well-known/openid-configuration
curl -s http://localhost:3400/.well-known/jwks.json
```
**Expected:**
- issuer metadata has `credential_configurations_supported: {}` until a schema opts in.
- AS metadata advertises `grant_types_supported: ["urn:ietf:params:oauth:grant-type:pre-authorized_code"]`.
- `jwks.json` returns `{"keys":[]}` until the façade mints its first access token (its ES256 key is generated **lazily** on first `signJwt` call — this is expected, not a bug).

### P3 — Generate an issuer DID
```bash
curl -s -X POST http://localhost:3332/did/generate \
  -H "Content-Type: application/json" \
  -d '{"content":[{"alsoKnownAs":["oid4vc-pilot-issuer"],"services":[],"method":"rcw"}]}'
```
**Expected:** returns a DID document with `id: "did:rcw:<uuid>"`. Save this as `$ISSUER_DID`.

### P4 — Opt a schema into OID4VCI (both formats)
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
```
Then verify opt-in took effect:
```bash
curl -s http://localhost:3333/credential-schema/oid4vci-configs
```
**Expected:** JSON array containing your schema with `"formats":["ldp_vc","jwt_vc_json"]`.

### P5 — Issuer metadata reflects the new schema
```bash
curl -s http://localhost:3400/.well-known/openid-credential-issuer
```
**Expected:** `credential_configurations_supported` now has two keys:
`"OID4VC Pilot Credential_ldp_vc"` and `"OID4VC Pilot Credential_jwt_vc_json"`
(the `_<format>` suffix appears because the schema supports more than one format).

### P6 — Full pre-authorized_code issuance flow (`jwt_vc_json`)

**Step 1 — create the offer:**
```bash
curl -s -X POST http://localhost:3400/oid4vc/offer -H "Content-Type: application/json" -d '{
  "credential_configuration_id": "OID4VC Pilot Credential",
  "format": "jwt_vc_json",
  "claims": {"name": "Test Holder"}
}'
```
**Expected:** `{"offer_id":...,"credential_offer_uri":...,"credential_offer":{"credential_issuer":...,"credential_configuration_ids":["OID4VC Pilot Credential_jwt_vc_json"],"grants":{"urn:ietf:params:oauth:grant-type:pre-authorized_code":{"pre-authorized_code":"..."}}},"qr_data":"openid-credential-offer://..."}`
Save `pre-authorized_code` as `$CODE`.

**Step 2 — exchange the code for a token:**
```bash
curl -s -X POST http://localhost:3400/oid4vc/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:pre-authorized_code&pre-authorized_code=$CODE"
```
**Expected:** `{"access_token":"...","token_type":"Bearer","expires_in":300,"c_nonce":"...","c_nonce_expires_in":300}`
Save `access_token` as `$TOKEN` and `c_nonce` as `$NONCE`.

**Step 3 — build a holder PoP JWT.** Two valid proof shapes are supported:

*(a) inline JWK (self-contained holder key, no DID needed):*
```js
// run inside the oid4vc-service container: docker exec -it <container> node
const jose = require("jose");
const { publicKey, privateKey } = await jose.generateKeyPair("ES256", { extractable: true });
const publicJwk = await jose.exportJWK(publicKey);
publicJwk.alg = "ES256"; publicJwk.use = "sig";
const jwt = await new jose.SignJWT({ aud: "http://localhost:3400", nonce: "<c_nonce>", iss: "did:example:holder-1" })
  .setProtectedHeader({ alg: "ES256", typ: "openid4vci-proof+jwt", jwk: publicJwk })
  .setIssuedAt()
  .sign(privateKey);
```
> **Note:** `iss` is required in this branch — it becomes the holder DID bound
> into `credentialSubject.id`. Omitting it produces a credential with no
> subject `id`, which currently breaks storage (see Negative case N-extra
> below / Known Issues).

*(b) `kid` + identity-service-resolvable DID (closer to a real wallet with a registered DID):*
```bash
HOLDER_DID=$(curl -s -X POST http://localhost:3332/did/generate -H "Content-Type: application/json" \
  -d '{"content":[{"alsoKnownAs":["holder"],"services":[],"method":"rcw"}]}' | jq -r '.[0].id')
curl -s -X POST http://localhost:3332/utils/sign-jwt -H "Content-Type: application/json" -d '{
  "DID": "'"$HOLDER_DID"'",
  "payload": {"aud": "http://localhost:3400", "nonce": "'"$NONCE"'"},
  "header": {"typ": "openid4vci-proof+jwt"}
}'
```
This lazily provisions an ES256 key for `$HOLDER_DID` in Vault and returns
`{"jwt": "<PoP JWT with kid=$HOLDER_DID#jwt-key-1>"}`.

**Step 4 — request the credential:**
```bash
curl -s -X POST http://localhost:3400/oid4vc/credential \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"proof":{"proof_type":"jwt","jwt":"'"$POP_JWT"'"}}'
```
**Expected:** `{"credential":"<compact JWT>","c_nonce":"...","format":"jwt_vc_json"}`.
Decoding the JWT payload shows `vc.credentialSubject.id` set to the holder DID
and `sub` matching it — proving holder binding worked.

**Step 5 — verify the issued credential:**
```bash
curl -s -X POST http://localhost:3000/credentials/verify \
  -H "Content-Type: application/json" \
  -d '{"verifiableCredential":"<the credential string from step 4>"}'
```
**Expected:** `{"checks":[{"expired":"OK","proof":"OK"}]}`

> **DTO gotcha:** the field name is `verifiableCredential`, not `credential`.
> Passing the wrong field name silently produces `{"errors":[{}]}` with an
> unhelpful empty error object (worth hardening — see Known Issues).

### P7 — `ldp_vc` format issuance (same offer→token→PoP→credential flow, `"format":"ldp_vc"`)
Works identically **as long as every `credentialSubject` property is defined
in the VC's JSON-LD context** (e.g. `name` alone, with only the base
`https://www.w3.org/2018/credentials/v1` context, fails — see Known Issues,
this is pre-existing and unrelated to OID4VC).

### P8 — Full OID4VP verifier flow

**Step 1 — verifier creates a DCQL presentation request:**
```bash
curl -s -X POST http://localhost:3400/vp/request -H "Content-Type: application/json" -d '{
  "dcql_query": {
    "credentials": [
      {"id": "pilot_cred", "format": "jwt_vc_json", "claims": [{"path": ["name"]}]}
    ]
  }
}'
```
**Expected:** `{"transaction_id":"...","request_uri":"http://localhost:3400/vp/request-object/...","qr_data":"openid4vp://..."}`

**Step 2 — wallet fetches the signed request object (JAR):**
```bash
curl -s http://localhost:3400/vp/request-object/<id>
```
**Expected:** a compact JWS (`typ: oauth-authz-req+jwt`) whose payload contains
`nonce`, `state`, `dcql_query`, `response_uri`.

**Step 3 — wallet builds and submits the VP token** (same holder DID that
holds the credential from P6b; sign a JWT with `vp.verifiableCredential`
containing the credential string and `nonce` copied from the request object):
```bash
curl -s -X POST http://localhost:3332/utils/sign-jwt -H "Content-Type: application/json" -d '{
  "DID": "'"$HOLDER_DID"'",
  "payload": {"aud": "http://localhost:3400", "nonce": "'"$NONCE"'", "vp": {"verifiableCredential": ["'"$CRED"'"]}},
  "header": {"typ": "JWT"}
}'
curl -s -X POST http://localhost:3400/vp/response -H "Content-Type: application/json" \
  -d '{"state":"'"$STATE"'","vp_token":"'"$VP_JWT"'"}'
```
**Expected:** `{"redirect_uri":null,"status":"ok"}`

**Step 4 — verifier polls the result:**
```bash
curl -s http://localhost:3400/vp/status/<transaction_id>
```
**Expected:**
```json
{
  "status": "verified",
  "verified": true,
  "checks": {
    "holderSignature": "OK",
    "nonce": "OK",
    "credentialSignatures": "OK",
    "holderBinding": "OK",
    "revocation": "OK",
    "dcql": "OK"
  },
  "claims": {"pilot_cred": {"name": "..."}},
  "holderDid": "did:rcw:..."
}
```

---

## 5. Negative Test Cases

Every case below was run against the live stack; the response shown is the
**actual observed output**.

| # | Scenario | Command shape | Observed result |
|---|---|---|---|
| N1 | Reuse a pre-authorized_code (replay) | Call `/oid4vc/token` twice with the same code | 1st: `200` with token. 2nd: `400 {"message":"invalid_grant: bad or used code"}` |
| N2 | Missing `proof.jwt` on credential request | `POST /oid4vc/credential` with `{}` body | `400 {"message":"Missing proof.jwt"}` |
| N3 | Garbage/invalid bearer token | `Authorization: Bearer garbage.not.a.jwt` | `401 {"message":"Invalid access token"}` |
| N4 | No `Authorization` header at all | `POST /oid4vc/credential` with no auth header | `401 {"message":"Missing bearer token"}` |
| N5 | PoP proof `nonce` doesn't match the live `c_nonce` | Sign PoP with a made-up nonce | `400 {"error":"invalid_or_missing_proof","c_nonce":"<fresh nonce>"}` — service reissues a nonce for retry |
| N6 | PoP proof `aud` doesn't match the issuer's public URL | Sign PoP with `aud: "http://wrong-issuer.example"` | `400 {"message":"invalid_proof: PoP audience mismatch"}` |
| N7 | `credential_configuration_id` doesn't exist / not opted in | `POST /oid4vc/offer` with a bogus id | `404 {"message":"Credential configuration 'X' not enabled for OID4VCI"}` |
| N8 | Format requested isn't enabled for that schema | `POST /oid4vc/offer` with `"format":"vc+sd-jwt"` on a schema only opted into `ldp_vc`/`jwt_vc_json` | `400 {"message":"Format 'vc+sd-jwt' not supported for this credential"}` |
| N9 | Dereference an unknown/expired offer id | `GET /oid4vc/offer/00000000-0000-0000-0000-000000000000` | `404 {"message":"Offer not found or expired"}` |
| N10 | Unsupported `grant_type` | `POST /oid4vc/token` with `grant_type=authorization_code` | `400 {"message":"unsupported_grant_type"}` |
| N11 | Missing `pre-authorized_code` on token request | `POST /oid4vc/token` with only `grant_type` | `400 {"message":"invalid_request: missing pre-authorized_code"}` |
| N12 | Verify a tampered credential (flip one char in the JWS signature) | `POST /credentials/verify` with corrupted JWT | `200 {"checks":[{"expired":"OK","proof":"NOK"}]}` — fails cleanly, no exception |
| N13 | Verify a garbage/non-JWT string | `POST /credentials/verify` with `"not-a-real-jwt"` | `200 {"checks":[{"expired":"OK","proof":"NOK"}]}` |
| N14 | VP `direct_post` with an unknown/bogus `state` | `POST /vp/response` with random state | `400 {"message":"unknown or expired state"}` |
| N15 | VP `direct_post` missing `state` entirely | `POST /vp/response` with only `vp_token` | `400 {"message":"missing state"}` |
| N16 | Poll status of an unknown VP transaction | `GET /vp/status/00000000-0000-0000-0000-000000000000` | `404 {"message":"VP transaction not found"}` |
| N17 | VP token signed with the wrong `nonce` | Holder signs VP with a made-up nonce instead of the request's | `403 {"verified":false,"checks":{"holderSignature":"OK"},"error":"Error: nonce mismatch"}` |
| N18 | DCQL query asks for a claim the credential doesn't have | `dcql_query` requests `path:["nonexistent_claim"]` | `403 {"verified":false,"checks":{...all OK except missing...},"error":"Error: DCQL not satisfied: claim nonexistent_claim missing for query pilot_cred"}` |
| N19 | Holder-binding mismatch (a different DID presents someone else's credential) | A second, unrelated DID signs the VP but embeds the first holder's credential | `403 {"verified":false,"checks":{"holderSignature":"OK","nonce":"OK"},"error":"Error: holder binding failed: subject != presenter"}` |
| N20 | Replay an already-`verified` VP response | Resubmit the exact same `state` + `vp_token` after a successful verify | `400 {"message":"transaction not pending"}` |

**Reading the `checks` object:** every VP negative case returns a partial
`checks` map showing exactly which validations passed before the failing one
— useful for pinpointing what a real wallet/verifier got wrong (e.g. N17
shows `holderSignature: OK` but stops there, proving the signature itself was
fine and only the nonce was wrong).

---

## 6. Known Issues / Notes (found during this test pass)

Bugs found in this pass that were fixed on the branch (mentioned here so you
know what "correct" behavior looks like if you're diffing against an older
checkout):

1. `package.json` `start:prod` pointed at `dist/src/main`; Nest's build
   (`sourceRoot: src`) actually outputs to `dist/main.js`. Container crash-looped on boot.
2. Missing `@fastify/static` dependency — Swagger's Fastify integration
   throws and crashes the process without it.
3. `main.ts` manually registered `@fastify/formbody`, which collides with
   `FastifyAdapter`'s own default urlencoded content-type parser
   (`FST_ERR_CTP_ALREADY_PRESENT`). Removed the manual registration.
4. `credential_offer.credential_configuration_ids` was populated with the
   internal offer UUID instead of the actual key published under
   `credential_configurations_supported` — a real wallet correlating the
   offer against issuer metadata would never find a match. Fixed to use the
   canonical `<name>` / `<name>_<format>` id.

Still-open items worth knowing about (not fixed, out of scope for this pass):

- **`ldp_vc` + custom claims:** issuing `ldp_vc` fails
  (`jsonld.ValidationError: Safe mode validation error`) whenever
  `credentialSubject` has a property not defined in the VC's JSON-LD context
  (e.g. a bare `name` field with only the base VC context). This reproduces
  identically calling `credentials-service /credentials/issue` directly,
  bypassing `oid4vc-service` entirely — it is a pre-existing limitation of
  the JSON-LD signing pipeline, not something the OID4VC work introduced.
  Schemas intended for `ldp_vc` issuance need a schema-specific `@context`
  that defines their custom properties.
- **PoP proof without holder identity:** if a wallet submits an inline-`jwk`
  proof with no `iss` claim (a legitimate "bare key, no DID" holder-binding
  pattern per some wallets), `credentialSubject` ends up with no `id`, and
  `credentials-service` then fails to persist the record
  (`Argument subjectId for data.subjectId is missing`) with an opaque 500.
  Consider validating early in `oid4vc-service` (clear 400) rather than
  letting it surface as a downstream Prisma error.
- **`/credentials/verify` DTO field name:** passing `{"credential": ...}`
  instead of `{"verifiableCredential": ...}` silently returns
  `{"errors":[{}]}` with no indication the field name is wrong.

---

## 7. Cleanup

```bash
docker compose --profile oid4vc down
docker compose down
```

Vault unseal keys/root token are written to `keys.txt` at the repo root by
`enable-v2.sh` — it's gitignored; delete it once you're done if you'd rather
not keep dev secrets on disk.
