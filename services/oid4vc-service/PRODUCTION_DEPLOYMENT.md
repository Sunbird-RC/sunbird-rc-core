# oid4vc-service — Production Deployment & Inji Wallet Testing Guide

Companion to `README.md` (architecture), `TESTING.md` (verified manual test
cases), and `API_FLOW_DESIGN.md` (sequence diagrams). This doc covers:

1. Every flag/config across the stack needed to enable OID4VC end-to-end
2. A production hardening checklist — what must change before go-live
3. Step-by-step deployment
4. How to test against MOSIP's Inji Wallet, including two known integration
   gaps you should fix or work around first

---

## 1. Complete flag/config reference

OID4VC touches five components. Every flag that affects OID4VC behavior is
listed below, with its default and what happens if you leave it unset.

### 1.1 `oid4vc-service` (the façade itself)

| Env var | Default | Purpose |
|---|---|---|
| `PORT` | `3400` | listen port |
| `PUBLIC_URL` | `http://localhost:3400` | **must be the externally-reachable HTTPS URL in production** — it's embedded as `iss`/`aud` in minted tokens, JARs, and PoP checks. Wrong value = every token/proof validation fails. |
| `CREDENTIAL_SERVICE_BASE_URL` | `http://localhost:3000` | credentials-service |
| `IDENTITY_BASE_URL` | `http://localhost:3332` | identity-service |
| `SCHEMA_BASE_URL` | `http://localhost:3333` | credential-schema |
| `SESSION_STORE` | `memory` | **must be `redis` for any multi-replica deployment** — `memory` loses all offers/nonces/tokens on restart and isn't shared across replicas (session-store.interface.ts's own in-memory impl explicitly warns against this) |
| `REDIS_URL` | `redis://localhost:6379` | required when `SESSION_STORE=redis` |
| `ISSUER_DID` | *(blank → auto-generated on boot)* | **must be pinned in production.** Left blank, the façade mints a brand-new signing DID every restart — every previously-issued access token / JAR becomes unverifiable, and JWKS/DID resolution breaks for anything issued before the restart. Generate once via identity-service's `/did/generate`, then set this permanently. |
| `OID4VP_ENABLED` | `true` | mounts `/vp/*` routes; set `false` to run issuance-only |
| `DRAFT13_COMPAT_MODE` | `false` | emit OID4VCI draft-13 shapes (Inji interop) — see §4 |
| `ENABLE_AUTH` | `false` | **currently a dead flag — see §2.1.** Loaded into config but not enforced anywhere in the code. Don't rely on it. |
| `JWKS_URI` | *(blank, unused)* | present in config but not read anywhere else in the code as of this writing — safe to leave unset |
| `OFFER_TTL` | `600`s | how long an offer session lives before expiring |
| `NONCE_TTL` | `300`s | `c_nonce` lifetime (single-use regardless) |
| `ACCESS_TOKEN_TTL` | `300`s | façade-minted access token lifetime |
| `DEFERRED_TTL` | `86400`s | deferred-issuance transaction lifetime |
| `VP_TXN_TTL` | `300`s | OID4VP request/response transaction lifetime |

### 1.2 `identity-service` (all signing keys)

| Env var | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | — | dedicated DB (do not share with the Java registry's schema — Prisma conflict) |
| `VAULT_ADDR` / `VAULT_API_ADDR` / `VAULT_ADDRESS` | — | Vault reachability |
| `VAULT_TOKEN` | — | **rotate off the initial root token before production** — use a scoped policy token |
| `VAULT_BASE_URL` | — | e.g. `http://vault:8200/v1/` |
| `VAULT_ROOT_PATH` | — | KV mount path, e.g. `kv` |
| `VAULT_TIMEOUT` | `5000`ms | request timeout |
| `VAULT_PROXY` | — | string-boolean, whether to route through a proxy |
| `SIGNING_ALGORITHM` | — | e.g. `Ed25519Signature2020` — governs the **existing JSON-LD path only**; ES256 keys for `jwt_vc_json`/`vc+sd-jwt` are generated automatically per-DID on first use (`jwt.service.ts:ensureES256Key`), no separate flag |
| `JWKS_URI` | — | present for symmetry, not required for local signing |
| `ENABLE_AUTH` | `false` | this one **is** enforced here (unlike oid4vc-service) — see `src/auth/auth.guard.ts` |
| `WEB_DID_BASE_URL` | — | only relevant if you issue `did:web` identifiers |

### 1.3 `credential-schema`

| Env var | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | — | dedicated DB |
| `IDENTITY_BASE_URL` | — | for DID-related calls |
| `JWKS_URI` / `ENABLE_AUTH` | — | same pattern as identity-service |
| `SCHEMA_BASE_URL` | — | self-reference, used in generated URLs |

**Per-schema OID4VCI opt-in** (not an env var — set via the schema payload,
see `TESTING.md` §4 P4 for a worked example):
```json
"oid4vciConfig": {
  "oid4vciEnabled": true,
  "oid4vciFormats": ["ldp_vc", "jwt_vc_json"],
  "vct": "...",
  "display": [{ "name": "...", "locale": "en-US" }]
}
```
`GET /credential-schema/oid4vci-configs` lists everything currently opted in.

### 1.4 `credentials-service`

| Env var | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | — | dedicated DB |
| `IDENTITY_BASE_URL` / `SCHEMA_BASE_URL` | — | dependency URLs |
| `JWKS_URI` / `ENABLE_AUTH` | — | same pattern |
| `QR_TYPE` | — | `W3C_VC` enables the QR rendering utility for the *existing* certificate/QR feature — unrelated to OID4VCI's own `qr_data` field |
| `STATUS_LIST_ENABLED` | `false` | turns on `credentialStatus`/revocation-list checks — **recommended `true` in production** if you want OID4VP's `revocation` check (§ API_FLOW_DESIGN.md §2.3) to mean anything; otherwise every VC is treated as non-revocable |

### 1.5 Java registry (optional — only if wiring automatic offers)

`java/registry/src/main/resources/application.yml:246-248`:

| Env var | Default | Purpose |
|---|---|---|
| `oid4vc_enabled` | `false` | turns on the `generateCredentials()` / `GRANT_CLAIM` hooks that call `POST /oid4vc/offer` automatically |
| `oid4vc_offer_url` | `http://localhost:3400/oid4vc/offer` | **must be the container/network-reachable URL** — e.g. `http://oid4vc-service:3400/oid4vc/offer` in docker-compose. The default `localhost` value is a footgun: it silently resolves inside the registry container, not to oid4vc-service, and every offer creation attempt fails (fail-open, so entity creation still succeeds — but no wallet offer is ever produced, only a `logger.warn`). |

**Also required, independent of these two flags** (pre-existing V2 signing prerequisites — not OID4VC-specific, but OID4VC depends on them entirely):

| Env var | Purpose |
|---|---|
| `signature_enabled=true` | required — `generateCredentials()`'s oid4vc hook only fires `if (signatureEnabled && credentialTemplate != null)` |
| `signature_provider=dev.sunbirdrc.registry.service.impl.SignatureV2ServiceImpl` | routes signing through the V2 (Node) services |
| `did_enabled=true`, `claims_enabled=true` (only if using attestation/`GRANT_CLAIM` issuance) | |

**Critical, not optional — naming alignment:** the registry passes `vertexLabel`
(entity type) or `"<sourceEntity>_<policyName>"` (claim grant) as
`credential_configuration_id`. This **must exactly equal** the `name` field
of the corresponding `credential-schema` entry, or offer creation silently
404s. See `API_FLOW_DESIGN.md` §4.3 for the full explanation.

### 1.6 nginx (public gateway)

`nginx/nginx.conf:38-58` — additive locations, active whenever `oid4vc-service`
is deployed:

| Location | Proxies to |
|---|---|
| `/oid4vc/` | `oid4vc-service:3400/oid4vc/` (includes offer, token, nonce, credential, deferred, notification — **all of it, no split between public/internal routes**) |
| `/vp/` | `oid4vc-service:3400/vp/` |
| `/.well-known/openid-credential-issuer` | issuer metadata |
| `/.well-known/openid-configuration` | façade AS metadata |
| `/oid4vc-jwks.json` | façade JWKS |

No TLS, no rate limiting, no path-level access control exist in this file
today (plain `listen 80` only, confirmed by reading the file). See §2.2.

---

## 2. Production hardening checklist (do these before go-live)

This is the concrete, must-do list — not aspirational. Each item was found
by reading the code, not assumed.

### 2.1 Fix or restrict the unauthenticated internal endpoints

**Finding (see `API_FLOW_DESIGN.md` §4.4):** `POST /oid4vc/offer` is
documented in code as *"internal, called by issuer/registry"*, but nginx
proxies the entire `/oid4vc/` prefix publicly, and `ENABLE_AUTH` in
`oid4vc-service` is loaded into config but never enforced (no guard anywhere
in the codebase — verified by grep, not assumption). As shipped, anyone who
can reach your public host can call it.

Pick one before enabling `oid4vc.enabled=true` on the registry in any
shared/production environment:
- **nginx-level (fastest):** split `/oid4vc/offer` into its own `location`
  block with an `allow <internal-cidr>; deny all;` rule, or place it behind
  mTLS / a network policy that only the registry can reach.
- **App-level (more correct long-term):** implement the `ENABLE_AUTH` flag
  for real — e.g. a shared-secret header check, enforced only on
  `POST /oid4vc/offer` (and arguably `POST /vp/request` / `GET /vp/status/:id`,
  which are verifier-authenticated per the design but currently have no
  actual guard either). Wallet-facing routes (`offer/:id` dereference,
  `token`, `credential`, `nonce`, `vp/response`, `vp/request-object/:id`) must
  stay open — real wallets have no registry credential.

### 2.2 TLS + rate limiting on the public gateway

`nginx/nginx.conf` has no `ssl` block and no `limit_req` directives —
confirmed by inspection. Before production:
- Terminate TLS in front of nginx (load balancer / ingress / nginx `ssl_certificate` directives) — OID4VCI/OID4VP both assume HTTPS in their metadata URLs and PoP/JAR audience checks.
- Add rate limiting on `/oid4vc/token`, `/oid4vc/credential`, `/oid4vc/nonce`, `/vp/response` at minimum — these are the endpoints an attacker would hammer for replay/enumeration attempts (this is exactly Plan.md's P4.4 gate).

### 2.3 Pin `ISSUER_DID`

Don't run production on an auto-generated ephemeral DID. Generate it once:
```bash
curl -s -X POST http://identity:3332/did/generate -H "Content-Type: application/json" \
  -d '{"content":[{"alsoKnownAs":["oid4vc-issuer-prod"],"services":[],"method":"rcw"}]}'
```
Take the returned `id` and set `ISSUER_DID` on `oid4vc-service` permanently.
Restarting without a pinned DID invalidates every previously-minted access
token and JAR the moment the container restarts.

### 2.4 `SESSION_STORE=redis`, not `memory`

Required for anything beyond a single replica, and recommended even for a
single replica in production (survives container restarts). Point
`REDIS_URL` at a persistent, access-controlled Redis instance — not the
unauthenticated `redis:latest` container this repo uses for local dev.

### 2.5 Vault production posture

The `vault.json` shipped in this repo is dev-shaped: file storage backend,
TLS disabled, manual unseal (see `enable-v2.sh` / `setup_vault.sh`, which
explicitly print unseal keys to `keys.txt` on disk). In production:
- Use Vault's Raft/Consul storage backend and auto-unseal (cloud KMS), not file storage + manual unseal keys sitting on disk.
- Enable TLS on Vault's listener.
- Scope the token identity-service uses down to only the paths it needs (`kv/` per-DID secrets), not the initial root token.

### 2.6 Enable `STATUS_LIST_ENABLED=true` on `credentials-service`

Without it, OID4VP's `revocation` check always passes trivially — there's no
actual revocation being consulted. Turn it on if revocation matters to you
(it should, for anything beyond a demo).

### 2.7 Rotating/verifying keys

Per Plan.md's P4.4 gate: façade signing key lives in Vault (already true —
delegated through identity-service), access tokens are short-lived (5 min
default — already true), but there's no automated rotation policy for the
underlying ES256/Ed25519 DID keys themselves. Decide and document a rotation
cadence before go-live; this repo doesn't implement one.

---

## 3. Deployment procedure (docker-compose)

This mirrors what was actually run and verified in `TESTING.md` §3, with the
production deltas from §2 applied.

```bash
# 1. .env — apply every flag from §1 above, with production values:
#    PUBLIC_URL=https://your-domain/oid4vc  (or however you route it)
#    SESSION_STORE=redis, REDIS_URL=<real redis>
#    ISSUER_DID=<pinned, from §2.3>
#    STATUS_LIST_ENABLED=true
#    oid4vc_enabled=true, oid4vc_offer_url=http://oid4vc-service:3400/oid4vc/offer  (registry only, if wiring auto-offers)

# 2. Bring up prerequisites
docker compose up -d db vault redis

# 3. Init/unseal vault (first run only), start identity/credential-schema/credential
bash enable-v2.sh

# 4. Build/deploy oid4vc-service
docker compose --profile oid4vc up -d --build oid4vc-service

# 5. Confirm health
curl -sf https://<your-domain>/oid4vc/../health   # or internally: curl -sf http://oid4vc-service:3400/health
curl -s https://<your-domain>/.well-known/openid-credential-issuer
```

Then apply §2.1/§2.2 (auth restriction + TLS/rate limiting) at the nginx
layer before exposing publicly, and opt in the schemas you intend to issue
via `oid4vciConfig` (per §1.3).

---

## 4. Testing with Inji Wallet

### 4.1 What Inji Wallet actually expects — two integration paths

Research into MOSIP's current architecture (sources at the end of this
section) surfaced two distinct integration models, and they are **not
interchangeable**:

**Path A — direct Credential Offer (Issuer-Initiated), pre-authorized_code.**
This is what `oid4vc-service` implements and what `DRAFT13_COMPAT_MODE` was
built for. MOSIP's `inji-vci-client` libraries (used to build Inji-compatible
wallet clients) support this "Issuer Initiated (Credential Offer)" flow
directly — scan a QR encoding `openid-credential-offer://...`, no
pre-registration needed.

**Path B — the published Inji Wallet app, via its Mimoto backend.** The
full consumer Inji Wallet mobile app doesn't do free-form issuer discovery —
it fetches a fixed list of configured issuers from a backend component
called **Mimoto**, driven by `mimoto-issuers-config.json`. Getting a new
issuer to show up on the wallet's "Add new card" screen means adding an
entry to that config and redeploying Mimoto. Crucially, the example issuer
schema this file uses is built around an **`authorization_code` grant via an
external OIDC/OAuth authorization server** (ESignet) — fields like
`authorization_audience`, `redirect_uri`, `proxy_token_endpoint`, and
`client_alias` all point at a separate auth server, not at the issuer's own
token endpoint directly. `oid4vc-service` only implements the
**pre-authorized_code** grant and is its own authorization server for that
grant (Plan.md's explicit design decision) — there is no
`authorization_code`/redirect support here at all.

**Practical implication:** Path A is the one this codebase is actually built
to support and is the one worth testing first. Path B (the full Play
Store/App Store Inji Wallet app talking to a standard Mimoto deployment) may
require either a Mimoto-side adapter for pre-authorized_code issuers (if one
exists in your Mimoto version — not confirmed by this research) or won't
integrate cleanly without extra work. Don't assume Path B works until you've
verified it against your specific Mimoto/Inji Wallet build.

### 4.2 A known bug to fix before testing Path A

`oid4vci.service.ts:buildOfferObject()` (line ~157) always emits
`credential_configuration_ids` in the credential offer object, **regardless**
of `DRAFT13_COMPAT_MODE`. OID4VCI draft-13's credential offer uses a
`credentials` field (an array of credential-type strings matching keys in
`credentials_supported`) — not `credential_configuration_ids`, which is the
final-1.0 field name. Today, `issuerMetadata()` correctly swaps
`credential_configurations_supported` ↔ `credentials_supported` based on the
flag, but the offer object's key name isn't swapped to match. A draft-13
client parsing the offer may not find the field it expects.

**Fix before a real device test:** in `buildOfferObject`, branch the returned
key the same way `issuerMetadata()` already does:
```ts
return {
  credential_issuer: this.config.publicUrl,
  ...(this.config.draft13CompatMode
    ? { credentials: [configId] }
    : { credential_configuration_ids: [configId] }),
  grants: { [PREAUTH_GRANT]: grant },
};
```

### 4.3 Step-by-step: testing Path A (direct Credential Offer)

1. Set `DRAFT13_COMPAT_MODE=true` on `oid4vc-service` and redeploy (apply the
   §4.2 fix first).
2. Opt a schema into OID4VCI with a format Inji can render (`ldp_vc` or
   `jwt_vc_json` — per `TESTING.md` §4 P4).
3. Confirm draft-13 shaped metadata:
   ```bash
   curl -s https://<host>/.well-known/openid-credential-issuer
   # expect: "credentials_supported" (not "credential_configurations_supported")
   ```
4. Create an offer (from the registry, or manually per `TESTING.md` §4 P6
   step 1) and get the `qr_data` (`openid-credential-offer://...`).
5. Render `qr_data` as an actual QR code (any QR-image library/site — the
   string itself is what matters, not how you draw it) and scan it with the
   Inji-compatible wallet/client under test.
6. Confirm in your logs: `GET /oid4vc/offer/:id` → `POST /oid4vc/token` →
   `POST /oid4vc/credential` all land, in that order, from the wallet's IP —
   this is the same sequence verified manually in `TESTING.md` §4 P6, now
   coming from a real device instead of curl.
7. Confirm the credential lands in the wallet's UI and, if the wallet
   supports it, that it validates against your `/.well-known/jwks.json`.

### 4.4 Step-by-step: testing Path B (published Inji Wallet + Mimoto)

Only attempt this after Path A works, and be prepared for it to need
adaptation:

1. Stand up (or get access to) a Mimoto instance you control.
2. Add an entry to `mimoto-issuers-config.json` (schema below) pointing
   `wellknown_endpoint` at `https://<host>/.well-known/openid-credential-issuer`.
3. Since `oid4vc-service` has no `authorization_code` support,
   `authorization_audience` / `proxy_token_endpoint` (which assume a separate
   OAuth AS) don't have a real target here — this is the crux of the
   mismatch from §4.1. You will likely need a Mimoto version/config path that
   supports pre-authorized_code issuers directly, or a small adapter. Verify
   this against your Mimoto version before investing further.
4. Rebuild/redeploy Mimoto with the updated config so the new issuer appears
   on the wallet's "Add new card" screen.
5. From there the flow inside the wallet is the same as §4.3 steps 6-7.

Example issuer entry shape (fields confirmed from MOSIP's own
`mimoto-issuers-config.json` — adapt `client_id`/`client_alias` to whatever
your Mimoto deployment's onboarding process assigns, and treat
`authorization_audience`/`proxy_token_endpoint` as the open question from
step 3 above):
```json
{
  "issuer_id": "sunbird-rc-oid4vc",
  "credential_issuer": "sunbird-rc-oid4vc",
  "protocol": "OpenId4VCI",
  "client_id": "<assigned during Mimoto partner onboarding>",
  "client_alias": "<keystore alias, per Mimoto onboarding>",
  "wellknown_endpoint": "https://<host>/.well-known/openid-credential-issuer",
  "credential_issuer_host": "https://<host>",
  "qr_code_type": "OnlineSharing",
  "enabled": "true",
  "display": [
    { "name": "Sunbird RC", "title": "Sunbird RC Credentials", "language": "en" }
  ]
}
```

### 4.5 Sources (Inji/Mimoto research — verify against your specific version before relying on this)

- [Mimoto | Inji docs — backend service overview](https://docs.mosip.io/inji/inji-wallet/backend-services/mimoto)
- [mosip/mimoto README (issuer config, keystore requirements)](https://github.com/inji/mimoto/blob/master/README.md)
- [Credential Providers | Inji docs](https://docs.inji.io/inji-wallet/inji-mobile/technical-overview/customization-overview/credential_providers)
- [inji-vci-client-ios-swift — Issuer-Initiated (Credential Offer) support](https://github.com/inji/inji-vci-client-ios-swift)
- [mosip/inji-config repository](https://github.com/mosip/inji-config)
- [MOSIP community: Mimoto issuer-validation startup failure thread](https://community.mosip.io/t/mimoto-v0-19-2-fails-during-startup-issuer-validation-when-issuer-config-is-loaded-from-local-filesystem/2573)
- [MOSIP Inji Certify developer-release announcement](https://www.biometricupdate.com/202511/mosip-advances-tool-for-verifiable-credential-issuance-with-developer-release)
