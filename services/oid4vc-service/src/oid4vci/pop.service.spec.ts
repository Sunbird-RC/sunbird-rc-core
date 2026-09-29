import * as crypto from 'crypto';
import * as jose from 'jose';
import { PopService } from './pop.service';

describe('PopService.verifyJwtProof', () => {
  let identity: any;
  let service: PopService;

  const expected = { audience: 'https://issuer.example', nonce: 'nonce-123' };

  beforeEach(() => {
    identity = { resolveDID: jest.fn() };
    service = new PopService(identity);
  });

  async function makeKeyPair() {
    const { publicKey, privateKey } = crypto.generateKeyPairSync('ec', {
      namedCurve: 'prime256v1',
    });
    const publicJwk = (await jose.exportJWK(publicKey)) as jose.JWK;
    return { privateKey, publicJwk };
  }

  async function signProof(
    privateKey: crypto.KeyObject,
    header: Record<string, any>,
    claimOverrides: Record<string, any> = {},
  ) {
    const claims = {
      iat: Math.floor(Date.now() / 1000),
      aud: expected.audience,
      nonce: expected.nonce,
      ...claimOverrides,
    };
    return new jose.SignJWT(claims)
      .setProtectedHeader({ alg: 'ES256', ...header })
      .sign(privateKey);
  }

  function tamperSignature(jwt: string): string {
    const [header, payload, signature] = jwt.split('.');
    const sigBytes = Buffer.from(signature, 'base64url');
    sigBytes[Math.floor(sigBytes.length / 2)] ^= 0xff;
    return `${header}.${payload}.${sigBytes.toString('base64url')}`;
  }

  it('inline jwk header (no kid/iss): valid, holderDid derived as did:jwk, holderKid undefined', async () => {
    const { privateKey, publicJwk } = await makeKeyPair();
    const jwt = await signProof(privateKey, { jwk: publicJwk });

    const result = await service.verifyJwtProof(jwt, expected);

    expect(result.valid).toBe(true);
    expect(result.holderDid).toBe(
      `did:jwk:${Buffer.from(JSON.stringify(publicJwk)).toString('base64url')}`,
    );
    expect(result.holderKid).toBeUndefined();
    expect(result.holderJwk).toEqual(publicJwk);
  });

  it('kid pointing at a self-contained did:jwk resolves locally, valid, holderKid = kid verbatim', async () => {
    const { privateKey, publicJwk } = await makeKeyPair();
    const did = `did:jwk:${Buffer.from(JSON.stringify(publicJwk)).toString('base64url')}`;
    const kid = `${did}#0`;
    const jwt = await signProof(privateKey, { kid }, { iss: did });

    const result = await service.verifyJwtProof(jwt, expected);

    expect(result.valid).toBe(true);
    expect(result.holderKid).toBe(kid);
    expect(result.holderDid).toBe(did);
    expect(identity.resolveDID).not.toHaveBeenCalled();
  });

  it('kid pointing at a non-self-contained DID falls through to identity.resolveDID', async () => {
    const { privateKey, publicJwk } = await makeKeyPair();
    const kid = 'did:web:example.com#key-1';
    const jwt = await signProof(privateKey, { kid }, { iss: 'did:web:example.com' });
    identity.resolveDID.mockResolvedValue({
      verificationMethod: [{ id: kid, publicKeyJwk: publicJwk }],
    });

    const result = await service.verifyJwtProof(jwt, expected);

    expect(result.valid).toBe(true);
    expect(identity.resolveDID).toHaveBeenCalledWith('did:web:example.com');
    expect(result.holderKid).toBe(kid);
  });

  it('inline jwk alongside a mismatching did:jwk kid/iss is rejected', async () => {
    const { privateKey, publicJwk } = await makeKeyPair();
    const { publicJwk: wrongJwk } = await makeKeyPair();
    const wrongDid = `did:jwk:${Buffer.from(JSON.stringify(wrongJwk)).toString('base64url')}`;
    const jwt = await signProof(privateKey, { jwk: publicJwk, kid: `${wrongDid}#0` }, { iss: wrongDid });

    const result = await service.verifyJwtProof(jwt, expected);

    expect(result).toEqual({
      valid: false,
      error: 'kid/iss DID does not match inline jwk header',
    });
  });

  it('inline jwk alongside a kid for a non-self-contained (did:web) DID is rejected', async () => {
    const { privateKey, publicJwk } = await makeKeyPair();
    const jwt = await signProof(
      privateKey,
      { jwk: publicJwk, kid: 'did:web:example.com#key-1' },
      { iss: 'did:web:example.com' },
    );

    const result = await service.verifyJwtProof(jwt, expected);

    expect(result).toEqual({
      valid: false,
      error: 'inline jwk header not permitted alongside a registry-resolved holder DID',
    });
    expect(identity.resolveDID).not.toHaveBeenCalled();
  });

  it('no jwk and no resolvable kid: registry resolves but has no matching verificationMethod', async () => {
    const { privateKey } = await makeKeyPair();
    const kid = 'did:web:example.com#key-1';
    const jwt = await signProof(privateKey, { kid }, { iss: 'did:web:example.com' });
    identity.resolveDID.mockResolvedValue({ verificationMethod: [] });

    const result = await service.verifyJwtProof(jwt, expected);

    expect(result).toEqual({ valid: false, error: 'No holder JWK found in proof (jwk/kid)' });
  });

  it('signature verification failure on a tampered proof', async () => {
    const { privateKey, publicJwk } = await makeKeyPair();
    const jwt = await signProof(privateKey, { jwk: publicJwk });
    const tampered = tamperSignature(jwt);

    const result = await service.verifyJwtProof(tampered, expected);

    expect(result.valid).toBe(false);
    expect(result.error).toEqual(expect.stringContaining('signature verification failed'));
  });

  it('audience mismatch', async () => {
    const { privateKey, publicJwk } = await makeKeyPair();
    const jwt = await signProof(privateKey, { jwk: publicJwk }, { aud: 'https://someone-else.example' });

    const result = await service.verifyJwtProof(jwt, expected);

    expect(result).toEqual({ valid: false, error: 'PoP audience mismatch' });
  });

  it('nonce mismatch', async () => {
    const { privateKey, publicJwk } = await makeKeyPair();
    const jwt = await signProof(privateKey, { jwk: publicJwk }, { nonce: 'wrong-nonce' });

    const result = await service.verifyJwtProof(jwt, expected);

    expect(result).toEqual({ valid: false, error: 'PoP nonce mismatch' });
  });
});