# Use Case 1 — Age Verification (privacy-preserving, selective disclosure)

A citizen proves they are **over 18** to an Age Verification Portal **without
revealing date of birth or any other attribute**. A mock **National Identity
Authority** authenticates the citizen via **Keycloak** (real OIDC login), issues
a **National Identity Credential** (`vc+sd-jwt`), and the wallet later presents
**only `over_18`** using SD-JWT selective disclosure.

```
Keycloak (mock National ID System)          oid4vc-service (Sunbird RC)
      │  OIDC auth-code login                     ▲ issue / verify
      ▼                                           │
 Issuer Portal ──POST /oid4vc/offer──────────────┘
 (National Identity Authority)   offer (openid-credential-offer://)
      │                                           
      ▼ scan/paste                                
   Wallet (holder)  ──present ONLY over_18──►  Verifier Portal
   holds all 5 attrs   (SD-JWT selective       (Age Verification)
                        disclosure)             PASS / FAIL
```

## Components (this folder)

| Path | Role |
|---|---|
| `seed-national-id.mjs` | Seeds the `National Identity Credential` (`vc+sd-jwt`) schema + a "National Identity Authority" issuer DID. |
| `keycloak/setup-keycloak.mjs` | Creates the `national-id-portal` client + two citizens (`citizen.over18`, `citizen.under18`, pw `Passw0rd!`) with identity attributes. |
| `keycloak/disable-ssl-required.sh` | One-time: lets the admin REST calls work against the local legacy Keycloak. |
| `server.mjs` | Hosts both portals; runs the issuer-side OIDC login + `POST /oid4vc/offer`. |
| `issuer/`, `verifier/` | The two portal web pages. |
| `test-selective-disclosure.mjs` | Headless proof: issue 5 attrs, present only `over_18`, assert nothing else leaks. |
| `test-e2e-oidc.mjs` | Headless proof of the **whole** chain incl. real Keycloak login. |

## Prerequisites

The V2 stack must be running (`db vault redis identity credential-schema
credential` + `oid4vc-service` on `:3400`) and **Keycloak** on `:8080`
(`docker compose up -d keycloak`). SD-JWT uses the wallet's pure-JS crypto, so
everything here works over plain HTTP — no HTTPS needed.

## Run (local)

```bash
cd services/oid4vc-service/demo
npm install

# 1. Seed the National Identity schema (idempotent)
node seed-national-id.mjs

# 2. Configure Keycloak (one-time ssl flag, then client + users)
bash keycloak/disable-ssl-required.sh
node keycloak/setup-keycloak.mjs

# 3. Start the portals
OID4VC_BASE=http://localhost:3400 PORT=4000 npm start
#   Issuer Portal:   http://localhost:4000/issuer
#   Verifier Portal: http://localhost:4000/verifier
```

## Demo script (browser)

1. **Issue** — open `http://localhost:4000/issuer` → *Login with National ID* →
   sign in as `citizen.over18` / `Passw0rd!`. The National Identity Authority
   validates identity + DOB and issues the credential; scan the QR (or paste the
   `openid-credential-offer://` link) into the **wallet**
   (`services/oid4vc-service/test-wallet`, `npm run dev` → `:5555`, or the
   deployed `/wallet/`). The wallet now holds all 5 attributes.
2. **Present** — open `http://localhost:4000/verifier` → *Request "Over 18"
   proof* → scan the QR with the wallet. The wallet shows a **consent** prompt
   listing exactly what will be shared (`over_18` only) → confirm. The Verifier
   Portal shows **ACCESS GRANTED**, the disclosed claims (`{over_18: true}` and
   nothing else), and the six verification checks.
3. **Negative** — repeat as `citizen.under18` → **ACCESS DENIED** (not over 18),
   still disclosing only `over_18`.

## Automated proof (no browser)

```bash
node test-selective-disclosure.mjs   # issue 5 attrs, present only over_18, assert no DOB leak
node test-e2e-oidc.mjs               # full chain incl. real Keycloak OIDC login
```
Both print a PASS (over-18) / FAIL (under-18) summary and assert that
`date_of_birth`, `full_name`, `national_id`, `gender` are **never** disclosed —
only `over_18` reaches the verifier.

## What this demonstrates

- **Trusted issuer** — credential signed by the National Identity Authority DID;
  the verifier checks the signature (`credentialSignatures: OK`) and status
  (`revocation: OK`, backed by a real status list when `STATUS_LIST_ENABLED=true`).
- **Authenticated issuance** — real Keycloak OIDC login gates credential issuance.
- **Selective disclosure** — the citizen holds DOB, name, ID, gender, over_18,
  but shares **only over_18**. Holder binding, nonce/replay, and DCQL
  satisfaction are all verified.
