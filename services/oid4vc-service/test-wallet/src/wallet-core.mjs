// The holder. Fully independent of the issuer's services: generates its own
// key, mints its own proof-of-possession and VP JWTs. This is what makes the
// interop signal genuine rather than self-referential (contrast with the
// Postman collection, where identity-service signs the holder proofs).
//
// Key choice: a single ES256 (P-256) key covers OID4VCI PoP, JWT-VP holder
// signature, AND the mdoc device key. Holder identity is a `did:jwk` — the
// oid4vc-service decodes it locally (pop.service.ts / oid4vp.service.ts), so
// no DID registration round-trip is needed.
//
// Crypto backend: @noble/curves (pure JS), NOT WebCrypto. This is deliberate —
// `crypto.subtle` is unavailable in a browser "insecure context" (plain HTTP on
// a non-localhost host, e.g. the wallet served at http://<vm-ip>/wallet/), which
// made jose's WebCrypto key generation throw and left the holder uninitialised.
// noble only needs `crypto.getRandomValues` (available even in insecure
// contexts) for key generation and is otherwise self-contained, so the exact
// same code path runs in Node, a secure browser, and an insecure-context
// browser. base64url helpers (util.mjs) are pure encoding, no crypto.
import { p256 } from '@noble/curves/p256';
import { sha256 } from '@noble/hashes/sha2';
import { b64urlFromString, bytesToB64url } from './util.mjs';

const ALG = 'ES256';

function jwksFromPriv(priv) {
  const pub = p256.getPublicKey(priv, false); // 65 bytes: 0x04 || X(32) || Y(32)
  const publicJwk = {
    kty: 'EC',
    crv: 'P-256',
    x: bytesToB64url(pub.slice(1, 33)),
    y: bytesToB64url(pub.slice(33, 65)),
  };
  // exportJWK convention: `alg` carried on the PRIVATE jwk (see mdoc.mjs — it
  // hands this straight to @auth0/mdl, which needs alg present on the key).
  const privateJwk = { ...publicJwk, d: bytesToB64url(priv), alg: ALG };
  return { publicJwk, privateJwk };
}

// Sign a compact ES256 JWS from header+payload objects and a raw P-256 key.
function es256Jws(header, payload, priv) {
  const enc = (o) => b64urlFromString(JSON.stringify(o));
  const signingInput = `${enc(header)}.${enc(payload)}`;
  const digest = sha256(new TextEncoder().encode(signingInput));
  // noble sign is deterministic (RFC 6979) + low-S — a valid JWS ES256
  // signature (64-byte R||S), accepted by the service's jose/WebCrypto verify.
  const sig = p256.sign(digest, priv).toCompactRawBytes();
  return `${signingInput}.${bytesToB64url(sig)}`;
}

export async function createHolder() {
  const priv = p256.utils.randomPrivateKey(); // uses crypto.getRandomValues
  const { publicJwk, privateJwk } = jwksFromPriv(priv);
  // did:jwk (spec): base64url(JSON of the public JWK). Computed once and reused
  // verbatim everywhere so issuance-time credentialSubject.id and
  // presentation-time holder DID are byte-identical (the holder-binding check).
  const did = `did:jwk:${b64urlFromString(JSON.stringify(publicJwk))}`;
  return { priv, publicJwk, privateJwk, did, alg: ALG };
}

// OID4VCI proof-of-possession JWT.
// header: { typ: 'openid4vci-proof+jwt', alg: ES256, jwk }
// claims: { iss(=holder did), aud(=issuer/public url), iat, nonce(=c_nonce) }
export async function buildPopJwt(holder, { audience, nonce }) {
  const header = { alg: holder.alg, typ: 'openid4vci-proof+jwt', jwk: holder.publicJwk };
  const payload = { nonce, iss: holder.did, aud: audience, iat: Math.floor(Date.now() / 1000) };
  return es256Jws(header, payload, holder.priv);
}

// OID4VP JWT-VP. The service reads header.jwk (inline holder key), verifies the
// signature, checks claims.nonce === request nonce, then pulls embedded VCs out
// of vp.verifiableCredential. `verifiableCredential` is an array of either the
// raw compact string (jwt_vc_json / vc+sd-jwt) or the JSON-LD object (ldp_vc).
export async function buildVpJwt(holder, { audience, nonce, verifiableCredential }) {
  const header = { alg: holder.alg, typ: 'JWT', jwk: holder.publicJwk };
  const payload = {
    nonce,
    iss: holder.did,
    aud: audience,
    iat: Math.floor(Date.now() / 1000),
    vp: {
      '@context': ['https://www.w3.org/2018/credentials/v1'],
      type: ['VerifiablePresentation'],
      holder: holder.did,
      verifiableCredential,
    },
  };
  return es256Jws(header, payload, holder.priv);
}
