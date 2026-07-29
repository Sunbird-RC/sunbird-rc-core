// Runtime configuration, overridable by query string so one build works against
// any deployment:  /verifier-app/?base=https://issuer.example.com
//
// There is deliberately no per-credential configuration here: credential types,
// their `vct` and their configuration ids are all discovered at runtime from
// /credential-schema/oid4vci-configs (see api.ts listCredentialTypes).
const params = new URLSearchParams(location.search)

// Same-origin by default: nginx serves this app and oid4vc-service from the
// same host, so `''` keeps API calls relative (and CORS irrelevant).
export const BASE = (params.get('base') ?? import.meta.env.VITE_BASE_URL ?? '').replace(/\/$/, '')



// VP_TXN_TTL on oid4vc-service defaults to 300s; the countdown mirrors it so an
// unscanned QR reports itself as expired instead of silently 404-ing.
export const TXN_TTL_SECONDS = Number(params.get('ttl') ?? 300)

export const POLL_INTERVAL_MS = 1500
