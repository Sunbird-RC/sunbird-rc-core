# OID4VC Test Wallet

An **independent holder** (wallet) for the Sunbird RC `oid4vc-service`, for local
Dev/QA testing of the full OpenID4VCI (issuance) and OpenID4VP (DCQL
presentation) flows across **all four credential formats**:

| Format | Issuance | Presentation |
|---|---|---|
| `ldp_vc` | ✅ | ✅ JWT-VP |
| `jwt_vc_json` | ✅ | ✅ JWT-VP |
| `vc+sd-jwt` | ✅ | ✅ JWT-VP (disclosures) |
| `mso_mdoc` | ✅ | ✅ CBOR `DeviceResponse` |

The wallet does **all holder crypto itself** — it generates its own ES256
(P-256) key, represents itself as a `did:jwk`, and mints its own PoP JWTs, VP
JWTs and mdoc `DeviceResponse`. It never asks the issuer's services to sign on
its behalf, so passing tests are a genuine interop signal (unlike the Postman
collection, where `identity-service` signs the holder proofs).

The core (`src/*.mjs`) is shared verbatim between a **headless Node runner** and
a **browser UI** — the browser runs the exact same code path that the headless
test proves against the live stack.

## Layout

```
src/wallet-core.mjs   holder key (ES256/did:jwk), PoP JWT, VP JWT
src/mdoc.mjs          @auth0/mdl DeviceResponse builder (OID4VP SessionTranscript)
src/vci-client.mjs    OID4VCI: accept offer -> token -> PoP -> credential
src/vp-client.mjs     OID4VP: fetch request -> build vp_token -> submit
src/store.mjs         in-memory credential store + DCQL-based selection
src/util.mjs          fetch/crypto helpers (isomorphic Node + browser)
test/e2e.mjs          headless runner: all 4 formats, issuance + VP
web/                  browser UI (app.js reuses src/*.mjs)
```

## Prerequisites

The full stack must be running (`db vault redis identity credential-schema
credential` + `oid4vc-service`), with the wallet's three test schemas seeded.
The wallet resolves its scenarios **by schema name** from the issuer's live
metadata (not hardcoded schema UUIDs), so it is portable across deployments —
the same build works against a local stack or the VM.

### Seed the test schemas (one-time, idempotent)

Creates the three well-known schemas the scenarios resolve against
(`Wallet Test W3C Credential` [ldp_vc + jwt_vc_json], `Wallet Test SD-JWT
Credential` [vc+sd-jwt], `Wallet Test mDL` [mso_mdoc, needs an EC P-256
issuer DID]):

```bash
# local (identity :3332, credential-schema :3333, oid4vc :3400)
node test/seed.mjs

# behind a gateway (e.g. the VM — everything on one host via nginx paths)
SCHEMA_BASE=http://<host> IDENTITY_BASE=http://<host> OID4VC_BASE=http://<host> \
  node test/seed.mjs
```

Re-running is safe: schemas already present (matched by name+format) are skipped.

### `PUBLIC_URL` must be reachable by both the wallet and the containers

For **`ldp_vc` only**, `identity-service` dereferences the credential's JSON-LD
`@context` from the issuer's `PUBLIC_URL`. If `PUBLIC_URL=http://localhost:3400`,
the container's `localhost` is itself and the fetch fails (`ECONNREFUSED`) —
issuance 500s. Set `PUBLIC_URL` to a host that resolves identically from the
host machine **and** from inside the containers (a LAN IP works on Docker
Desktop):

```bash
# from repo root — recreate just this service
OID4VC_PUBLIC_URL=http://<LAN_IP>:3400 \
  docker compose --profile oid4vc up -d --no-deps --build oid4vc-service
```

`jwt_vc_json`, `vc+sd-jwt` and `mso_mdoc` do not need this (no JSON-LD
dereferencing) and work with the default `localhost` URL.

> The browser UI needs `oid4vc-service` to allow cross-origin calls. `app.enableCors()`
> was added to `services/oid4vc-service/src/main.ts` for this (safe: these are
> public protocol routes with no cookie/session auth).

## Run — headless (recommended for CI/regression)

```bash
npm install
OID4VC_BASE=http://<LAN_IP>:3400 npm run e2e
```

Expected: `4/4 formats passed issuance + VP`, each with all six OID4VP checks
`OK` (`holderSignature`, `nonce`, `credentialSignatures`, `holderBinding`,
`revocation`, `dcql`).

## Run — browser UI

```bash
npm run dev            # serves on http://localhost:5555 (base URL defaults to :3400)
```

Open it, then use the per-format **quick test** buttons (the page also plays
issuer + verifier for convenience) or paste a real
`openid-credential-offer://` / `openid4vp://` deep link. Each offer / request
is also rendered as a QR so you can point a phone wallet at it.

## Deployed (VM)

The wallet is built into a static image
(`docker.io/snt1/sunbird-rc-oid4vc-test-wallet`, `test-wallet/Dockerfile` —
Vite build served by nginx:alpine) and runs as the `test-wallet` service in
`docker-compose.cloud.yml`, fronted by the gateway at **`/wallet/`**:

- **URL:** `http://<host>/wallet/` (e.g. the deployed VM).
- The base-URL field defaults to `location.origin` when served behind the
  gateway, so all `/oid4vc`, `/vp`, `/.well-known` calls route same-origin
  through nginx — no CORS, and `oid4vc-service`'s port is never exposed.
- Assets use a relative base (vite `base: './'`), so the same build works at
  the web root or under `/wallet/`.
- Remember to seed the schemas on that host first (see Prerequisites).

## What it is not

- Not a production wallet — no secure key storage, no consumer UX.
- mdoc trust is intentionally not chained to a real IACA root (the service runs
  `disableCertificateChainValidation: true`); fine for Dev/QA, not production.
