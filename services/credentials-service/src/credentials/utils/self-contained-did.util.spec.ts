import { isSelfContainedDid, resolveSelfContainedDidToJwk } from './self-contained-did.util';

describe('isSelfContainedDid', () => {
  it('returns true for a did:jwk identifier', () => {
    expect(isSelfContainedDid('did:jwk:abc123')).toBe(true);
  });

  it('returns false for did:key', () => {
    expect(isSelfContainedDid('did:key:z6Mk')).toBe(false);
  });

  it('returns false for did:web', () => {
    expect(isSelfContainedDid('did:web:example.com')).toBe(false);
  });

  it('returns false for undefined', () => {
    expect(isSelfContainedDid(undefined)).toBe(false);
  });

  it('returns false for empty string', () => {
    expect(isSelfContainedDid('')).toBe(false);
  });

  it('returns false for a random string', () => {
    expect(isSelfContainedDid('not-a-did')).toBe(false);
  });
});

describe('resolveSelfContainedDidToJwk', () => {
  it('resolves a valid did:jwk to a JWK object', () => {
    const jwk = { kty: 'EC', crv: 'P-256', x: 'abc', y: 'def' };
    const encoded = Buffer.from(JSON.stringify(jwk)).toString('base64url');
    const did = `did:jwk:${encoded}`;

    const result = resolveSelfContainedDidToJwk(did);
    expect(result).toEqual(jwk);
  });

  it('returns undefined for undefined input', () => {
    expect(resolveSelfContainedDidToJwk(undefined)).toBeUndefined();
  });

  it('returns undefined for non-did:jwk prefix', () => {
    expect(resolveSelfContainedDidToJwk('did:key:abc')).toBeUndefined();
  });

  it('strips fragment identifier before decoding', () => {
    const jwk = { kty: 'RSA', n: 'abc', e: 'AQAB' };
    const encoded = Buffer.from(JSON.stringify(jwk)).toString('base64url');
    const did = `did:jwk:${encoded}#key-1`;

    const result = resolveSelfContainedDidToJwk(did);
    expect(result).toEqual(jwk);
  });

  it('throws on malformed base64url payload', () => {
    expect(() => resolveSelfContainedDidToJwk('did:jwk:!!!invalid-base64!!!')).toThrow('malformed did:jwk');
  });

  it('throws when decoded payload is not valid JSON', () => {
    const notJson = Buffer.from('this is not json').toString('base64url');
    expect(() => resolveSelfContainedDidToJwk(`did:jwk:${notJson}`)).toThrow('malformed did:jwk');
  });

  it('round-trips a real Ed25519 JWK', () => {
    const jwk = {
      kty: 'OKP',
      crv: 'Ed25519',
      x: '11qYAYKxCrfVS/7TyWQHOg7hcvPapiMLRwIofBPuMk',
    };
    const encoded = Buffer.from(JSON.stringify(jwk)).toString('base64url');
    const did = `did:jwk:${encoded}`;

    const result = resolveSelfContainedDidToJwk(did);
    expect(result).toEqual(jwk);
  });
});
