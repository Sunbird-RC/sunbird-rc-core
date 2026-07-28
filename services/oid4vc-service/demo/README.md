# OID4VC Demo — 3 use cases, all formats, production-like wallet

Three privacy-preserving verifiable-credential use cases over Sunbird RC's
oid4vc-service, with a Keycloak "National Identity System" for authentication,
mock issuer/verifier portals, and an account-based wallet.

| # | Use case | Issuer(s) | Verifier | Keycloak users |
|---|----------|-----------|----------|----------------|
| 1 | Age Verification | National Identity Authority | Age Verification Portal | citizen.over18, citizen.under18 |
| 2 | Agriculture Rural Credit | Farmer Registry Authority | Rural Credit Portal | farmer.male, farmer.female |
| 3 | Education Credentials | 3 institutions (Secondary Board, University, PG Institute) | Education Verification Portal | learner.secondary, learner.graduate, learner.postgraduate |

All users share the password **`Passw0rd!`**.

## What's new in v2
- **All 4 formats** (`ldp_vc`, `jwt_vc_json`, `vc+sd-jwt`, `mso_mdoc`) selectable
  per credential on each Issuer/Verifier card. True selective disclosure is real
  for **`vc+sd-jwt`** and **`mso_mdoc`**; ldp_vc/jwt_vc_json share the whole
  credential (labelled in the UI).
- **Production-like wallet**: signup/login (`wallet-backend`), a **stable holder
  DID** persisted per account (only reset on demand), and per-user credential
  storage. The holder private key is encrypted client-side (PBKDF2→AES-GCM) so
  the server only stores ciphertext.

## Components (this folder + siblings)

| Path | Role |
|---|---|
| `seed-demo.mjs` | Seeds Farmer + Academic×3 schemas (all 4 formats) + issuer DIDs; warms ES256 keys so mso_mdoc works. |
| `keycloak/setup-keycloak.mjs` | Creates the `national-id-portal` client + all 7 demo users with attributes. |
| `keycloak/disable-ssl-required.sh` | One-time: lets admin REST calls work against the legacy Keycloak. |
| `server.mjs` | Hosts both portals (data-driven `ISSUERS`/`VERIFIERS`), OIDC login, offer creation. |
| `../wallet-backend/` | Accounts + persistence service (`/wallet-api`). |
| `../test-wallet/` | The browser wallet (login gate, pass cards, consent, all 4 formats). |
| `test-usecases.mjs` | Headless proof of all 3 use cases across formats (asserts no leaks). |
| `test-wallet-account.mjs` | Headless proof of stable DID + persistence + reset. |

## Run (local)

Prereqs: the V2 stack (`identity`, `credential-schema`, `credential`,
`oid4vc-service` on :3400) + Keycloak on :8080 + Postgres `db`.

```bash
cd services/oid4vc-service/demo && npm install
node seed-demo.mjs                              # schemas + issuer DIDs (+ mdoc warm-up)
bash keycloak/disable-ssl-required.sh           # one-time (KC_CONTAINER=<name> on the VM)
node keycloak/setup-keycloak.mjs                # client + 7 users

# wallet backend (accounts) — needs Postgres reachable
cd ../wallet-backend && npm install
# WALLET_TRUSTED_VERIFIERS configures the browser wallet's allowlist (fetched
# once at startup via GET /wallet-api/trusted-verifiers, enforced in
# test-wallet/src/trust.mjs before any credential is presented). Unset =
# accept any verifier, with a console warning — fine for this demo, not for
# a real deployment. clientId must match what oid4vc-service's /vp/request
# emits (see §4.1 in the service README for OID4VP_SIGN_REQUEST /
# OID4VP_LEGACY_CLIENT_ID_SCHEME); allowUnsigned is needed for the legacy
# unsigned demo path.
export WALLET_TRUSTED_VERIFIERS='[{"clientId":"did:web:demo-verifier.local","responseUriOrigin":"http://localhost:4000","allowUnsigned":true}]'
PGHOST=<db-host> PGPASSWORD=<pw> node server.mjs   # :4100

# portals
cd ../demo && OID4VC_BASE=http://localhost:3400 PORT=4000 node server.mjs
#   Issuer:   http://localhost:4000/issuer
#   Verifier: http://localhost:4000/verifier

# wallet
cd ../test-wallet && npm run dev                 # http://localhost:5555
```

## Demo flow (browser)
1. **Wallet** (`:5555`) → **Sign up** (any username/password). Note the stable
   holder DID in the sidebar.
2. **Issuer Portal** (`:4000/issuer`) → pick an issuer + format → log in as the
   matching user → the portal issues the credential and shows a QR / offer link.
3. Paste the offer link into the wallet's **Receive** tab → the credential is
   stored to your account.
4. **Verifier Portal** (`:4000/verifier`) → pick the matching service + format →
   scan/paste the `openid4vp://` link into the wallet's **Present** tab → consent
   → the verifier shows the result and exactly what was disclosed.
5. Sign out / sign back in → **same DID**, credentials still there. Settings →
   **Reset identity** → new DID, credentials cleared.

## Automated proof (no browser)
```bash
node test-usecases.mjs        # UC1/UC2/UC3 across sd-jwt + mso_mdoc; asserts only requested claims disclosed
node test-wallet-account.mjs  # stable DID across re-login, persistence, reset (needs wallet-backend @ :4100)
```

## Deploy (when instructed)
`docker-compose.demo.yml` adds `keycloak`, `demo`, and `wallet-backend` over the
cloud stack; nginx needs `/issuer /verifier /callback /qr /shared /auth
/wallet-api` routes. mso_mdoc in the browser needs HTTPS (secure context) — the
VM already serves HTTPS.
