import * as crypto from 'crypto';
import { resolveSelfContainedDidToJwk } from './self-contained-did.util';

// Standard base58 (base58-btc) encoder, the inverse of the private decoder
// inside self-contained-did.util.ts — used only to build did:key fixtures
// from real generated keys, so these tests exercise actual decode/decompress
// logic rather than asserting against hand-copied magic strings.
const BASE58_ALPHABET = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';
function base58Encode(bytes: Buffer): string {
  let zeros = 0;
  while (zeros < bytes.length && bytes[zeros] === 0) zeros++;
  let num = BigInt(bytes.length > zeros ? '0x' + bytes.subarray(zeros).toString('hex') : '0x0');
  const zero = BigInt(0);
  const base = BigInt(58);
  let out = '';
  while (num > zero) {
    const rem = num % base;
    out = BASE58_ALPHABET[Number(rem)] + out;
    num = num / base;
  }
  return '1'.repeat(zeros) + out;
}

describe('resolveSelfContainedDidToJwk', () => {
  it('returns undefined for undefined input', () => {
    expect(resolveSelfContainedDidToJwk(undefined)).toBeUndefined();
  });

  it('returns undefined for a DID method it does not own (delegates to the registry)', () => {
    expect(resolveSelfContainedDidToJwk('did:web:example.com')).toBeUndefined();
    expect(resolveSelfContainedDidToJwk('did:example:123')).toBeUndefined();
  });

  describe('did:jwk', () => {
    it('decodes a base64url-encoded JWK', () => {
      const jwk = { kty: 'EC', crv: 'P-256', x: 'abc', y: 'def' };
      const did = `did:jwk:${Buffer.from(JSON.stringify(jwk)).toString('base64url')}`;
      expect(resolveSelfContainedDidToJwk(did)).toEqual(jwk);
    });

    it('strips a #fragment before decoding', () => {
      const jwk = { kty: 'EC', crv: 'P-256', x: 'abc', y: 'def' };
      const did = `did:jwk:${Buffer.from(JSON.stringify(jwk)).toString('base64url')}#0`;
      expect(resolveSelfContainedDidToJwk(did)).toEqual(jwk);
    });

    it('throws on a malformed did:jwk', () => {
      expect(() => resolveSelfContainedDidToJwk('did:jwk:not-valid-base64url-json')).toThrow('malformed did:jwk');
    });
  });

  describe('did:key', () => {
    it('resolves an Ed25519 multicodec to a matching JWK', () => {
      const { publicKey } = crypto.generateKeyPairSync('ed25519');
      const jwk = publicKey.export({ format: 'jwk' }) as any;
      const rawBytes = Buffer.from(jwk.x, 'base64url');
      const did = `did:key:z${base58Encode(Buffer.concat([Buffer.from([0xed, 0x01]), rawBytes]))}`;

      expect(resolveSelfContainedDidToJwk(did)).toEqual({ kty: 'OKP', crv: 'Ed25519', x: jwk.x });
    });

    it('resolves a P-256 (JsonWebKey2020/mdoc) multicodec to a matching JWK', () => {
      const { publicKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
      const jwk = publicKey.export({ format: 'jwk' }) as any;
      const xBytes = Buffer.from(jwk.x, 'base64url');
      const yBytes = Buffer.from(jwk.y, 'base64url');
      const prefixByte = yBytes[yBytes.length - 1] % 2 === 0 ? 0x02 : 0x03;
      const compressedPoint = Buffer.concat([Buffer.from([prefixByte]), xBytes]);
      const did = `did:key:z${base58Encode(Buffer.concat([Buffer.from([0x80, 0x24]), compressedPoint]))}`;

      expect(resolveSelfContainedDidToJwk(did)).toMatchObject({ kty: 'EC', crv: 'P-256', x: jwk.x, y: jwk.y });
    });

    it('strips a #fragment before decoding', () => {
      const { publicKey } = crypto.generateKeyPairSync('ed25519');
      const jwk = publicKey.export({ format: 'jwk' }) as any;
      const rawBytes = Buffer.from(jwk.x, 'base64url');
      const multibase = base58Encode(Buffer.concat([Buffer.from([0xed, 0x01]), rawBytes]));
      const did = `did:key:z${multibase}#z${multibase}`;

      expect(resolveSelfContainedDidToJwk(did)).toEqual({ kty: 'OKP', crv: 'Ed25519', x: jwk.x });
    });

    it('throws for an unsupported multicodec prefix', () => {
      const did = `did:key:z${base58Encode(Buffer.from([0x99, 0x99, 1, 2, 3]))}`;
      expect(() => resolveSelfContainedDidToJwk(did)).toThrow(/unsupported did:key multicodec/);
    });

    it('throws when the multibase encoding does not start with z', () => {
      expect(() => resolveSelfContainedDidToJwk('did:key:abc123')).toThrow(/unsupported did:key multibase encoding/);
    });

    it('throws on an invalid base58 character', () => {
      expect(() => resolveSelfContainedDidToJwk('did:key:z0IOl')).toThrow('malformed did:key base58 payload');
    });
  });
});