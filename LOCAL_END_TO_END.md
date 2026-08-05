# Running the issuer portal end to end, locally

One command brings up the whole stack; a second wires it together. No cloud
deployment, no server access.

```bash
docker compose -f docker-compose.local.yml up -d
./scripts/bootstrap-local.sh
open http://localhost/issuer-portal/
```

First run takes ~10 minutes: four of the images are amd64-only, so on Apple
silicon they run under emulation, and the Java registry is slow to boot. Later
runs are fast.

| URL | What |
| --- | --- |
| http://localhost/issuer-portal/ | Issuer portal (staff) |
| http://localhost/verifier-app/ | Verifier console |
| http://localhost/auth/admin | Keycloak admin (`admin` / `admin123`) |

> **Use port 80, not 5174.** `npm run dev:mock` serves the portal on
> **http://localhost:5174** with **sample data and no Keycloak** — useful for
> reviewing the UI offline, and it says so in a banner across the top. It is a
> different application instance from the one this stack runs. If you see
> "Mock mode" or a farmer called **Sunita Rao (FRM-000126)**, you are on the mock
> server: the real registry has three farmers, `FRM-000123`–`FRM-000125`.

| Login | Password | What they see |
| --- | --- | --- |
| `issuer.staff` | `Passw0rd!` | The full console: all farmers, CRUD, issuance |
| `farmer.ravi` | `Passw0rd!` | **Only `FRM-000123`**, read-only, plus add-to-wallet. Two parcels |
| `farmer.lakshmi` | `Passw0rd!` | **Only `FRM-000124`** |
| `farmer.norecord` | `Passw0rd!` | "Account not linked" — no record at all |

### Who can do what

Staff manage the registry; a farmer sees only their own record. Enforced in the
portal's BFF, **not** in the UI — hiding a table in React would leave the API
open, so the gate is server-side and the UI merely stops offering actions that
would be refused.

| Route | `issuer-staff` | `citizen` |
| --- | --- | --- |
| `GET /api/farmers` (list everyone) | 200 | **403** |
| `GET/PUT/DELETE /api/farmers/:id` | 200 | **403** |
| `POST /api/farmers` (create) | 201 | **403** |
| `POST /api/farmers/:id/link` | 200 | **403** |
| `POST /api/entities/*` | 201 | **403** |
| `POST /api/claims/resolve`, `POST /api/offers` | ok | **403** |
| `GET /api/me`, `POST /api/me/offer` | n/a | **own record only** |

A citizen's record is chosen from the **`farmerId` claim in their token**, never
from a path or body parameter. Verified: `POST /api/me/offer` with
`farmerId: FRM-000124` and forged `claims` in the body still issues `FRM-000123`'s
credential with its real 4.5 acres.

Read-only for citizens is deliberate. If the subject could edit the data, a
credential asserting it would be asserting something its own subject controls,
which defeats the point of an authority issuing it.

## What to click through

1. **Sign in as `issuer.staff`.** The farmer list is read from the real registry.
2. **Open `FRM-000123` → Issue credential.** Two parcels, so the picker appears.
   Switch between them and watch `landRecordRef`, `landAreaAcres` and
   `ownershipType` change. The "From" column shows which record each value came
   from; nothing is editable.
3. **Open `FRM-000125` → Issue credential.** Issuance is **blocked**, naming
   `landAreaAcres`, because that farmer has no parcel.
4. **Back on `FRM-000123`, generate an offer.** You get a QR and a 6-digit PIN.
   Scan it with a wallet and enter the PIN.
5. **Verify at `/verifier-app/`.**

## Verified working

Driven end to end against this stack, not asserted:

- **Staff login** — Keycloak form → `/callback` → httpOnly session → registry reads.
- **Claim resolution per parcel** — `LR-KA-77-2201` gives 4.5 acres/Owned,
  `LR-KA-77-2202` gives 1.75/Leased, both with `primaryCrop=Wheat`.
- **Blocked issuance** — `FRM-000125` reports `missing: ['landAreaAcres']`.
- **Wrong PIN refused**, and it **consumes the offer**, so a 6-digit code cannot
  be brute-forced.
- **Correct PIN issues a real signed SD-JWT VC** — signed by the issuer
  `did:web`, correct `vct`, `cnf` holder binding, 8 `_sd` digests and 8
  disclosures, every claim matching the registry.
- **Path B (wallet self-service)** — `farmer.ravi` receives `FRM-000123`,
  `farmer.lakshmi` receives `FRM-000124`, `farmer.norecord` is refused with a
  message naming what an administrator must do. Both wallets sent hostile
  `claims` asking for another farmer's data and 999 acres; **both received only
  their own record**.
- **`/vp/request`** issues a transaction, so the verifier console works too.

## Why this file is not just the cloud compose with different values

Three things genuinely differ on a laptop, and each cost a debugging round:

1. **Vault runs in dev mode.** The cloud file uses file storage, which must be
   initialised and unsealed by hand before identity-service can sign anything.
   Dev mode starts unsealed with a fixed root token; the bootstrap mounts a
   kv-v2 engine at `kv`, which is the path `VAULT_ROOT_PATH` points at.
2. **nginx binds port 80**, so `PUBLIC_URL` carries no port. A `did:web` with a
   port needs it percent-encoded, and the portal's credential-type filter builds
   a plain `did:web:<host>:` prefix — a ported URL silently hides every
   credential type.
3. **Postgres creates all four databases up front.** Prisma will not create a
   missing database; it fails at boot. The cloud deployment happens to have had
   them created by hand.

## Things that bit, and are now handled

Recorded because each was invisible from the outside:

| Symptom | Cause |
| --- | --- |
| `did:web:http%3A::localhost:<uuid>` | `WEB_DID_BASE_URL` must be a **bare host**. `getDidPrefixForBaseUrl` (did.service.ts:63) strips only the literal `https://`, then percent-encodes any remaining colon |
| registry: `Could not resolve placeholder 'elastic_search_password'`, then `'sunbird_keycloak_user_password'` | The Java registry resolves **every** `${...}` while building config beans, before any `*_enabled` flag. All 84 variables are required; `scripts/local-registry.env` has the full set |
| registry `unhealthy` while serving fine | Its image has `wget` and `nc` but **no curl** |
| keycloak `unhealthy` while serving fine | It binds its eth0 address, so `localhost:8080` inside the container is refused. Probe `$(hostname -i)` |
| `/registry/health` → 404, then 500 | With a **variable** `proxy_pass`, nginx does not strip the location prefix — an explicit `rewrite` is needed. And `set` must come **before** `rewrite ... break`, or it never runs (`using uninitialized "up" variable`) |
| `500 Error issuing credential` | The schema had `additionalProperties: false`, but issuance always adds `credentialSubject.id` (the holder DID). The real reason only appears in credentials-service's log as `additionalProperty: "id"` |
| Citizen saw every farmer, and could create records | The BFF had one gate — "is there a session" — so every route was open to any logged-in account. Role gates are now per-route, and a citizen's own record is resolved from their token claim rather than a request parameter |
| "Link login" returned 502 | `serviceAccountsEnabled: false` on the `issuer-portal` client, while the BFF obtains its service token via `client_credentials` (401 → surfaced as 502). Now enabled, with `issuer-staff` and realm-management `manage-users` granted to the service account |
| Wallet: "something went wrong", nothing in the server log | Issuer metadata advertised **two** authorization servers, and OID4VCI then requires each offer grant to name which one applies. A Credo wallet refuses outright: *"...multiple entries, but the credential offer grant did not specify which authorization server to use."* Fixed by setting `authorization_server` on the grant whenever more than one is advertised |
| Wallet: invalid VCT type metadata | Every `display` entry MUST carry a `locale`; `@sd-jwt/sd-jwt-vc` (which Credo runs on the fetched document) rejects the whole credential without it. Now defaulted to `en-US` on both the vct document and issuer metadata |
| Wallet reached nothing at all, locally | The QR said `http://localhost/...`; on a phone `localhost` is the phone. `credential_issuer`, `vct` and the issuer DID were also localhost, and `did:web` mandates HTTPS. **A phone wallet cannot work against the local stack** — use the deployed HTTPS host |
| Schema created but nothing issuable | `oid4vciEnabled` and `oid4vciFormats` are the field names, and the schema must be `PUBLISHED` — `getOid4vciConfigs` filters on both |
| Sign out → Keycloak "We are sorry… Invalid redirect uri" (400) | Keycloak validates the **logout** redirect against the client's `redirectUris` (with `post.logout.redirect.uris: '+'` meaning "reuse them"). Only `/callback` was registered, but logout returns to the portal's landing page. Now a wildcard over the base path is registered. The local session was already cleared at that point, so it looked like logout half-worked |
| Sign out did nothing in mock mode | `readSession()` returned a session unconditionally when `PORTAL_MOCK=1`, so clearing the cookie changed nothing. Mock mode now mints a real cookie-backed session at `/login`, sharing one code path with the Keycloak flow — so the logout button is exercised in mock mode too |
| Keycloak logout parameter | Versions ≤17 accept `redirect_uri`; 18+ want `post_logout_redirect_uri` plus `client_id` or `id_token_hint`. All are sent, so the same code works across the range |
| `POST /credential-schema/credential-schema` → 404 | The service's controller is mounted at that prefix and serves POST at its **root**: `POST /credential-schema` |

## Notes

- **oid4vc-service is built from source, not pulled.** The published
  `credo-fix4` image predates the Keycloak self-service issuance, the real
  `tx_code` and `GET /qr` — running it would test the old behaviour while
  appearing to test the new.
- **`did:web` on localhost.** The method mandates https, so a strict external
  resolver will look for `https://localhost/<uuid>/did.json` and fail.
  identity-service resolves its own DIDs from its database and the portal's
  filter is a string prefix match, so the portal and issuance work regardless.
  Only third-party verification of the issuer DID needs the real HTTPS host.
- **Registry auth stays off locally, and cannot work in this topology.** Tried and
  measured, not assumed. Spring's resource-server config uses **one** URL for two
  jobs — the `iss` a token must carry, and the host the JWK set is fetched from —
  and locally those need different values:

  | `OAUTH2_RESOURCES_0_URI` | Result |
  | --- | --- |
  | `http://keycloak:8080/auth/...` | Valid staff tokens **401** — `iss` mismatch, since Keycloak stamps `iss` from the URL the *browser* used (`http://localhost/auth/...`) |
  | `http://localhost/auth/...` | Valid staff tokens **401** — inside the registry container `localhost` is the registry itself, so the JWKS fetch fails (confirmed by probing from in-container) |

  Worse, with auth **on** an anonymous `POST /api/v1/Farmer/search` still returned
  **200** — the registry gates writes (`401` on create) but not search. So it broke
  every authenticated read while protecting no reads.

  On a real deployment the public host is a DNS name resolvable from inside the
  network too, so one value serves both and this works. Locally, authorization is
  enforced in the portal's BFF, which is where the reported privilege bug actually
  lived.
- **The offer endpoint is open by default** (`OFFER_REQUIRES_STAFF=false`) so the
  verifier console's sample-issuance button keeps working. Set it `true` to
  require the `issuer-staff` role — verified: no token `401`, citizen `403`,
  staff `201`.
- Everything is throwaway: fixed dev credentials, no persistence worth keeping.
  `docker compose -f docker-compose.local.yml down -v` resets completely,
  including the databases, which forces the init script to re-run.
