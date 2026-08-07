import * as crypto from 'crypto';
import * as bs58 from 'bs58';
import * as jose from 'jose';

// `did:key` and `did:jwk` are *self-contained*: the public key is encoded in the
// identifier itself, so the DID Document is derivable offline and there is
// nothing to look up. identity-service's registry only knows its own DB plus
// did:web (see did.service.ts resolveDID — any other method 404s), so DIDs of
// these methods must be resolved locally or every standards-first wallet that
// binds with them fails with `Error resolving DID`.
//
// Standards-compliant wallets bind with did:key whenever the issuer
// advertises it in `cryptographic_binding_methods_supported`, and W3C
// `jwt_vc_json` forbids raw-JWK binding, so did:key support is mandatory to
// interoperate with them at all.

// multicodec varint prefixes for public key types — https://github.com/multiformats/multicodec
// (ed25519-pub 0xed, secp256k1-pub 0xe7, p256-pub 0x1200, p384-pub 0x1201, p521-pub 0x1202)
const ED25519_PUB = Buffer.from([0xed, 0x01]);
const X25519_PUB = Buffer.from([0xec, 0x01]);
const SECP256K1_PUB = Buffer.from([0xe7, 0x01]);
const P256_PUB = Buffer.from([0x80, 0x24]);
const P384_PUB = Buffer.from([0x81, 0x24]);
const P521_PUB = Buffer.from([0x82, 0x24]);

// ASN.1 OIDs, DER-encoded (tag + length + value).
const ID_EC_PUBLIC_KEY = Buffer.from('06072a8648ce3d0201', 'hex'); // 1.2.840.10045.2.1
const CURVE_OIDS: Record<string, Buffer> = {
  'P-256': Buffer.from('06082a8648ce3d030107', 'hex'), // 1.2.840.10045.3.1.7
  'P-384': Buffer.from('06052b81040022', 'hex'), // 1.3.132.0.34
  'P-521': Buffer.from('06052b81040023', 'hex'), // 1.3.132.0.35
  secp256k1: Buffer.from('06052b8104000a', 'hex'), // 1.3.132.0.10
};

// Minimal DER TLV. Every structure we build here is well under 128 bytes, so
// the short-form length byte is always valid.
function derTlv(tag: number, content: Buffer): Buffer {
  if (content.length > 127) {
    throw new Error('DER short-form length overflow');
  }
  return Buffer.concat([Buffer.from([tag, content.length]), content]);
}

// Converts a SEC1 elliptic-curve point (compressed *or* uncompressed, as
// did:key uses compressed form) into a JWK, by wrapping it in a
// SubjectPublicKeyInfo and letting Node/OpenSSL do the point decompression.
function ecPointToJwk(curve: string, point: Buffer): jose.JWK {
  const curveOid = CURVE_OIDS[curve];
  if (!curveOid) {
    throw new Error(`unsupported did:key curve '${curve}'`);
  }

  const spki = derTlv(
    0x30,
    Buffer.concat([
      derTlv(0x30, Buffer.concat([ID_EC_PUBLIC_KEY, curveOid])),
      // BIT STRING: leading 0x00 = "no unused bits in the final octet".
      derTlv(0x03, Buffer.concat([Buffer.from([0x00]), point])),
    ]),
  );

  return crypto
    .createPublicKey({ key: spki, format: 'der', type: 'spki' })
    .export({ format: 'jwk' }) as jose.JWK;
}

// did:jwk — the method-specific id is the base64url-encoded JWK itself.
function resolveDidJwk(did: string): jose.JWK {
  try {
    return JSON.parse(
      Buffer.from(did.slice('did:jwk:'.length), 'base64url').toString('utf8'),
    );
  } catch {
    throw new Error('malformed did:jwk');
  }
}

// did:key — the method-specific id is a multibase (base58-btc, 'z') encoded
// multicodec-prefixed public key.
function resolveDidKey(did: string): jose.JWK {
  const multibase = did.slice('did:key:'.length).split('#')[0];
  if (!multibase.startsWith('z')) {
    throw new Error(`unsupported did:key multibase encoding '${multibase[0]}'`);
  }

  let decoded: Buffer;
  try {
    decoded = Buffer.from(bs58.decode(multibase.slice(1)));
  } catch {
    throw new Error('malformed did:key base58 payload');
  }

  const prefix = decoded.subarray(0, 2);
  const key = decoded.subarray(2);

  if (prefix.equals(ED25519_PUB)) {
    return { kty: 'OKP', crv: 'Ed25519', x: key.toString('base64url') };
  }
  if (prefix.equals(X25519_PUB)) {
    return { kty: 'OKP', crv: 'X25519', x: key.toString('base64url') };
  }
  if (prefix.equals(P256_PUB)) return ecPointToJwk('P-256', key);
  if (prefix.equals(P384_PUB)) return ecPointToJwk('P-384', key);
  if (prefix.equals(P521_PUB)) return ecPointToJwk('P-521', key);
  if (prefix.equals(SECP256K1_PUB)) return ecPointToJwk('secp256k1', key);

  throw new Error(`unsupported did:key multicodec 0x${prefix.toString('hex')}`);
}

export function isSelfContainedDid(did?: string): boolean {
  return !!did && (did.startsWith('did:key:') || did.startsWith('did:jwk:'));
}

// Resolves a self-contained DID to its public JWK. Returns undefined for DID
// methods that genuinely need the registry, so callers keep delegating those to
// identity-service.
export function resolveSelfContainedDidToJwk(did?: string): jose.JWK | undefined {
  if (!did) return undefined;
  if (did.startsWith('did:jwk:')) return resolveDidJwk(did);
  if (did.startsWith('did:key:')) return resolveDidKey(did);
  return undefined;
}

// Structural equality of two JWKs' public key material (ignores metadata like
// `kid`/`use`/`alg`). Used to confirm a self-asserted inline `jwk` header
// actually matches what a self-contained DID (did:key/did:jwk) in `kid`/`iss`
// resolves to, rather than trusting the header and the DID independently.
export function jwkPublicKeyEquals(a?: jose.JWK, b?: jose.JWK): boolean {
  if (!a || !b) return false;
  return (['kty', 'crv', 'x', 'y', 'n', 'e'] as const).every((f) => a[f] === b[f]);
}
