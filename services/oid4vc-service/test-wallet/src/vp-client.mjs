// OID4VP holder side: take an `openid4vp://` deep link, fetch the request
// object, pick a stored credential that satisfies the DCQL query, build the
// vp_token (JWT-VP for ldp_vc/jwt_vc_json/vc+sd-jwt; a CBOR DeviceResponse for
// mso_mdoc) and submit it via direct_post.
import * as jose from 'jose';
import { getJSON, postJSON, randomB64url, b64urlToString } from './util.mjs';
import { buildVpJwt } from './wallet-core.mjs';
import { buildMdocDeviceResponse } from './mdoc.mjs';
import { isTrustedVerifier } from './trust.mjs';

// SD-JWT selective disclosure: a stored sd-jwt is `<jws>~<d1>~…~<dn>~` where
// each disclosure is base64url(JSON [salt, claimName, value]). To present only
// the claims the verifier asked for (e.g. just `over_18`, keeping DOB private),
// keep the JWS untouched but drop every disclosure whose claimName isn't in the
// DCQL query's requested paths, then re-join. The service verifies only the
// disclosures we send (each against the `_sd` digest set), so omitted claims
// never reach the verifier. Returns the original string for non-sd-jwt inputs.
export function selectivelyDisclose(rawSdJwt, requestedClaimNames) {
  if (typeof rawSdJwt !== 'string' || !rawSdJwt.includes('~')) return rawSdJwt;
  const parts = rawSdJwt.split('~');
  const jws = parts[0];
  const wanted = new Set(requestedClaimNames || []);
  const kept = parts.slice(1).filter((d) => {
    if (!d) return false; // trailing empty segment
    try {
      const [, name] = JSON.parse(b64urlToString(d));
      return wanted.has(name);
    } catch {
      return false;
    }
  });
  // Preserve the trailing `~` terminator (no KB-JWT in this wallet).
  return [jws, ...kept, ''].join('~');
}

// did:jwk is deterministic by spec: the DID Document is just the
// base64url-decoded public JWK embedded in the identifier itself. Mirrors
// the same local-first fallback used on the issuance side (wallet-core.mjs's
// own did:jwk, and oid4vc-service's pop.service.ts / oid4vp.service.ts).
function resolveDidJwk(did) {
  try {
    return JSON.parse(b64urlToString(did.slice('did:jwk:'.length)));
  } catch {
    throw new Error('malformed did:jwk verifier client_id');
  }
}

// did:web resolution per the did:web method spec: `did:web:example.com` ->
// `https://example.com/.well-known/did.json`; `did:web:example.com:a:b` ->
// `https://example.com/a/b/did.json`. No dependency on identity-service —
// this is the wallet resolving a THIRD PARTY verifier's DID, not its own.
async function resolveDidWeb(did) {
  const rest = did.slice('did:web:'.length);
  const segments = rest.split(':').map(decodeURIComponent);
  const domain = segments.shift();
  const path = segments.length ? `/${segments.join('/')}/did.json` : '/.well-known/did.json';
  return await getJSON(`https://${domain}${path}`, 'resolve did:web verifier');
}

async function resolveVerifierKey(clientId, kid) {
  if (clientId.startsWith('did:jwk:')) return resolveDidJwk(clientId);
  if (clientId.startsWith('did:web:')) {
    const doc = await resolveDidWeb(clientId);
    const vm = (doc.verificationMethod || []).find(
      (m) => (kid ? m.id === kid : true) && m.publicKeyJwk,
    );
    if (!vm) throw new Error('verifier key not resolvable from did:web document');
    return vm.publicKeyJwk;
  }
  // Same posture walt.id took against this service's own did: client_id
  // during interop testing (see oid4vp.service.ts) — an unrecognized prefix
  // is refused rather than silently trusted.
  throw new Error(`unsupported client_id prefix for a signed request: ${clientId}`);
}

// Fetches the request object and, if it's a signed JAR (draft-23,
// application/oauth-authz-req+jwt), verifies it against a key resolved from
// the client_id's DID before returning the payload. Unsigned requests
// (draft-23 `redirect_uri:` prefix, or the legacy client_id_scheme shape)
// are returned as-is with `verified: false` — there is nothing to verify.
export async function resolveRequest(vpLink) {
  const q = vpLink.substring(vpLink.indexOf('?') + 1);
  const params = new URLSearchParams(q);
  const requestUri = params.get('request_uri');
  if (!requestUri) throw new Error('vp link missing request_uri');

  const res = await fetch(requestUri, {
    headers: { accept: 'application/oauth-authz-req+jwt, application/json' },
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`fetch request object -> HTTP ${res.status}: ${text}`);
  const contentType = res.headers.get('content-type') || '';
  const looksLikeJws = text.split('.').length === 3 && !contentType.includes('json');

  if (contentType.includes('oauth-authz-req+jwt') || looksLikeJws) {
    const jws = text.trim();
    const header = jose.decodeProtectedHeader(jws);
    const payload = jose.decodeJwt(jws);
    if (!payload.client_id?.startsWith('did:')) {
      throw new Error(`signed request object has non-DID client_id: ${payload.client_id}`);
    }
    if (payload.iss !== payload.client_id) {
      throw new Error('signed request object: iss does not match client_id');
    }
    const publicJwk = await resolveVerifierKey(payload.client_id, header.kid);
    const key = await jose.importJWK(publicJwk, header.alg || 'ES256');
    await jose.compactVerify(jws, key);
    return { requestObject: payload, verified: true, signed: true };
  }

  const requestObject = JSON.parse(text);
  return { requestObject, verified: false, signed: false };
}

// Presents from a stored credential. `store` is a CredentialStore.
export async function present(vpLink, holder, store) {
  const { requestObject: reqObj, verified, signed } = await resolveRequest(vpLink);
  if (!isTrustedVerifier(reqObj, { verified, signed })) {
    throw new Error(`verifier not trusted: ${reqObj.client_id}`);
  }
  const cq = (reqObj.dcql_query?.credentials || [])[0];
  if (!cq) throw new Error('request has no DCQL credential query');

  const cred = store.findForQuery(cq);
  if (!cred) throw new Error(`no stored credential satisfies query (format=${cq.format})`);

  // Per OID4VP §Response Parameters, `vp_token` is a JSON object keyed by the
  // DCQL credential query `id`, each value an array of Presentations — not a
  // bare JWT/DeviceResponse. Found live: walt.id sends exactly this shape;
  // this wallet previously sent a bare vp_token, which is what the server
  // side had (incorrectly) come to expect too.
  let submission;
  if (cq.format === 'mso_mdoc') {
    const mdocGeneratedNonce = await randomB64url();
    const fields = (cq.claims || []).map((c) => ({
      namespace: c.path[0],
      element: c.path[1],
    }));
    const vpToken = await buildMdocDeviceResponse(cred.raw, holder, {
      docType: cred.docType,
      fields,
      mdocGeneratedNonce,
      clientId: reqObj.client_id,
      responseUri: reqObj.response_uri,
      verifierNonce: reqObj.nonce,
    });
    submission = {
      state: reqObj.state,
      vp_token: { [cq.id]: [vpToken] },
      mdoc_generated_nonce: mdocGeneratedNonce,
    };
  } else if (cq.format === 'vc+sd-jwt' || cq.format === 'dc+sd-jwt') {
    // The Presentation *is* the SD-JWT compact string directly — no outer
    // VP-JWT wrapper. Present ONLY the disclosures the verifier requested.
    // NOTE: this wallet's stored SD-JWTs have no Key Binding JWT (see
    // selectivelyDisclose's comment), so this presentation carries no
    // cryptographic holder-binding/replay proof — a pre-existing limitation
    // of this test wallet, not something real wallets (e.g. walt.id) do.
    const requested = (cq.claims || [])
      .map((c) => (c.path || [])[c.path.length - 1])
      .filter(Boolean);
    const presentedRaw = selectivelyDisclose(cred.raw, requested);
    submission = { state: reqObj.state, vp_token: { [cq.id]: [presentedRaw] } };
  } else {
    // jwt_vc_json / ldp_vc: the Presentation is a Verifiable Presentation
    // wrapping the embedded credential, carrying its own nonce/aud.
    const vpJwt = await buildVpJwt(holder, {
      audience: reqObj.client_id,
      nonce: reqObj.nonce,
      verifiableCredential: [cred.raw],
    });
    submission = { state: reqObj.state, vp_token: { [cq.id]: [vpJwt] } };
  }

  const res = await postJSON(reqObj.response_uri, submission, {}, 'submit vp');
  return { submission: res, presented: cred, verified, signed, clientId: reqObj.client_id };
}
