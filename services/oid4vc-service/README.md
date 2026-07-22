# oid4vc-service

OpenID4VCI 1.0 (issuance) + OpenID4VP 1.0 (presentation) **protocol façade** for Sunbird RC.

It speaks the wallet protocols on the outside and delegates everything else to the
**existing, unchanged** services on the inside:

- **credentials-service** (`/credentials/issue`, `/credentials/verify`) — builds & signs the VC, stores it
- **identity-service** (`/utils/sign-jwt`, `/did/resolve`, `/.well-known/jwks.json`) — all key operations (keys stay in Vault)
- **credential-schema** (`/credential-schema/oid4vci-configs`) — drives the issuer metadata

It holds **no credential keys and no credential storage** — only its own OAuth/protocol
signing key (delegated to identity-service) and short-lived session state.

## Credential formats

All three formats are supported and chosen per credential type via the schema's
`oid4vciConfig.oid4vciFormats`:

| Format | Signed by (identity-service) | Selective disclosure |
|---|---|---|
| `ldp_vc` | `/utils/sign` (Ed25519, existing) | ✖ |
| `jwt_vc_json` | `/utils/sign-jwt` (ES256) | ✖ |
| `vc+sd-jwt` | `/utils/sign-sd-jwt` (ES256) | ✔ |

## Endpoints

### OID4VCI (issuer)
- `GET /.well-known/openid-credential-issuer` — issuer metadata
- `GET /.well-known/openid-configuration` + `GET /.well-known/jwks.json` — AS metadata so the Java registry trusts façade-minted tokens config-only
- `POST /oid4vc/offer` (internal) · `GET /oid4vc/offer/:id` (wallet)
- `POST /oid4vc/token` (pre-authorized_code grant only)
- `POST /oid4vc/nonce`
- `POST /oid4vc/credential` — verifies holder PoP, injects holder DID, issues VC
- `POST /oid4vc/deferred` · `POST /oid4vc/notification`

### OID4VP (verifier)
- `POST /vp/request` (DCQL in → QR out) · `GET /vp/request-object/:id`
- `POST /vp/response` (direct_post) · `GET /vp/status/:id`

## Session store

Abstract interface (`src/session`) with two backends chosen by `SESSION_STORE`:
- `memory` — single-instance/test (default). Do **not** run multiple replicas with this.
- `redis` — production. Uses native TTL + atomic GETDEL for single-use codes/nonces.

## Config

See `.env.sample`. Key flags:
- `SESSION_STORE=memory|redis`
- `DRAFT13_COMPAT_MODE=true` — emit OID4VCI draft-13 shapes for MOSIP Inji Wallet
- `OID4VP_ENABLED=true` — mount the `/vp/*` routes
- `ISSUER_DID` — the DID used to sign access tokens / request objects (auto-generated in dev if blank)

## Run

```bash
npm install
npm run build
npm run start:prod     # or start:dev for watch mode
# Swagger UI at /api
```

In the stack it runs under the `oid4vc` compose profile:

```bash
docker compose --profile oid4vc up oid4vc-service
```

## Draft-13 vs final-1.0

The core logic emits final OID4VCI/OID4VP 1.0 shapes. `DRAFT13_COMPAT_MODE=true`
switches the metadata (`credentials_supported`), the offer grant (`user_pin_required`),
and keeps `c_nonce` in the token response — the idioms MOSIP Inji Wallet expects today.
This is isolated to `oid4vci.service.ts` so both shapes are covered without branching the flow.

## Security notes

- The façade's token-signing key should live in Vault (via identity-service) in
  production; access tokens are short-lived (5 min).
- Public endpoints should sit behind the nginx gateway with TLS + rate limiting.
- Holder proof-of-possession (`pop.service.ts`) is the one new security-critical
  check; nonces are single-use (atomic GETDEL) to prevent replay.
