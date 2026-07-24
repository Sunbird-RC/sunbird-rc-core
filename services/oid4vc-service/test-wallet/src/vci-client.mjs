// OID4VCI holder side: take a credential-offer deep link (as a wallet would
// after scanning the QR), dereference it, run the pre-authorized_code grant,
// prove possession of the holder key, and receive the credential.
import { getJSON, postJSON, b64urlToString, PREAUTH_GRANT } from './util.mjs';
import { buildPopJwt } from './wallet-core.mjs';
import { inspectMdoc } from './mdoc.mjs';

// Parse an `openid-credential-offer://` deep link into its offer object.
// Supports both by-reference (credential_offer_uri) and by-value
// (credential_offer) forms.
export async function resolveOffer(offerLink) {
  const q = offerLink.substring(offerLink.indexOf('?') + 1);
  const params = new URLSearchParams(q);
  const byRef = params.get('credential_offer_uri');
  if (byRef) return await getJSON(byRef, 'dereference offer');
  const byVal = params.get('credential_offer');
  if (byVal) return JSON.parse(decodeURIComponent(byVal));
  throw new Error('offer link has neither credential_offer_uri nor credential_offer');
}

// Full issuance flow. Returns a store-ready credential record.
export async function acceptOffer(offerLink, holder, { txCode = '000000' } = {}) {
  const offer = await resolveOffer(offerLink);
  const issuer = offer.credential_issuer;
  const configId = (offer.credential_configuration_ids || offer.credentials || [])[0];
  const grant = (offer.grants || {})[PREAUTH_GRANT];
  if (!grant) throw new Error('offer does not contain a pre-authorized_code grant');
  const preAuthCode = grant['pre-authorized_code'];
  const txRequired = !!grant.tx_code || !!grant.user_pin_required;

  // 1. Token: exchange the pre-authorized code.
  const tokenReq = { grant_type: PREAUTH_GRANT, 'pre-authorized_code': preAuthCode };
  if (txRequired) tokenReq.tx_code = txCode;
  const token = await postJSON(`${issuer}/oid4vc/token`, tokenReq, {}, 'token');

  // 2. Proof of possession over the returned c_nonce.
  const popJwt = await buildPopJwt(holder, { audience: issuer, nonce: token.c_nonce });

  // 3. Credential request.
  const credRes = await postJSON(
    `${issuer}/oid4vc/credential`,
    { proof: { proof_type: 'jwt', jwt: popJwt } },
    { authorization: `Bearer ${token.access_token}` },
    'credential',
  );

  const record = {
    format: credRes.format,
    raw: credRes.credential,
    configId,
    issuer,
    label: configId,
  };

  // Enrich for display + DCQL selection.
  if (credRes.format === 'mso_mdoc') {
    const parsed = await inspectMdoc(credRes.credential);
    record.docType = parsed.docType;
    record.claims = parsed.claims;
  } else if (credRes.format === 'vc+sd-jwt') {
    record.vct = sdJwtVct(credRes.credential);
    record.claims = sdJwtClaims(credRes.credential);
  } else if (credRes.format === 'jwt_vc_json') {
    record.claims = jwtVcSubject(credRes.credential);
  } else {
    // ldp_vc object
    record.claims = credRes.credential.credentialSubject || {};
  }
  return record;
}

function decodeJwtPayload(jwt) {
  try {
    const [, p] = jwt.split('.');
    return JSON.parse(b64urlToString(p));
  } catch {
    return {};
  }
}

function jwtVcSubject(jwt) {
  const c = decodeJwtPayload(jwt);
  return (c.vc && c.vc.credentialSubject) || {};
}

function sdJwtVct(sdjwt) {
  return decodeJwtPayload(sdjwt.split('~')[0]).vct;
}

function sdJwtClaims(sdjwt) {
  const parts = sdjwt.split('~');
  const claims = { ...decodeJwtPayload(parts[0]) };
  delete claims._sd;
  delete claims._sd_alg;
  for (const d of parts.slice(1).filter((x) => x.length)) {
    try {
      const [, name, value] = JSON.parse(b64urlToString(d));
      claims[name] = value;
    } catch {
      /* ignore malformed disclosure */
    }
  }
  return claims;
}
