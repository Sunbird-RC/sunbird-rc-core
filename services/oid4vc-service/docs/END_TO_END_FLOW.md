# End-to-end flow: issuance and verification, step by step

A walkthrough of what actually happens on the wire when a credential is issued
into a wallet and later presented to a verifier — every HTTP call, what each
response contains, and which specification governs it.

Written to be readable without prior OpenID4VC knowledge. All examples are real
responses captured from the running deployment at
`https://98.70.36.106.sslip.io`, with a Paradym wallet (Android) as the holder.

## Table of Contents

- [The cast, and one idea that explains the rest](#the-cast-and-one-idea-that-explains-the-rest)
- [1. Which specification does what](#1-which-specification-does-what)
- [2. Issuance, step by step](#2-issuance-step-by-step)
- [3. What the credential actually looks like](#3-what-the-credential-actually-looks-like)
- [4. Verification, step by step](#4-verification-step-by-step)
- [5. The three random values, and what each defends](#5-the-three-random-values-and-what-each-defends)
- [6. Timeouts](#6-timeouts)
- [7. Draft-13 vs final-1.0 differences](#7-draft-13-vs-final-10-differences)
- [8. How to debug this](#8-how-to-debug-this)

Existing diagrams: [`images/vci-flow.png`](images/vci-flow.png) (issuance) and
[`images/vp-flow.png`](images/vp-flow.png) (presentation).

---

## The cast, and one idea that explains the rest

Three parties:

| Party | In this deployment |
| --- | --- |
| **Issuer** | `oid4vc-service`, which mints credentials |
| **Holder** | a wallet app on a phone (Paradym) |
| **Verifier** | `oid4vc-service` again, in its verifier role, driven by the verifier console at `/verifier-app/` |

**The single most useful idea: both QR codes are only pointers.** Neither carries
a credential, a request, or any personal data. Each contains a URL the wallet
fetches over HTTPS, plus the minimum needed to trust what comes back. That is why
the wallet always makes several follow-up calls after a scan.

One term used throughout: a **JWT** is a piece of text that has been digitally
signed. Anyone can read it; nobody can alter it without breaking the signature.

---

## 1. Which specification does what

| Specification | Responsibility |
| --- | --- |
| **OpenID4VCI** (OpenID for Verifiable Credential Issuance) | the conversation that gets a credential *into* a wallet |
| **OpenID4VP** (OpenID for Verifiable Presentations) | the conversation that gets a claim *out of* a wallet |
| **SD-JWT VC** + **SD-JWT** core | the credential *format*, and what makes selective disclosure possible |
| **OAuth 2.0** (RFC 6749 / 6750) | the token endpoint and bearer auth that OpenID4VCI builds on |
| **W3C DID Core**, `did:web`, `did:key` | how issuer and holder keys are named and resolved |

Two boundaries worth internalising:

- **OpenID4VCI is format-agnostic.** It carries `vc+sd-jwt`, `jwt_vc_json`,
  `ldp_vc` or `mso_mdoc` identically — it names the format and passes the bytes.
  That is why one flow serves all four formats.
- **OpenID4VCI's job ends the moment the credential is stored.** Everything under
  `/vp/*` is OpenID4VP. They are separate specs with separate endpoints.

---

## 2. Issuance, step by step

### Step 0 — your application creates the offer

**Calls:** `POST /oid4vc/offer` with the credential type and the subject's values.

**Gets back:** a QR payload —

```
openid-credential-offer://?credential_offer_uri=https://98.70.36.106.sslip.io/oid4vc/offer/819887f4-…
```

The subject's values are now held **server-side** against that offer id. They are
not in the QR.

> `POST /oid4vc/offer` is this service's own API, not part of any spec. What it
> *produces* is OpenID4VCI's **Credential Offer**. The wallet never calls it.

### Step 1 — the wallet opens the link in the QR

**Calls:** `GET /oid4vc/offer/819887f4-…`

**Gets back:**

```json
{
  "credential_issuer": "https://98.70.36.106.sslip.io",
  "credential_configuration_ids": ["did:schema:c869c569-…_vc+sd-jwt"],
  "grants": {
    "urn:ietf:params:oauth:grant-type:pre-authorized_code": {
      "pre-authorized_code": "tEukI3xOPDMISL5eJhEAjtVccSRWZQvX"
    }
  }
}
```

| Field | Meaning |
| --- | --- |
| `credential_issuer` | which server to talk to |
| `credential_configuration_ids` | which credential is on offer |
| `pre-authorized_code` | a one-time ticket that stands in for logging in |

> **Spec:** OpenID4VCI **Credential Offer Endpoint**, by-reference form. The
> `openid-credential-offer://` scheme and the `credential_offer_uri` option are
> both VCI-defined; by-reference exists because QR codes have limited capacity.
> The grant is VCI's **Pre-Authorized Code Flow**.

### Step 2 — the wallet asks what can be issued, and how

**Calls:** `GET /.well-known/openid-credential-issuer`

**Gets back** (top level):

| Field | Value here |
| --- | --- |
| `credential_issuer` | `https://98.70.36.106.sslip.io` |
| `credential_endpoint` | `…/oid4vc/credential` |
| `nonce_endpoint` | `…/oid4vc/nonce` |
| `deferred_credential_endpoint` | `…/oid4vc/deferred` |
| `notification_endpoint` | `…/oid4vc/notification` |
| `credential_configurations_supported` | a catalogue (64 types at the time of writing) |

And inside the chosen catalogue entry:

```json
{
  "format": "vc+sd-jwt",
  "cryptographic_binding_methods_supported": ["did:web", "did:key", "jwk"],
  "credential_signing_alg_values_supported": ["ES256", "Ed25519Signature2020"],
  "proof_types_supported": { "jwt": { "proof_signing_alg_values_supported": ["ES256"] } },
  "vct": "https://98.70.36.106.sslip.io/vct/farmer-land-holding-credential",
  "display": [{ "name": "Farmer Land Holding Credential", "locale": "en-US" }]
}
```

Four decisions come out of this:

| Field | What the wallet concludes |
| --- | --- |
| `credential_endpoint` | where to request the credential |
| `proof_signing_alg_values_supported: ["ES256"]` | **"I must create a P-256 key."** A wallet that can only do Ed25519 stops here |
| `cryptographic_binding_methods_supported` | how to *name* that key. Paradym chooses `did:key` |
| `vct` + `display` | the type identifier, where to fetch display metadata, and the name to print on the card |

> **Spec:** OpenID4VCI **Credential Issuer Metadata** — the well-known path is
> fixed by the spec. `proof_types_supported` and
> `cryptographic_binding_methods_supported` are VCI; `vct` belongs to SD-JWT VC.

### Step 3 — the wallet asks where to redeem the ticket

**Calls:** `GET /.well-known/openid-configuration`

**Gets back:**

| Field | Value here |
| --- | --- |
| `token_endpoint` | `…/oid4vc/token` |
| `grant_types_supported` | `["urn:ietf:params:oauth:grant-type:pre-authorized_code"]` |
| `token_endpoint_auth_methods_supported` | `["none"]` — no client secret needed |
| `issuer`, `response_types_supported`, `jwks_uri` | identity and key information |

Step 2 said *what*; step 3 says *where to get permission*.

> **Spec:** this one is **not** OpenID4VCI. VCI delegates authorization-server
> discovery to OAuth, and there are two standard paths. A wallet may try either:
>
> | Path | Spec | Result here |
> | --- | --- | --- |
> | `/.well-known/oauth-authorization-server` | RFC 8414 | 404 |
> | `/.well-known/openid-configuration` | OpenID Connect Discovery 1.0 | 200 |
>
> The 404-then-fallback seen in the access log is correct wallet behaviour, not a
> fault.

> **Known wart:** `jwks_uri` advertises `/.well-known/jwks.json`, which currently
> returns 404. Nothing depends on it — the credential's signing key is found via
> the issuer's DID document instead — but the advertisement is inconsistent and
> should be either served or removed.

### Step 4 — the wallet redeems the ticket for permission

**Calls:**

```
POST /oid4vc/token
Content-Type: application/x-www-form-urlencoded

grant_type=urn:ietf:params:oauth:grant-type:pre-authorized_code&pre-authorized_code=tEuk…
```

**Gets back:**

| Field | Meaning |
| --- | --- |
| `access_token` | permission slip, valid 300 s |
| `token_type: Bearer` | how to send it |
| `expires_in: 300` | |
| `c_nonce` | a one-time random number the wallet must sign |
| `c_nonce_expires_in: 300` | |

Two things that are not obvious:

**The access token is itself a signed JWT (`typ: at+jwt`) whose `sub` is the offer
id** (`token.service.ts:41-55`). The token is therefore a *pointer back to the
subject's data*: the credential endpoint reads `sub` to find
`oid4vc:offer:<offerId>`. The wallet never sees or sends those values.

**The `c_nonce` is recorded server-side** as `oid4vc:nonce:<nonce>` with a 300 s
TTL, and is designed to be spent exactly once.

> **Spec:** OAuth 2.0 **Token Endpoint** (RFC 6749) with VCI additions —
> `access_token`, `token_type` and `expires_in` are OAuth; **`c_nonce` and
> `c_nonce_expires_in` are OpenID4VCI**.
>
> Draft-13 returned `c_nonce` in this response; 1.0 moved it to a dedicated
> **Nonce Endpoint**. This service does both — the token response carries
> `c_nonce` *and* `nonce_endpoint` is advertised — so wallets of either
> generation work.
>
> Using `sub` to carry the offer id is an implementation choice here, not a spec
> requirement.

### Step 5 — the wallet creates a key and proves it holds it

No HTTP call; this happens on the phone.

1. It generates a **fresh P-256 key pair inside the device keystore** — one per
   credential. The private half never leaves the device.
2. It derives a name from the public half: `did:key:zDnae…`
3. It builds a **proof-of-possession JWT**:

```
header    { "typ": "openid4vci-proof+jwt", "alg": "ES256",
            "kid": "did:key:zDnae…#zDnae…" }
payload   { "aud": "https://98.70.36.106.sslip.io", "iat": …, "nonce": "<c_nonce>" }
```

signed with the brand-new private key. The wallet is saying *"bind the credential
to this key, and here is proof I hold it right now."* `aud` proves the proof was
made for **this** issuer; `nonce` proves it was made **just now**.

> **Spec:** OpenID4VCI **Proof Types** (`jwt`). The `openid4vci-proof+jwt` type
> marker, the `aud` requirement and the `nonce` requirement are all VCI.
> `did:key` comes from W3C DID Core plus the did:key method — VCI only states
> which binding methods are permitted.

### Step 6 — the wallet requests the credential

**Calls:**

```
POST /oid4vc/credential
Authorization: Bearer <access_token>

{ "credential_configuration_id": "…", "format": "vc+sd-jwt",
  "proof": { "proof_type": "jwt", "jwt": "<proof of possession>" } }
```

**The server checks, in this order** (`oid4vci.service.ts:376-398`):

1. **Is the token valid?** — present, correctly signed, unexpired
2. **Find the subject's data** — `oid4vc:offer:<token.sub>`. If the 10-minute
   offer has lapsed the result is `Offer session expired`, *even though the token
   is still valid*
3. **Is a proof attached?**
4. **Burn the nonce** — an atomic read-and-delete. A replayed proof finds nothing
   and gets `invalid_or_missing_proof`, **returned together with a fresh
   `c_nonce`** so an honest wallet can retry immediately
5. **Verify the proof** — resolve the holder's key (inline `jwk`, else `kid` →
   DID; `did:key`/`did:jwk` resolved locally, other methods via the registry),
   check the signature, check `aud`, check the nonce matched
6. **Mint and sign** the credential, bound to the holder's key

The ordering is deliberate: **the nonce is burned before the signature is
verified**, so a malformed proof still costs the sender its one nonce.

> **Spec:** OpenID4VCI **Credential Endpoint** and **Credential Response**.
> Bearer auth is RFC 6750. The `proof` object, the `invalid_proof` error, and the
> convention of returning a fresh `c_nonce` alongside the error are all VCI.

### Step 7 — the wallet verifies before storing

**Calls:** `GET /<issuer-uuid>/did.json`, then `GET /vct/<slug>`

Then it checks:

1. **Is the issuer's signature genuine?** — resolve the issuer's `did:web`, take
   `#jwt-key-1`'s `publicKeyJwk`, verify the signature
2. **Do the values match their hashes?** — re-hash each disclosure and confirm it
   appears in the signed `_sd` list
3. **Is this locked to *my* key?** — compare `cnf` against the key from step 5
4. **What should it be called?** — fetch Type Metadata for the display name

> **Spec:** SD-JWT VC verification, W3C DID Core for resolution, RFC 7800 for
> `cnf`. Type Metadata is SD-JWT VC §11 (see `app.controller.ts:59`).
>
> **Path quirk:** SD-JWT VC §6.3.1 says a resolver should insert
> `/.well-known/vct` between the authority and the path of the `vct` URL. walt.id
> therefore fetches `/.well-known/vct/vct/<slug>` rather than the `vct` value
> itself, so this service serves **both** paths (`app.controller.ts:73`).

### Step 8 — stored

The wallet saves the signed credential, every disclosure, and a handle to the
keystore key. This is the point at which the wallet UI says *"Added …
Credential"*.

**OpenID4VCI's role is now complete.**

---

## 3. What the credential actually looks like

This is the part worth understanding properly, because it is what makes
selective disclosure work later
(`identity-service/src/vc/jwt.service.ts:178-209`).

For each disclosable attribute:

1. generate a random **16-byte salt**
2. build `disclosure = base64url(JSON.stringify([salt, name, value]))`
3. compute `digest = SHA-256(disclosure)`
4. **delete the attribute from the payload**, keeping only the digest

Then set `_sd` to the sorted digest list and `_sd_alg` to `sha-256`, sign, and
join everything with tildes:

```
<signed JWS: digests only>~<disclosure 1>~<disclosure 2>~<disclosure 3>~
```

> **The signature covers the digests, never the values.** The values travel
> alongside — unsigned, but bound by hash.

That single design choice is why, at presentation time, the wallet can simply
**omit the disclosures it does not wish to share** and the issuer's signature
still verifies: the verifier re-hashes whatever it *did* receive and checks
membership in `_sd`. Nothing is re-signed and the issuer is not involved.

The signed payload also carries `iss`, `sub` (the holder's `did:key`), `iat`,
`vct`, `jti`, and **`cnf`** — the record of which key the credential is bound to.

| Element | Spec |
| --- | --- |
| salted disclosures, `_sd`, `_sd_alg`, tilde concatenation | SD-JWT core (`draft-ietf-oauth-selective-disclosure-jwt`) |
| `vct`, and its meaning as a credential | SD-JWT VC (`draft-ietf-oauth-sd-jwt-vc`) |
| `cnf` | RFC 7800 (Proof-of-Possession Key Semantics) |
| `iss`, `sub`, `iat`, `jti` | RFC 7519 (JWT) |

---

## 4. Verification, step by step

### Step 1 — the verifier creates a request

**Calls:** `POST /vp/request` with `signed: true` and a DCQL query.

**Gets back:** `transaction_id`, `request_uri`, and a QR payload —

```
openid4vp://?client_id=did:web:98.70.36.106.sslip.io:f2572a18-…
           &request_uri=https://98.70.36.106.sslip.io/vp/request-object/<txn>
```

Server-side (`oid4vp.service.ts:145-170`) three values are generated and two
store entries written:

| Entry | Purpose |
| --- | --- |
| `oid4vp:txn:<transaction_id>` | the transaction: query, `nonce`, `state`, `status: pending` |
| `oid4vp:state:<state>` → `{ id }` | a reverse index, so the wallet's response can find the transaction |

### Step 2 — the wallet fetches and verifies the request

**Calls:** `GET /vp/request-object/<txn>`

**Gets back** a **signed JWT** (`typ: oauth-authz-req+jwt`,
`kid: <verifierDid>#key-0`) whose payload is:

| Claim | Value |
| --- | --- |
| `client_id`, `iss` | `did:web:98.70.36.106.sslip.io:f2572a18-…` — who is asking |
| `aud` | `https://self-issued.me/v2` |
| `response_type` | `vp_token` |
| `response_mode` | `direct_post` — post the answer straight back |
| `response_uri` | `https://…/vp/response` |
| `nonce` | random 24 bytes — freshness |
| `state` | random 16 bytes — correlation |
| `dcql_query` | which attributes are being requested |
| `exp` | now + 300 s |

The wallet **verifies that signature** by resolving the verifier's `did:web`
(a `GET /<verifier-uuid>/did.json` appears in the access log at this point). This
is what prevents anyone forging a request in the verifier's name, and it is why
the verifier DID must be resolvable by a wallet.

> **`signed: true` is not optional in practice.** A Credo-based wallet requests
> this object as `application/oauth-authz-req+jwt`; an unsigned (JSON) request
> is answered with **406** and the wallet reports a generic failure.

### Step 3 — the holder approves, and the wallet posts the answer

**Calls:**

```
POST /vp/response
{ "vp_token": "<SD-JWT + selected disclosures + Key Binding JWT>",
  "state": "<the state value>" }
```

**This POST is how the verifier learns of the approval.** Nothing travels back
through the QR, and the server never polls the wallet. `response_mode:
direct_post` is the instruction that makes the wallet do this.

Server-side (`oid4vp.service.ts:188-197` onward):

1. read `state` from the body
2. look up `oid4vp:state:<state>` → the transaction id
3. load the transaction and **require `status === 'pending'`** — this is what
   rejects replays and double submissions
4. run the checks: issuer signature, the holder's Key Binding JWT, `nonce` match,
   `aud`/`client_id` match, revocation, and DCQL satisfaction
5. store `status: 'verified'` (or `'failed'`) together with the disclosed claims

### Step 4 — the verifier UI notices

**Calls:** `GET /vp/status/<transaction_id>`, every 1.5 s.

This reads **the verifier's own stored result** — it is not asking the wallet
anything. Hence the only possible states are `pending`, `verified` and `failed`.

> **Spec:** OpenID4VP 1.0 — `vp_token`, `response_mode: direct_post`, the
> response endpoint. **DCQL** (Digital Credentials Query Language) is OpenID4VP
> 1.0 (`dcql.service.ts:18`). The signed request object is **RFC 9101** (JAR).
> The Key Binding JWT is SD-JWT core.
>
> **Naming trap:** the credential format is `vc+sd-jwt` in **OpenID4VCI**, but
> the same format is spelled `dc+sd-jwt` in **OpenID4VP's DCQL**. It was renamed
> mid-draft and wallets enforce each strictly in its own context.

---

## 5. The three random values, and what each defends

Easy to conflate, so worth stating separately:

| Value | Defends against |
| --- | --- |
| **`nonce`** | replay. The wallet signs it into the proof (issuance) or the Key Binding JWT (presentation), so an old artefact cannot be reused |
| **`state`** | mis-association. It tells the server which transaction an incoming response belongs to. No security claim of its own |
| **`client_id` / `aud`** | cross-verifier replay. A presentation obtained by a different verifier cannot be posted to this one, because the wallet signs in the audience that asked |

A practical consequence: because the wallet's response is an **outbound POST** to
the verifier, a verifier on `localhost` can never work with a real phone. The
public HTTPS host and a publicly trusted certificate are prerequisites.

---

## 6. Timeouts

| Value | Lifetime | Symptom when exceeded |
| --- | --- | --- |
| Offer / pre-authorized code | **600 s**, single use | `invalid_grant: bad or used code` |
| Access token | **300 s** | `401` at the credential endpoint |
| `c_nonce` | **300 s**, single use | `invalid_or_missing_proof` |
| VP transaction (and its `state` index) | **300 s** | `VP transaction not found` |

All four are set in `src/config/configuration.ts` and overridable by environment
variable (`OFFER_TTL`, `ACCESS_TOKEN_TTL`, `NONCE_TTL`, `VP_TXN_TTL`).

---

## 7. Draft-13 vs final-1.0 differences

`DRAFT13_COMPAT_MODE=true` switches these. Each one is a silent failure in the
wrong combination, so they are the first thing to check when a credential works
in one wallet and not another.

| Where | Draft-13 | Final 1.0 (default here) |
| --- | --- | --- |
| Issuer metadata catalogue | `credentials_supported` | `credential_configurations_supported` |
| Credential offer | `credentials: [id]` | `credential_configuration_ids: [id]` |
| PIN in the offer grant | `user_pin_required: true` | `tx_code: { input_mode, length }` |
| Credential response | `credential: "<string>"` | `credentials: [ { credential } ]` |
| `c_nonce` source | token response | dedicated Nonce Endpoint |

Two of these are handled by returning **both** shapes rather than switching:
the credential response, and `c_nonce` (present in the token response *and*
`nonce_endpoint` advertised).

---

## 8. How to debug this

A wallet shows nearly the same unhelpful message regardless of cause. Do not
start from the phone — **start from the status code of
`POST /oid4vc/credential`** (issuance) or `POST /vp/response` (presentation):

- **4xx or 5xx** → the fault is server-side. The service log names it.
- **200, but the wallet still errors** → a valid credential was issued and the
  *wallet* rejected it during its own verification. Usually key binding or
  credential shape.

The access log is the fastest way to see how far a wallet got:

```bash
docker logs sunbird-rc-oid4vc-nginx-1 2>&1 | grep okhttp | tail -20
```

A complete, successful issuance looks like this (`okhttp` is the Android wallet):

```
GET  /oid4vc/offer/<id>                     200
GET  /.well-known/openid-credential-issuer  200
GET  /.well-known/oauth-authorization-server 404   ← expected, wallet falls back
GET  /.well-known/openid-configuration      200
POST /oid4vc/token                          200
POST /oid4vc/credential                     200
GET  /<issuer-uuid>/did.json                200   ← wallet verifying the issuer
GET  /vct/<slug>                            200   ← wallet fetching display data
```

### Failure modes seen in practice, mapped to steps

| Symptom | Step | Cause |
| --- | --- | --- |
| offer appears to contain no credentials | 2 | `credential_definition` wrongly present on an SD-JWT configuration, so a strict wallet discarded the whole entry |
| `invalid_grant: bad or used code` | 4 | offer expired or already redeemed — the same opaque error covers both |
| `invalid_proof: … Error resolving DID` | 6 | `did:key` in the proof's `kid` sent to a registry that only resolves `did:web` |
| `Missing kmsKeyId for jwk with thumbprint …` | 7 | `cnf.jwk` emitted for a DID-bound request; a DID-bound wallet expects `cnf.kid` |
| `JWT nbf and vc.issuanceDate do not match` | 7 | millisecond-precision `issuanceDate` against an integer-seconds `nbf` (the `jwt_vc_json` path) |
| `SD-JWT+KB presentation invalid` | §4 step 3 | verifier could not resolve `cnf.kid` to a key when checking the Key Binding JWT |
| `406 invalid_request_uri` | §4 step 2 | request created unsigned; the wallet asked for a signed JAR |

Every one of these produced a near-identical message in the wallet UI. The split
along the 4xx/200 line held in all cases.
