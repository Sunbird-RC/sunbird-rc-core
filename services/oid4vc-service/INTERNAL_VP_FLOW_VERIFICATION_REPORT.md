# OID4VC Internal Issue→VP Flow Verification Report

**Date:** 2026-07-23
**Deployment tested:** `http://98.70.36.106` (Azure VM, `docker-compose.cloud.yml`)
**Reason:** External wallet testing hit wallet-side limitations that block full
end-to-end verification through a real wallet UI. This report drives the entire
OID4VCI issuance → OID4VP presentation flow **directly against our own server**,
for all three supported credential formats, using a self-generated `did:jwk`
holder key to stand in for a wallet. This gives unambiguous, reproducible proof
of what the flow does end-to-end, independent of any third-party wallet.

## Summary

| Format | Offer | Token | Credential Issue | Standalone Verify | VP Submit | Final Status |
|---|---|---|---|---|---|---|
| `ldp_vc` | ✅ 201 | ✅ 200 | ✅ 200 | ✅ proof: OK | ✅ 200 | ✅ **verified: true**, all 6 checks OK |
| `jwt_vc_json` | ✅ 201 | ✅ 200 | ✅ 200 | ✅ proof: OK | ✅ 200 | ✅ **verified: true**, all 6 checks OK |
| `vc+sd-jwt` | ✅ 201 | ✅ 200 | ✅ 200 | ✅ proof: OK | ✅ 200 | ✅ **verified: true**, all 6 checks OK |

All three formats pass the complete issuance → verification → presentation →
DCQL-claim-disclosure chain.

## Methodology

For each format, a Node.js script (`jose` library) performed, against the live
deployed server, exactly what a real wallet would do:

1. Generate an ES256 `did:jwk` holder keypair.
2. `POST /oid4vc/offer` — create an offer (credential: "Age Verification
   Credential", the one schema onboarded with all three formats enabled,
   `schemaId: did:schema:c577667f-71c7-4e8d-8576-c70e6d38939a`).
3. `GET /oid4vc/offer/:id` — dereference (wallet step).
4. `POST /oid4vc/token` — exchange the pre-authorized_code.
5. Build and sign a PoP (`openid4vci-proof+jwt`) JWT with the holder key.
6. `POST /oid4vc/credential` — request and receive the signed credential.
7. `POST /credentials/verify` — independently verify the issued credential
   (standalone, no presentation context) as a sanity baseline.
8. `POST /vp/request` — create a DCQL presentation request asking for `name`
   and `age_over_18`.
9. `GET /vp/request-object/:id` — fetch the (unsigned, plain-JSON) request.
10. Build a JWT-VP wrapper (`{iss: holderDid, nonce, vp: {verifiableCredential:
    [issuedCredential]}}`), signed by the same holder key, inline `jwk` header
    (no `kid`) — matching the real-world `did:jwk` wallet shape used
    throughout this session's interop testing.
11. `POST /vp/response` — submit the presentation (`direct_post`).
12. `GET /vp/status/:id` — poll the final verifier-side result.

DCQL query shape was format-appropriate: `jwt_vc_json`/`ldp_vc` use
`meta.type_values` + `credentialSubject`-prefixed claim paths (per OID4VP DCQL
spec for W3C VC formats); `vc+sd-jwt` uses `meta.vct_values` + bare claim paths
(per spec for SD-JWT-based formats, where there is no `credentialSubject`
wrapper).

## Full results (raw)

<details>
<summary><code>ldp_vc</code> — click to expand</summary>

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
  "claims": { "age_cred": { "name": "Full Flow Test (ldp_vc)", "age_over_18": true } }
}
```
</details>

<details>
<summary><code>jwt_vc_json</code> — click to expand</summary>

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
  "claims": { "age_cred": { "name": "Full Flow Test (jwt_vc_json)", "age_over_18": true } }
}
```
</details>

<details>
<summary><code>vc+sd-jwt</code> — click to expand</summary>

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
  "claims": { "age_cred": { "name": "Full Flow Test (vc+sd-jwt)", "age_over_18": true } }
}
```
</details>

## Conclusion

All three OID4VC-supported credential formats (`ldp_vc`, `jwt_vc_json`,
`vc+sd-jwt`) complete the full issuance → verification → DCQL-gated
presentation flow correctly, end-to-end, against this deployment.
