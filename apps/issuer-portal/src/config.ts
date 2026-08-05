// Runtime configuration. Everything the browser needs is served by the BFF on
// the same origin, so there is no base-URL knob here and no CORS to negotiate —
// deliberately unlike verifier-app, which is a pure static bundle talking to the
// gateway directly.

/** Poll interval while waiting for a wallet to pick up an offer. */
export const POLL_INTERVAL_MS = 2000

/** Mirrors OFFER_TTL on oid4vc-service (default 300s) so the QR self-expires. */
export const OFFER_TTL_SECONDS = 300

/** Page size for the holder list. */
export const PAGE_SIZE = 20


/**
 * Which credential attribute carries the farmer's own identifier. Used to
 * resolve claims from the registry record and to spot the one attribute that
 * must never be hand-edited.
 */
export const FARMER_ID_ATTRIBUTE = 'farmerId'
