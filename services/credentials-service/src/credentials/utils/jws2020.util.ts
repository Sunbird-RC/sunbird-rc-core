import * as crypto from 'crypto';
import * as jsonld from 'jsonld';

export interface Jws2020VerifyResult {
  verified: boolean;
  error?: string;
}

const HASH_BY_ALG: Record<string, string> = {
  ES256: 'sha256',
  ES384: 'sha384',
  ES512: 'sha512',
  RS256: 'sha256',
  RS384: 'sha384',
  RS512: 'sha512',
  PS256: 'sha256',
  PS384: 'sha384',
  PS512: 'sha512',
};

// Verifies a JsonWebSignature2020 Linked-Data proof per
// https://w3c-ccg.github.io/lds-jws2020/: canonicalize the proof options and
// the document-minus-proof separately (URDNA2015), SHA-256 hash each,
// concatenate the two hashes, and check that against a detached JWS
// (RFC7797 b64:false — header..signature, no embedded payload) carried in
// `proof.jws`. `publicKeyJwk` is imported via Node's `crypto` module, which
// also does the actual signature check — including converting JWS's raw
// r||s ECDSA encoding via `dsaEncoding: 'ieee-p1363'` for the ES* algorithms.
export async function verifyJsonWebSignature2020(
  document: any,
  publicKeyJwk: crypto.JsonWebKey,
  expectedPurpose: string,
  documentLoader?: (url: string) => Promise<any>,
): Promise<Jws2020VerifyResult> {
  const proof = document?.proof;
  if (!proof || proof.type !== 'JsonWebSignature2020') {
    return { verified: false, error: 'not a JsonWebSignature2020 proof' };
  }
  if (proof.proofPurpose !== expectedPurpose) {
    return {
      verified: false,
      error: `expected proofPurpose '${expectedPurpose}', got '${proof.proofPurpose}'`,
    };
  }
  if (typeof proof.jws !== 'string') {
    return { verified: false, error: 'proof.jws missing' };
  }

  const jwsParts = proof.jws.split('.');
  // RFC7797 detached mode: header..signature — the empty middle segment IS
  // the point; the payload is supplied out-of-band (the hashes below).
  if (jwsParts.length !== 3 || jwsParts[1] !== '') {
    return { verified: false, error: 'proof.jws is not a detached JWS (expected header..signature)' };
  }
  const [headerB64, , signatureB64] = jwsParts;

  let header: { alg?: string; b64?: boolean; crit?: string[] };
  try {
    header = JSON.parse(Buffer.from(headerB64, 'base64url').toString('utf8'));
  } catch {
    return { verified: false, error: 'malformed JWS protected header' };
  }
  if (header.b64 !== false || !header.crit?.includes('b64')) {
    return { verified: false, error: "JWS header must set b64:false and crit:['b64'] (detached payload)" };
  }
  const alg = header.alg;
  if (!alg) return { verified: false, error: 'JWS header missing alg' };

  // "Create Verify Hash" algorithm (Linked Data Proofs 1.0 / LD-JWS suite):
  // hash the canonicalized proof-options (proof minus jws, @context defaulted
  // to the document's) and the canonicalized document-minus-proof, and
  // concatenate — that concatenation is the JWS's detached payload.
  const { jws: _jws, ...proofOptions } = proof;
  if (!proofOptions['@context']) proofOptions['@context'] = document['@context'];
  const { proof: _proof, ...docWithoutProof } = document;

  let optionsCanon: string;
  let docCanon: string;
  try {
    [optionsCanon, docCanon] = await Promise.all([
      jsonld.canonize(proofOptions, { algorithm: 'URDNA2015', documentLoader } as any),
      jsonld.canonize(docWithoutProof, { algorithm: 'URDNA2015', documentLoader } as any),
    ]);
  } catch (e) {
    return { verified: false, error: `canonicalization failed: ${e}` };
  }

  const optionsHash = crypto.createHash('sha256').update(optionsCanon, 'utf8').digest();
  const docHash = crypto.createHash('sha256').update(docCanon, 'utf8').digest();
  const verifyData = Buffer.concat([optionsHash, docHash]);
  const signingInput = Buffer.concat([Buffer.from(headerB64, 'ascii'), Buffer.from('.'), verifyData]);
  const signature = Buffer.from(signatureB64, 'base64url');

  let publicKey: crypto.KeyObject;
  try {
    publicKey = crypto.createPublicKey({ key: publicKeyJwk as any, format: 'jwk' });
  } catch (e) {
    return { verified: false, error: `invalid public key JWK: ${e}` };
  }

  try {
    const verified = verifySignature(alg, signingInput, signature, publicKey);
    return verified ? { verified: true } : { verified: false, error: 'signature verification failed' };
  } catch (e) {
    return { verified: false, error: `signature verification error: ${e}` };
  }
}

function verifySignature(alg: string, data: Buffer, signature: Buffer, key: crypto.KeyObject): boolean {
  if (alg === 'EdDSA') {
    return crypto.verify(null, data, key, signature);
  }
  const hash = HASH_BY_ALG[alg];
  if (!hash) throw new Error(`unsupported alg '${alg}'`);
  if (alg.startsWith('PS')) {
    return crypto.verify(hash, data, { key, padding: crypto.constants.RSA_PKCS1_PSS_PADDING }, signature);
  }
  if (alg.startsWith('RS')) {
    return crypto.verify(hash, data, key, signature);
  }
  if (alg.startsWith('ES')) {
    // JWS ECDSA signatures are raw fixed-length r||s (RFC7518 §3.4), not the
    // ASN.1 DER encoding Node's crypto.verify expects by default.
    return crypto.verify(hash, data, { key, dsaEncoding: 'ieee-p1363' }, signature);
  }
  throw new Error(`unsupported alg '${alg}'`);
}