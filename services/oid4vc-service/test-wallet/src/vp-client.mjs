// OID4VP holder side: take an `openid4vp://` deep link, fetch the request
// object, pick a stored credential that satisfies the DCQL query, build the
// vp_token (JWT-VP for ldp_vc/jwt_vc_json/vc+sd-jwt; a CBOR DeviceResponse for
// mso_mdoc) and submit it via direct_post.
import { getJSON, postJSON, randomB64url, b64urlToString } from './util.mjs';
import { buildVpJwt } from './wallet-core.mjs';
import { buildMdocDeviceResponse } from './mdoc.mjs';

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

export async function resolveRequest(vpLink) {
  const q = vpLink.substring(vpLink.indexOf('?') + 1);
  const params = new URLSearchParams(q);
  const requestUri = params.get('request_uri');
  if (!requestUri) throw new Error('vp link missing request_uri');
  return await getJSON(requestUri, 'fetch request object');
}

// Presents from a stored credential. `store` is a CredentialStore.
export async function present(vpLink, holder, store) {
  const reqObj = await resolveRequest(vpLink);
  const cq = (reqObj.dcql_query?.credentials || [])[0];
  if (!cq) throw new Error('request has no DCQL credential query');

  const cred = store.findForQuery(cq);
  if (!cred) throw new Error(`no stored credential satisfies query (format=${cq.format})`);

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
    submission = { state: reqObj.state, vp_token: vpToken, mdoc_generated_nonce: mdocGeneratedNonce };
  } else {
    // For vc+sd-jwt, present ONLY the disclosures the verifier requested
    // (selective disclosure); ldp_vc / jwt_vc_json have no disclosures to
    // filter, so selectivelyDisclose returns them unchanged.
    const requested = (cq.claims || [])
      .map((c) => (c.path || [])[c.path.length - 1])
      .filter(Boolean);
    const presentedRaw =
      cq.format === 'vc+sd-jwt' ? selectivelyDisclose(cred.raw, requested) : cred.raw;
    const vpJwt = await buildVpJwt(holder, {
      audience: reqObj.client_id,
      nonce: reqObj.nonce,
      verifiableCredential: [presentedRaw],
    });
    submission = { state: reqObj.state, vp_token: vpJwt };
  }

  const res = await postJSON(reqObj.response_uri, submission, {}, 'submit vp');
  return { submission: res, presented: cred };
}
