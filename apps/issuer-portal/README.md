# Issuer Portal

Staff console for the **Sunbird RC registry**: manage issuing authorities and their
holders and records, and issue verifiable credentials **whose contents come from
those records**.

One Keycloak realm serves both this portal and the wallet, which is what makes
"only my own credentials" true rather than aspirational.

> **Scope: this portal is a registry front end.** `oid4vc-service` can also issue
> credentials whose claims come from an authority's *own* database, via a claim
> source it hosts itself
> ([details](../../services/oid4vc-service/README.md#where-a-credentials-claims-come-from-the-registry-is-optional)).
> Those credential types are outside this portal's remit: its record CRUD has
> nothing to manage, and its "what will be issued" preview reads the registry, so
> for such a type the preview would not reflect what actually gets issued. An
> own-database authority manages records in its own system.

---

## Review it locally (no backing services needed)

```bash
cd apps/issuer-portal
npm install
npm run dev:mock
```

Then open **http://localhost:5174/issuer-portal/**

`dev:mock` sets `PORTAL_MOCK=1`, which replaces the registry, Keycloak and
oid4vc-service with an in-memory store holding four sample farmers. The UI shows
an unmissable banner while it's on. Sample data deliberately includes the awkward
cases:

| Farmer | Why it's there |
| --- | --- |
| `FRM-000123` Ravi Kumar | Fully populated, **two** land parcels — exercises the parcel picker |
| `FRM-000124` Lakshmi Devi | One parcel, already has a credential issued |
| `FRM-000125` Anand Patil | **No** land parcel — issuance is correctly blocked on a missing required claim |
| `FRM-000126` Sunita Rao | Minimal record, no wallet login linked |

Mock QR codes are **not scannable** and say so on the image. Everything else —
the claim resolution, the missing-value block, the PIN, the countdown, the
collected state (simulated after 25s) — behaves as it will in production.

### Against real services

```bash
npm run build
KEYCLOAK_PUBLIC_URL=https://98.70.36.106.sslip.io/auth \
KEYCLOAK_INTERNAL_URL=https://98.70.36.106.sslip.io/auth \
KEYCLOAK_REALM=sunbird-rc \
KEYCLOAK_CLIENT_ID=issuer-portal \
KEYCLOAK_CLIENT_SECRET=… \
SESSION_SECRET=$(openssl rand -base64 32) \
PUBLIC_BASE_URL=http://localhost:4100 \
REGISTRY_BASE_URL=https://98.70.36.106.sslip.io/registry \
OID4VC_BASE_URL=https://98.70.36.106.sslip.io \
SCHEMA_BASE_URL=https://98.70.36.106.sslip.io \
node bff/server.mjs
```

Provision the realm first with
`node ../scripts/setup-keycloak-issuer.mjs`, which creates the `issuer-portal`
and `sunbird-wallet` clients, the `issuer-staff` / `citizen` roles, the `farmerId`
claim mapper and sample users.

---

## Architecture

```
browser ──► BFF (node, zero deps) ──► Keycloak   (login, and linking a farmerId)
                     │              ──► registry   (farmer/land/crop/seed CRUD)
                     │              ──► oid4vc-service (offer creation, QR)
                     └──► serves the built React bundle
```

**Why a BFF and not a static bundle like `verifier-app`.** The Keycloak client is
confidential, so the code-for-token exchange needs a secret that cannot ship to a
browser. Two things follow: the browser holds only an httpOnly session cookie and
never a token, and every call is same-origin — which sidesteps the CORS problem
that made the verifier console hang on "Loading credential types…", because
registry and credential-schema send no `access-control-allow-origin`.

### Two issuance paths

**Path A — staff hand over a QR and a PIN.** Built here. Claims resolve from the
registry, are shown read-only, and issuance is blocked if a required value is
missing. The offer carries a 6-digit `tx_code`; the PIN is what proves the QR
reached the intended person, since a QR alone is a bearer token.

**Path B — the farmer adds it themselves.** Staff link the farmer record to a
Keycloak account on the Profile tab, which writes `farmerId` onto that user. The
farmer then signs into their wallet with the same account, and oid4vc-service
reads `farmerId` off the presented access token to pick the record. Because the
record follows *who signed in*, a farmer can only ever get their own credential —
and no PIN is needed, as the login already proved identity.

Path B is implemented server-side in oid4vc-service:
`authorization_servers` advertises the Keycloak realm, `TokenService` verifies
realm-issued tokens against the realm JWKS, and the credential endpoint builds
claims from the registry record named by the token. Enable it by setting
`KEYCLOAK_PUBLIC_URL` and `REGISTRY_BASE_URL` on that service. Keycloak is the
authorization server and oid4vc-service is a resource server, so there is no
`/authorize`, no PKCE implementation and no consent UI of our own to maintain.
The remaining unknown is the wallet's redirect scheme — see below.

---

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `4100` | BFF listen port |
| `BASE_PATH` | `/issuer-portal` | Path prefix; must match `base` in `vite.config.ts` and the nginx location |
| `PUBLIC_BASE_URL` | `http://localhost:$PORT` | Browser-facing origin. Builds `redirect_uri`, and its scheme decides whether the session cookie is `Secure` |
| `KEYCLOAK_PUBLIC_URL` | `http://localhost:8080/auth` | Used for **redirects** — must be reachable from a browser |
| `KEYCLOAK_INTERNAL_URL` | = public | Used for **token/userinfo/admin** — stays inside the compose network |
| `KEYCLOAK_REALM` | `sunbird-rc` | |
| `KEYCLOAK_CLIENT_ID` | `issuer-portal` | Confidential client |
| `KEYCLOAK_CLIENT_SECRET` | — | **Required** unless `PORTAL_MOCK=1` |
| `SESSION_SECRET` | — | **Required** unless `PORTAL_MOCK=1`. HMAC key for the session cookie |
| `REGISTRY_BASE_URL` | `http://localhost:8081` | |
| `OID4VC_BASE_URL` | `http://localhost:3400` | |
| `SCHEMA_BASE_URL` | `http://localhost:3333` | Credential type discovery |
| `PORTAL_MOCK` | unset | `1` replaces all three backends with in-memory sample data |

Splitting the two Keycloak URLs is load-bearing, not fussiness: conflating them is
the classic Keycloak-behind-a-gateway failure, where redirects point at an
unreachable internal name or back-channel calls leave the network needlessly.

---

### Enabling Path B on oid4vc-service

| Variable | Purpose |
| --- | --- |
| `KEYCLOAK_PUBLIC_URL` | Turns the whole feature on, and is what gets advertised in issuer metadata |
| `KEYCLOAK_INTERNAL_URL` | In-cluster base for JWKS fetches; defaults to the public URL |
| `KEYCLOAK_REALM` | `sunbird-rc` |
| `KEYCLOAK_SUBJECT_CLAIM` | Token claim holding the registry key; default `farmerId` |
| `KEYCLOAK_AUDIENCE` | Optional `aud` check. Leave blank — Keycloak's default access token has `aud=account` |
| `REGISTRY_BASE_URL` | Where claims are read from. Without it, self-service refuses rather than falling back to caller-supplied claims |
| `OFFER_REQUIRES_STAFF` | `true` requires the `issuer-staff` role on `POST /oid4vc/offer` |

**One open item.** The `sunbird-wallet` Keycloak client's redirect URIs are
placeholders until the wallet build's real deep-link scheme is read off it;
`setup-keycloak-issuer.mjs` prints a warning saying so. Re-run it with
`WALLET_REDIRECT_URIS=…` once known. Nothing else in Path B depends on this.

## Things worth knowing before changing this

- **Claim mapping is duplicated, deliberately and dangerously.**
  `bff/claim-mapping.mjs` resolves registry records into claims for Path A;
  `oid4vc-service/src/oid4vci/registry-claims.util.ts` does the same for Path B,
  where the portal isn't involved at all. They are two implementations of one
  rule, in two languages, and they **will** drift. Change one, change the other;
  the round-trip test below is what catches a drift that review misses.
- **`farmerId` is immutable.** It's the join key for every child record, so the
  form locks it after creation and the registry's unique index enforces it.
- **Sessions are in-process.** More than one replica needs sticky sessions or a
  shared store; single replica is the intended deployment.
- **Credential types are filtered, not listed.** A type is offered only if it
  supports `vc+sd-jwt` *and* its author DID is resolvable by a wallet. Ten of the
  twelve SD-JWT schemas in this deployment fail the second test; issuing them
  would produce credentials that look fine here and fail in every wallet. The
  hidden count is surfaced in the UI rather than silently dropped.

## Verifying the Keycloak login flow

Both login flows can be verified locally, without the deployed server.

### 1. Start Keycloak and provision it

```bash
docker run -d --name kc-verify --platform linux/amd64 -p 8085:8080 \
  -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin123 -e DB_VENDOR=h2 \
  ghcr.io/sunbird-rc/sunbird-rc-keycloak:latest
# ~70s to boot on Apple silicon (amd64 under emulation)
until curl -sf http://localhost:8085/auth/realms/master >/dev/null; do sleep 5; done

cd "$(git rev-parse --show-toplevel)"
KC_BASE=http://localhost:8085 KC_ADMIN=admin KC_ADMIN_PASSWORD=admin123 \
PORTAL_PUBLIC_URL=http://localhost:4100 \
ISSUER_PORTAL_CLIENT_SECRET=local-portal-secret \
node scripts/setup-keycloak-issuer.mjs
```

The script creates the realm if absent, so this works against a fresh Keycloak —
the published image does **not** import `sunbird-rc`. Re-running prints all `✓`
and no `+`, which is the idempotency check.

### 2. Staff login (confidential client, through the BFF)

```bash
cd issuer-portal
PORT=4100 PUBLIC_BASE_URL=http://localhost:4100 \
KEYCLOAK_PUBLIC_URL=http://localhost:8085/auth \
KEYCLOAK_INTERNAL_URL=http://localhost:8085/auth \
KEYCLOAK_CLIENT_SECRET=local-portal-secret \
SESSION_SECRET=local-session-secret \
REGISTRY_BASE_URL=http://localhost:8081 \
OID4VC_BASE_URL=http://localhost:3400 \
SCHEMA_BASE_URL=http://localhost:3333 \
node bff/server.mjs
```

Open **http://localhost:4100/issuer-portal/** and sign in as `issuer.staff` /
`Passw0rd!`. What to confirm:

| Check | Expected |
| --- | --- |
| Before login, `GET /api/farmers` | `401` |
| `GET /api/session` before login | `200` with `authenticated: false` — a probe, not a gate |
| After login | `portal_sid` cookie set, httpOnly, `path=/issuer-portal` |
| Wrong password | stays on the Keycloak form; **no** `portal_sid` cookie |
| Signing in as `farmer.ravi` | session works, but roles lack `issuer-staff`, so the UI shows the no-issuing-role warning |
| `/logout` | a fresh client sees `authenticated: false` |

### 3. Wallet login (public client, authorization_code + PKCE)

This is the flow a wallet runs. Verified values on a local run:

```
farmer.ravi     -> farmerId FRM-000123    roles ['citizen', …]
farmer.lakshmi  -> farmerId FRM-000124
farmer.norecord -> farmerId absent
```

`farmerId` must appear in the **access** token, not only the ID token — the wallet
presents the access token at the credential endpoint, so an ID-token-only claim
would be invisible to oid4vc-service. Exchanging the code without a
`code_verifier` must be rejected, which is what `pkce.code.challenge.method=S256`
on the client enforces.

Then, with `KEYCLOAK_PUBLIC_URL` and `REGISTRY_BASE_URL` set on oid4vc-service:

| Request | Expected |
| --- | --- |
| Ravi's token → `POST /oid4vc/credential` | log shows `farmerId=FRM-000123` |
| Lakshmi's token | log shows `farmerId=FRM-000124` |
| Lakshmi's token **plus** `farmerId: FRM-000123` and forged `claims` in the body | still `FRM-000124` — the body is not consulted |
| `farmer.norecord`'s token | `400` naming the missing link, not a `500` |
| A token from the `master` realm | `401` |
| A token with an edited payload | `401` |

And the staff gate, with `OFFER_REQUIRES_STAFF=true`:

| Token on `POST /oid4vc/offer` | Expected |
| --- | --- |
| none | `401` |
| `citizen` | `403` |
| `issuer-staff` | `201`, with a 6-digit `tx_code` that is **absent** from `credential_offer` |

### 4. Without any of the above

`npx jest token.service.keycloak` covers realm-token acceptance against a real
JWK set served over HTTP — wrong key, wrong issuer, expired, audience opt-in, and
the public/internal URL split — with no Keycloak running.

## Verification

1. `npm run build` — clean under `strict`, `noUnusedLocals`, `noUnusedParameters`.
2. Mock walkthrough: create a farmer → add a parcel → issue → QR and PIN appear.
   Then open `FRM-000125` and confirm issuance is **blocked** with
   `landAreaAcres` named.
3. Two-parcel check: on `FRM-000123`, switch parcels and confirm
   `landRecordRef`, `landAreaAcres` and `ownershipType` all change.
4. Auth gate (non-mock): every `/api/*` route returns 401 without a session, a
   forged cookie MAC returns 401, and `/callback` without `state` returns 400.
5. Round-trip against real services: issue via the portal, collect in Paradym,
   then verify in the verifier console — all seven checks OK. This is the test
   that catches claim-mapping drift.
6. Path B, server side: `cd .. && npx jest self-service tx-code` — 22 tests
   covering that claims come from the token's record, that wallet-supplied claims
   are ignored, that naming another farmer in the request changes nothing, that a
   wrong PIN burns the offer, and that the staff gate fails closed.
7. Path B, end to end: sign into Paradym as `farmer.ravi`, add the credential,
   and confirm it carries `FRM-000123`'s data. Then repeat as `farmer.lakshmi`
   and confirm the first citizen's data is unreachable. **Test this explicitly
   rather than inferring it** — it is the property the whole design exists for.
