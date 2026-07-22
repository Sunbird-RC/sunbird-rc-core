# oid4vc-service — API Flow Design (VC Issuance + VP Verification)

Visual sequence diagrams proving out the two protocol flows implemented by
`oid4vc-service`, drawn against the **actual endpoints and message shapes**
verified in `TESTING.md` (every request/response in that file was captured
from a live run of this stack).

## Diagrams

Legend used in both diagrams: **solid arrow = request**, **dashed arrow =
response**, colored note boxes = the security-critical checks performed at
that step. The point-wise breakdown below each image mirrors it exactly.

### OID4VCI — Credential (VC) Issuance

![OID4VCI Credential Issuance Flow](docs/images/vci-flow.png)

### OID4VP — Presentation (VP) Verification

![OID4VP Presentation Verification Flow](docs/images/vp-flow.png)

---

## 1. OID4VCI — Credential Issuance Flow

**Actors (left → right):** Registry / Issuer · oid4vc-service · credential-schema · identity-service · credentials-service · Wallet

`POST /oid4vc/offer` is **not** called by the Wallet. The controller docs it
literally as *"internal, called by issuer/registry"*
(`src/oid4vci/oid4vci.controller.ts:22`), and in this repo the Java registry
already calls it from two hook points — see §4 below. The Wallet only ever
calls `GET /oid4vc/offer/:id` (dereference), `POST /oid4vc/token`, and
`POST /oid4vc/credential`.

### 1.1 Discovery
1. **Wallet → oid4vc-service**: `GET /.well-known/openid-credential-issuer`
2. **oid4vc-service → credential-schema**: `GET /credential-schema/oid4vci-configs` (live lookup, not cached across requests)
3. **credential-schema → oid4vc-service**: opted-in schemas with `formats`, `display`, `vct`
4. **oid4vc-service → Wallet**: `credential_configurations_supported` — one entry per `<schema>_<format>` combination (e.g. `OID4VC Pilot Credential_jwt_vc_json`)

### 1.2 Offer creation (issuer-side, pre-authorized_code grant)
5. **Registry/Issuer → oid4vc-service**: `POST /oid4vc/offer` `{credential_configuration_id, claims}` — fire-and-forget, fail-open (never blocks the entity-create or claim-grant flow it's called from)
6. **oid4vc-service → Registry/Issuer**: `{offer_id, credential_offer_uri, credential_offer, qr_data}` — the registry surfaces this alongside its normal API response / notification, however it chooses (QR, deep link, etc.)

### 1.3 Wallet dereferences the offer
7. **Wallet → oid4vc-service**: `GET /oid4vc/offer/:id` (after scanning the QR / following the link)
8. **oid4vc-service → Wallet**: `credential_offer` — `{credential_configuration_ids, grants: {"urn:ietf:params:oauth:grant-type:pre-authorized_code": {"pre-authorized_code": ...}}}`

### 1.4 Token exchange
9. **Wallet → oid4vc-service**: `POST /oid4vc/token` with `grant_type=urn:ietf:params:oauth:grant-type:pre-authorized_code&pre-authorized_code=...`
10. **oid4vc-service → identity-service**: `POST /utils/sign-jwt` — mints a 5-minute ES256 access token bound to the offer session
11. **identity-service → oid4vc-service**: signed JWT (`kid` = façade issuer DID)
12. **oid4vc-service → Wallet**: `{access_token, c_nonce, expires_in: 300, c_nonce_expires_in: 300}`

> **Security note (verified in TESTING.md N1):** the `pre-authorized_code` is
> single-use — an atomic store `GETDEL` consumes it. A replayed code returns
> `400 invalid_grant: bad or used code`.

### 1.5 Credential request (proof-of-possession)
13. **Wallet → oid4vc-service**: `POST /oid4vc/credential`, `Authorization: Bearer <access_token>`, body `{proof: {proof_type: "jwt", jwt: <PoP JWT>}}`
14. **oid4vc-service → identity-service**: resolve holder DID (if proof uses `kid`) and verify the PoP JWT signature

    **PoP checks performed:** `aud` == issuer public URL, `nonce` == live `c_nonce` (single-use, GETDEL'd on use). Either check failing returns a distinct `400` (`invalid_proof: PoP audience mismatch` / `invalid_or_missing_proof` with a fresh `c_nonce` for retry).

15. **oid4vc-service → credentials-service**: `POST /credentials/issue` `{credential, credentialSchemaId, format, holderJwk}` — holder DID injected as `credentialSubject.id` (or JWT `sub` for enveloped formats)
16. **credentials-service → identity-service**: `POST /utils/sign` (Ed25519, `ldp_vc`) **or** `POST /utils/sign-jwt` (ES256, `jwt_vc_json`)
17. **identity-service → credentials-service**: signed proof / compact JWS
18. **credentials-service → oid4vc-service**: signed VC (format-aware envelope)
19. **oid4vc-service → Wallet**: `{credential, c_nonce, format}`

> **Proof of holder binding:** decoding the returned `jwt_vc_json` credential
> shows `vc.credentialSubject.id` and top-level `sub` both equal to the
> holder DID that signed the PoP proof — this is the cryptographic evidence
> that the VC was bound to the wallet that requested it, not just anyone.

---

## 2. OID4VP — Presentation Verification Flow

**Actors (left → right):** Verifier · oid4vc-service · identity-service · credentials-service · Wallet

### 2.1 Verifier creates a request
1. **Verifier → oid4vc-service**: `POST /vp/request` `{dcql_query}`
2. **oid4vc-service → identity-service**: `POST /utils/sign-jwt` — signs the JAR (JWT-secured Authorization Request) with `nonce`, `state`, `dcql_query`, `response_uri`
3. **identity-service → oid4vc-service**: signed JAR
4. **oid4vc-service → Verifier**: `{transaction_id, request_uri, qr_data}` (`openid4vp://...`)

### 2.2 Wallet fetches and answers the request
5. **Wallet → oid4vc-service**: `GET /vp/request-object/:id`
6. **oid4vc-service → Wallet**: signed JAR (`Content-Type: application/oauth-authz-req+jwt`) — wallet decodes `nonce`, `state`, `dcql_query`, `response_uri`
7. **Wallet → oid4vc-service**: `POST /vp/response` (`direct_post`) `{state, vp_token}` — `vp_token` is a JWT signed by the holder's key, embedding `vp.verifiableCredential: [<VC>]` and the request's `nonce`

### 2.3 Verification chain (all inside `oid4vc-service`, delegating crypto)
8. **oid4vc-service → identity-service**: resolve holder DID from the VP token's `kid`/`iss`, verify the holder's signature over the VP token
   - check: `holderSignature == OK`
   - check: `nonce == txn.nonce` (replay protection — verified in TESTING.md N17)
9. **oid4vc-service → credentials-service**: `POST /credentials/verify` **per embedded VC**, with `options: {challenge: nonce, domain: publicUrl}` (P0.5 challenge/domain binding)
10. **credentials-service → oid4vc-service**: `{checks: [{proof, expired, revoked}]}`
    - check: `credentialSignatures == OK` (VC proof itself is valid, not tampered — verified in TESTING.md N12)
    - check: `revocation == OK` (status-list consulted if the VC carries `credentialStatus`)
    - check: `holderBinding == OK` — `credentialSubject.id` (or `sub`) of the embedded VC must equal the DID that signed the VP (verified in TESTING.md N19 — an impostor holding someone else's VC fails here)
    - check: `dcql == OK` — the DCQL query's requested claims/formats are all present in the disclosed credential(s) (verified in TESTING.md N18 — a query for a missing claim fails with a named reason)
11. **oid4vc-service → Wallet**: `{status: "ok"}` (or `403` with a `checks` map showing exactly which stage failed)

### 2.4 Verifier polls the result
12. **Verifier → oid4vc-service**: `GET /vp/status/:id`
13. **oid4vc-service → Verifier**: `{verified: true, checks: {holderSignature, nonce, credentialSignatures, holderBinding, revocation, dcql}, claims, holderDid}`

> **Proof of presentation:** all six checks returning `OK` together is the
> cryptographic proof that (a) the presenter controls the holder key, (b)
> the presented VC(s) are genuine, unrevoked, and bound to that same holder,
> and (c) the disclosed claims satisfy exactly what the verifier asked for
> via DCQL — nothing less, nothing forged.

---

## 3. Cross-reference to verified test evidence

Every numbered check above has a corresponding **executed** negative test in
`services/oid4vc-service/TESTING.md` §5 proving it actually rejects bad
input, not just that the happy path returns 200:

| Check | Proven by |
|---|---|
| pre-auth code single-use | N1 |
| PoP `nonce` binding | N5 |
| PoP `aud` binding | N6 |
| credential-config existence | N7 |
| format opt-in enforcement | N8 |
| VC signature tamper detection | N12 |
| VP `nonce` replay protection | N17 |
| DCQL satisfaction | N18 |
| VP holder-binding (impostor rejection) | N19 |
| VP transaction replay | N20 |

See `TESTING.md` for the exact commands and captured output for each.

---

## 4. Who calls `POST /oid4vc/offer`? (Registry integration)

**Short answer: it's designed to be called by an issuer-side backend — the
Java registry is the intended and already-implemented caller. It is not
called by the Wallet, and today nothing stops "any other external service"
from calling it either — see §4.4, that's a gap, not a feature.**

### 4.1 It's already wired into the Java registry (Plan.md P1.13 is done)

Two hook points, both fail-open (log + swallow the error, never fail the
underlying registry operation):

| Hook | File | Trigger |
|---|---|---|
| Entity create/update | `java/registry/src/main/java/dev/sunbirdrc/registry/service/impl/RegistryServiceImpl.java:294-312` (`generateCredentials()`) | Fires on **both** `addEntity()` (line 234) and `updateEntity()` (line 376) — any time a credential gets (re)signed |
| Claim grant / attestation | `java/registry/src/main/java/dev/sunbirdrc/registry/helper/RegistryHelper.java:633-637` (`updateState()`, `GRANT_CLAIM` branch) | Fires when an attestation is granted and has a `credentialTemplate` |

Both delegate to `java/registry/src/main/java/dev/sunbirdrc/registry/service/OID4VCIService.java`:
- `isEnabled()` → `oid4vc.enabled=true` **and** `oid4vc.offerUrl` non-empty
- `createOfferSafely(credentialConfigurationId, claims)` → builds `{credential_configuration_id, claims}`, POSTs to `offerUrl` via the existing `RetryRestTemplate` bean (same retrying HTTP client already used elsewhere in the registry — no new plumbing needed), and swallows any exception with a `logger.warn`.

`credential_configuration_id` passed is:
- `vertexLabel` (the entity type, e.g. `"Teacher"`) for the entity create/update hook
- `"<sourceEntity>_<policyName>"` (e.g. `"Teacher_TeacherAttestation"`) for the claim-grant hook

### 4.2 Why it's disabled by default in this compose setup

`java/registry/src/main/resources/application.yml:246-248`:
```yaml
oid4vc:
  enabled: ${oid4vc_enabled:false}
  offerUrl: ${oid4vc_offer_url:http://localhost:3400/oid4vc/offer}
```
`docker-compose.yml`'s `registry` service **never sets** `oid4vc_enabled` or
`oid4vc_offer_url` as environment variables, so the hook stays permanently
off even when you deploy with `--profile oid4vc`. This is the main reason
"nothing happens" out of the box — it's not missing code, it's missing
config.

### 4.3 Changes required to actually wire it up

1. **Add two env vars to the `registry` service block in `docker-compose.yml`:**
   ```yaml
   - oid4vc_enabled=true
   - oid4vc_offer_url=http://oid4vc-service:3400/oid4vc/offer
   ```
   (container-network hostname, not `localhost` — the default only works if
   registry and oid4vc-service somehow shared a network namespace, which they don't)

2. **Deploy with the `oid4vc` profile** so `oid4vc-service` is actually
   running: `docker compose --profile oid4vc up -d`. Optionally add
   `depends_on: oid4vc-service: condition: service_started` to the `registry`
   block for clarity — not strictly required since the hook is async/fail-open
   and will just log a warning if oid4vc-service isn't up yet on the first call.

3. **Align naming — the part most likely to trip people up.** `oid4vc-service`
   resolves `credential_configuration_id` by matching it against
   `credential-schema`'s `name` field (`services/credential-schema/src/schema/schema.service.ts:88`,
   `getOid4vciConfigs()` returns `name: s.name`). It has **no knowledge of the
   registry's entity/schema definitions** — they're two separate schema systems
   (Java registry `_schemas/*.json` vs. the `credential-schema` microservice's
   Postgres-backed schema records). So whoever creates the OID4VCI-enabled
   `credential-schema` entry must set its `name` to **exactly**:
   - the registry entity type name (e.g. `"Teacher"`) for entity-create/update-triggered offers, or
   - `"<sourceEntity>_<policyName>"` (e.g. `"Teacher_TeacherAttestation"`) for claim-grant-triggered offers.

   Get this wrong and offer creation silently 404s
   (`Credential configuration '<X>' not enabled for OID4VCI`) — logged as a
   warning in the registry, never surfaced to the entity-creation caller, and
   no wallet offer is ever produced. Worth a startup-time or CI check if this
   is adopted for real.

4. **Optional: pass `format` explicitly.** The registry hook doesn't set
   `format` today, so `oid4vc-service` defaults to `cfg.formats[0]` (whichever
   format is listed first in the schema's `oid4vciFormats` array). If you need
   a specific format issued automatically when a schema supports more than
   one, `OID4VCIService.createOfferSafely` needs a small change (add a
   `format` parameter, `body.put("format", format)`).

### 4.4 Security gap: today, "any external service" really can call it

`nginx/nginx.conf:39-41` proxies the **entire** `/oid4vc/` prefix publicly
with no restriction:
```nginx
location /oid4vc/ {
      proxy_pass http://oid4vc-service:3400/oid4vc/;
}
```
That includes `/oid4vc/offer`. Inside `oid4vc-service` itself, `ENABLE_AUTH`
is read into config (`src/config/configuration.ts:41`, `enableAuth`) but is
**never referenced anywhere else in the codebase** — no `AuthGuard`, no
`@UseGuards`, nothing. It's a dead flag. `Oid4vciController.createOffer()`
(`src/oid4vci/oid4vci.controller.ts:22-25`) has no auth check at all.

Net effect: **as deployed today, `POST /oid4vc/offer` is a completely
unauthenticated endpoint reachable from the public internet** through the
nginx gateway, despite being documented in code as "internal, called by
issuer/registry." Anyone who can reach the nginx host can:
- create arbitrary offer sessions for any OID4VCI-enabled schema with
  attacker-chosen `claims` (they can't get a real signed credential without
  passing PoP with a holder key, but they can consume session-store
  capacity, and error messages leak which schemas/formats are enabled)
- do this at whatever rate they like — no rate limiting is in front of it

This isn't a new risk invented by this analysis — it's exactly what Plan.md's
own P4.4 gate ("rate limiting on public endpoints") is meant to catch before
any non-dev deployment. Two independent, non-exclusive fixes:
- **nginx-level**: don't put `/oid4vc/offer` behind the same public location
  as the wallet-facing routes — split it into its own `location` block with
  an `allow`/`deny` list (internal network only) or require mTLS/a shared
  secret from the registry.
- **App-level**: actually implement the `ENABLE_AUTH` flag — e.g. a simple
  shared-secret header check on `createOffer` only (the wallet-facing routes
  must stay open, since real wallets have no registry credential).

### 4.5 Bottom line

| Question | Answer |
|---|---|
| Does the Wallet call `POST /oid4vc/offer`? | No — never. It only calls `GET /oid4vc/offer/:id`, `POST /oid4vc/token`, `POST /oid4vc/credential`. |
| Can the existing Java registry call it? | Yes — already implemented (P1.13), fail-open, at two hook points. |
| Is it enabled today? | No — `oid4vc.enabled` defaults `false`, and `docker-compose.yml` never sets the env vars that would turn it on. |
| What has to change to enable it? | 2 env vars on the `registry` service + naming alignment between `credential-schema.name` and the registry's `vertexLabel`/`title` (§4.3). No code changes required for the happy path. |
| Can "any other external service" call it too? | Yes, currently — and that's an unintended security gap, not a design choice (§4.4). Recommend restricting it before enabling `oid4vc.enabled=true` in any shared environment. |
