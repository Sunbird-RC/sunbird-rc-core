import * as crypto from 'crypto';

// `did:jwk` identifiers are self-contained: the public key JWK is the
// base64url payload of the identifier itself, so resolving one is local
// decoding, not a network lookup.
//
// ponytail: did:jwk only — did:key (multicodec/multibase) decoding lives in
// oid4vc-service's version of this file; port it here too if needed.

export function isSelfContainedDid(did?: string): boolean {
  return !!did && did.startsWith('did:jwk:');
}

export function resolveSelfContainedDidToJwk(did?: string): crypto.JsonWebKey | undefined {
  if (!did || !did.startsWith('did:jwk:')) return undefined;
  try {
    return JSON.parse(
      Buffer.from(did.slice('did:jwk:'.length).split('#')[0], 'base64url').toString('utf8'),
    );
  } catch {
    throw new Error('malformed did:jwk');
  }
}