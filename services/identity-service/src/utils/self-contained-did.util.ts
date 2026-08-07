import * as crypto from 'crypto';
import * as jose from 'jose';

// `did:key` and `did:jwk` encode the public key in the identifier itself, so
// they resolve offline. This registry only knows its own DB plus did:web (see
// did.service.ts resolveDID — any other method 404s), so a wallet that binds a
// credential to one of these DIDs cannot be verified without resolving locally.
//
// Needed by verifySdJwt: an SD-JWT VC bound by reference carries
// `cnf.kid` (a DID URL) rather than `cnf.jwk`, and the Key Binding JWT can only
// be checked once that DID URL is turned back into a public key.
//
// base58 is decoded here rather than via bs58 to avoid adding a dependency to
// this service (its lockfile pins a git dependency that cannot be re-resolved
// offline). The alphabet and algorithm are the standard base58-btc ones.
const BASE58_ALPHABET = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';

function base58Decode(input: string): Buffer {
  const bytes: number[] = [0];
  for (const char of input) {
    const value = BASE58_ALPHABET.indexOf(char);
    if (value === -1) throw new Error(`invalid base58 character '${char}'`);
    let carry = value;
    for (let i = 0; i < bytes.length; i++) {
      carry += bytes[i] * 58;
      bytes[i] = carry & 0xff;
      carry >>= 8;
    }
    while (carry > 0) {
      bytes.push(carry & 0xff);
      carry >>= 8;
    }
  }
  // Each leading '1' is a leading zero byte.
  for (let k = 0; k < input.length && input[k] === '1'; k++) bytes.push(0);
  return Buffer.from(bytes.reverse());
}

// multicodec varint prefixes — https://github.com/multiformats/multicodec
const ED25519_PUB = Buffer.from([0xed, 0x01]);
const X25519_PUB = Buffer.from([0xec, 0x01]);
const SECP256K1_PUB = Buffer.from([0xe7, 0x01]);
const P256_PUB = Buffer.from([0x80, 0x24]);
const P384_PUB = Buffer.from([0x81, 0x24]);
const P521_PUB = Buffer.from([0x82, 0x24]);

const ID_EC_PUBLIC_KEY = Buffer.from('06072a8648ce3d0201', 'hex'); // 1.2.840.10045.2.1
const CURVE_OIDS: Record<string, Buffer> = {
  'P-256': Buffer.from('06082a8648ce3d030107', 'hex'), // 1.2.840.10045.3.1.7
  'P-384': Buffer.from('06052b81040022', 'hex'), // 1.3.132.0.34
  'P-521': Buffer.from('06052b81040023', 'hex'), // 1.3.132.0.35
  secp256k1: Buffer.from('06052b8104000a', 'hex'), // 1.3.132.0.10
};

// Minimal DER TLV; every structure built here is well under 128 bytes, so the
// short-form length byte always applies.
function derTlv(tag: number, content: Buffer): Buffer {
  if (content.length > 127) throw new Error('DER short-form length overflow');
  return Buffer.concat([Buffer.from([tag, content.length]), content]);
}

// Wraps a SEC1 point (compressed, as did:key uses) in a SubjectPublicKeyInfo and
// lets OpenSSL do the point decompression.
function ecPointToJwk(curve: string, point: Buffer): jose.JWK {
  const curveOid = CURVE_OIDS[curve];
  if (!curveOid) throw new Error(`unsupported did:key curve '${curve}'`);
  const spki = derTlv(
    0x30,
    Buffer.concat([
      derTlv(0x30, Buffer.concat([ID_EC_PUBLIC_KEY, curveOid])),
      derTlv(0x03, Buffer.concat([Buffer.from([0x00]), point])),
    ]),
  );
  return crypto
    .createPublicKey({ key: spki, format: 'der', type: 'spki' })
    .export({ format: 'jwk' }) as jose.JWK;
}

function resolveDidKey(did: string): jose.JWK {
  const multibase = did.slice('did:key:'.length).split('#')[0];
  if (!multibase.startsWith('z')) {
    throw new Error(`unsupported did:key multibase encoding '${multibase[0]}'`);
  }
  let decoded: Buffer;
  try {
    decoded = base58Decode(multibase.slice(1));
  } catch {
    throw new Error('malformed did:key base58 payload');
  }
  const prefix = decoded.subarray(0, 2);
  const key = decoded.subarray(2);

  if (prefix.equals(ED25519_PUB)) return { kty: 'OKP', crv: 'Ed25519', x: key.toString('base64url') };
  if (prefix.equals(X25519_PUB)) return { kty: 'OKP', crv: 'X25519', x: key.toString('base64url') };
  if (prefix.equals(P256_PUB)) return ecPointToJwk('P-256', key);
  if (prefix.equals(P384_PUB)) return ecPointToJwk('P-384', key);
  if (prefix.equals(P521_PUB)) return ecPointToJwk('P-521', key);
  if (prefix.equals(SECP256K1_PUB)) return ecPointToJwk('secp256k1', key);
  throw new Error(`unsupported did:key multicodec 0x${prefix.toString('hex')}`);
}

function resolveDidJwk(did: string): jose.JWK {
  try {
    return JSON.parse(
      Buffer.from(did.slice('did:jwk:'.length).split('#')[0], 'base64url').toString('utf8'),
    );
  } catch {
    throw new Error('malformed did:jwk');
  }
}

// Resolves a self-contained DID (or DID URL) to its public JWK. Returns
// undefined for methods that genuinely need the registry, so callers keep
// delegating those to did.service.
export function resolveSelfContainedDidToJwk(did?: string): jose.JWK | undefined {
  if (!did) return undefined;
  if (did.startsWith('did:jwk:')) return resolveDidJwk(did);
  if (did.startsWith('did:key:')) return resolveDidKey(did);
  return undefined;
}
