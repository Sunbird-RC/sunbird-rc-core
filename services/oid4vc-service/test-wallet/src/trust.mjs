// Wallet-side trusted verifier allowlist. Enforced in vp-client.mjs's
// present(), BEFORE any credential is selected or a vp_token is built — the
// trust decision belongs to the wallet, since it's the one deciding whether
// to disclose data, and it's the only party positioned to check both "is
// this client_id one I recognize" and "does the response_uri behind it
// actually point where I expect", regardless of whether oid4vc-service (or
// any other verifier) enforces anything on its own side.
//
// Entry shape: { clientId, responseUriOrigin, allowUnsigned? }
//  - clientId must exactly match the request object's (already
//    signature-verified, for signed requests) client_id.
//  - responseUriOrigin is compared against new URL(reqObj.response_uri).origin,
//    so a request object that passes identity verification can't still
//    redirect the vp_token to a different endpoint.
//  - allowUnsigned: true lets this entry match a request that wasn't a
//    verified signed JAR (needed for the legacy/walt.id demo path, where
//    requests are unsigned by design). Defaults to false: an
//    unsigned/unverified request is refused unless explicitly opted in.

let allowlist = null; // null = not configured yet
let warned = false;

export function setTrustedVerifiers(list) {
  allowlist = Array.isArray(list) ? list : [];
}

// Test/dev helper — back to "not configured" so isTrustedVerifier's warn-once
// and allow-all default can be exercised again.
export function resetTrustedVerifiers() {
  allowlist = null;
  warned = false;
}

export function getTrustedVerifiers() {
  return allowlist;
}

export function isTrustedVerifier(reqObj, { verified, signed } = {}) {
  if (allowlist === null) {
    if (!warned) {
      console.warn(
        '[wallet] no trusted-verifier allowlist configured — accepting ANY verifier. ' +
          'Set WALLET_TRUSTED_VERIFIERS before using this wallet outside local development.',
      );
      warned = true;
    }
    return true;
  }

  const clientId = reqObj?.client_id;
  const responseUri = reqObj?.response_uri;
  if (!clientId || !responseUri) return false;

  const entry = allowlist.find((e) => e.clientId === clientId);
  if (!entry) return false;
  if (!verified && !entry.allowUnsigned) return false;

  let origin;
  try {
    origin = new URL(responseUri).origin;
  } catch {
    return false;
  }
  return origin === entry.responseUriOrigin;
}
