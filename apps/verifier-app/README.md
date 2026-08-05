# Verifier Console (OpenID4VP)

A standalone React app that drives the verifier side of `oid4vc-service`: it
creates a presentation request, renders the QR, and shows the claims a wallet
discloses. Independent of `demo/`, which is unchanged.

    npm install
    npm run dev        # http://localhost:5173/verifier-app/?base=https://98.70.36.106.sslip.io

## Flow

1. `POST /vp/request` with `signed: true` and a DCQL query
2. `qr_data` is rendered through the deployment's own `/qr` endpoint
3. The wallet fetches the request object and POSTs the presentation to `/vp/response`
4. The app polls `GET /vp/status/<transaction_id>` until it leaves `pending`

## Configuration

All optional, via query string — one build works against any deployment:

| Param  | Default     | Purpose |
| ------ | ----------- | ------- |
| `base` | same origin | oid4vc-service base URL |
| `ttl`  | `300`       | countdown length; mirrors `VP_TXN_TTL` |

Credential types are **not** configured here. They are discovered at runtime from
`/credential-schema/oid4vci-configs`, filtered to those offering `vc+sd-jwt` with
an issuer DID this host can resolve — so a schema created after this app was built
becomes selectable on the next page load, and each type's `vct`, configuration id
and attribute list are derived from the schema itself.

## Notes that are easy to get wrong

- `signed: true` is required for Credo-based wallets (Paradym). They request the
  JAR as `application/oauth-authz-req+jwt`; an unsigned request answers **406**.
- DCQL spells SD-JWT VC **`dc+sd-jwt`** while the OID4VCI credential format is
  **`vc+sd-jwt`**. Both appear in this app and the difference is deliberate.
- `meta.vct_values` is mandatory for SD-JWT DCQL; without it the query fails
  validation (`Invalid key: Expected "meta"`).
- Only schemas authored by a resolvable `did:web` work with standards wallets.

## Deploy

Built as an nginx-served static image and mounted at `/verifier-app/` on the main
nginx. `VITE_BASE` must match that path.
