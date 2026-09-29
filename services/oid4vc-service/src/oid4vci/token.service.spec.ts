import { UnauthorizedException } from '@nestjs/common';
import { TokenService } from './token.service';

describe('TokenService', () => {
  const ORIGINAL_ENV = process.env;
  let identity: any;

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    process.env.PUBLIC_URL = 'https://issuer.example';
    identity = {
      generateDID: jest.fn(),
      signJwt: jest.fn(),
      verifyJwt: jest.fn(),
      getJwks: jest.fn(),
    };
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  describe('onModuleInit', () => {
    it('auto-provisions an issuer DID via generateDID("rcw") when ISSUER_DID is unset', async () => {
      delete process.env.ISSUER_DID;
      identity.generateDID.mockResolvedValue({ id: 'did:rcw:auto1' });
      const service = new TokenService(identity);

      await service.onModuleInit();

      expect(identity.generateDID).toHaveBeenCalledWith('rcw');
      expect(service.getIssuerDid()).toBe('did:rcw:auto1');
    });

    it('uses process.env.ISSUER_DID directly when set, without calling generateDID', async () => {
      process.env.ISSUER_DID = 'did:rcw:configured';
      const service = new TokenService(identity);

      await service.onModuleInit();

      expect(identity.generateDID).not.toHaveBeenCalled();
      expect(service.getIssuerDid()).toBe('did:rcw:configured');
    });

    it('swallows a generateDID rejection (logs, does not throw) and leaves issuerDid undefined', async () => {
      delete process.env.ISSUER_DID;
      identity.generateDID.mockRejectedValue(new Error('identity-service unreachable'));
      const service = new TokenService(identity);

      await expect(service.onModuleInit()).resolves.not.toThrow();
      expect(service.getIssuerDid()).toBeFalsy();
    });
  });

  describe('mintAccessToken', () => {
    it('signs via identity.signJwt with iss/aud=publicUrl, exp = iat + ttl.accessToken, typ at+jwt', async () => {
      identity.generateDID.mockResolvedValue({ id: 'did:rcw:auto1' });
      identity.signJwt.mockResolvedValue('signed.at.jwt');
      const service = new TokenService(identity);
      await service.onModuleInit();

      const before = Math.floor(Date.now() / 1000);
      const token = await service.mintAccessToken({ sub: 'offer-1', scope: 'openid' });
      expect(token).toBe('signed.at.jwt');

      expect(identity.signJwt).toHaveBeenCalledTimes(1);
      const [did, payload, header] = identity.signJwt.mock.calls[0];
      expect(did).toBe('did:rcw:auto1');
      expect(header).toEqual({ typ: 'at+jwt' });
      expect(payload.iss).toBe('https://issuer.example');
      expect(payload.aud).toBe('https://issuer.example');
      expect(payload.sub).toBe('offer-1');
      expect(payload.scope).toBe('openid');
      expect(payload.exp).toBe(payload.iat + 300);
      expect(payload.iat).toBeGreaterThanOrEqual(before);
    });
  });

  describe('validateAccessToken', () => {
    function makeService() {
      const service = new TokenService(identity);
      return service;
    }

    it('throws UnauthorizedException("Missing bearer token") when header is missing', async () => {
      const service = makeService();
      await expect(service.validateAccessToken(undefined)).rejects.toThrow(UnauthorizedException);
      await expect(service.validateAccessToken(undefined)).rejects.toThrow('Missing bearer token');
    });

    it('throws UnauthorizedException("Missing bearer token") when header is not Bearer', async () => {
      const service = makeService();
      await expect(service.validateAccessToken('Basic abc')).rejects.toThrow('Missing bearer token');
    });

    it('returns the payload for a valid, unexpired, correctly-issued token', async () => {
      const service = makeService();
      const payload = { exp: Math.floor(Date.now() / 1000) + 100, iss: 'https://issuer.example' };
      identity.verifyJwt.mockResolvedValue({ verified: true, payload });

      const result = await service.validateAccessToken('Bearer sometoken');
      expect(result).toBe(payload);
    });

    it('throws UnauthorizedException("Invalid access token") when verified:false', async () => {
      const service = makeService();
      identity.verifyJwt.mockResolvedValue({ verified: false, error: 'bad sig' });

      await expect(service.validateAccessToken('Bearer sometoken')).rejects.toThrow(
        'Invalid access token',
      );
    });

    it('throws UnauthorizedException("Invalid access token") for an expired token', async () => {
      const service = makeService();
      identity.verifyJwt.mockResolvedValue({
        verified: true,
        payload: { exp: Math.floor(Date.now() / 1000) - 100, iss: 'https://issuer.example' },
      });

      await expect(service.validateAccessToken('Bearer sometoken')).rejects.toThrow(
        'Invalid access token',
      );
    });

    it('throws UnauthorizedException("Invalid access token") for the wrong iss', async () => {
      const service = makeService();
      identity.verifyJwt.mockResolvedValue({
        verified: true,
        payload: { exp: Math.floor(Date.now() / 1000) + 100, iss: 'https://someone-else.example' },
      });

      await expect(service.validateAccessToken('Bearer sometoken')).rejects.toThrow(
        'Invalid access token',
      );
    });
  });

  describe('asMetadata', () => {
    it('returns the OAuth AS metadata shape', () => {
      const service = new TokenService(identity);
      expect(service.asMetadata()).toEqual({
        issuer: 'https://issuer.example',
        token_endpoint: 'https://issuer.example/oid4vc/token',
        jwks_uri: 'https://issuer.example/.well-known/jwks.json',
        grant_types_supported: ['urn:ietf:params:oauth:grant-type:pre-authorized_code'],
        response_types_supported: ['token'],
        token_endpoint_auth_methods_supported: ['none'],
      });
    });
  });

  describe('jwks', () => {
    it('delegates to identity.getJwks()', async () => {
      const service = new TokenService(identity);
      identity.getJwks.mockResolvedValue({ keys: [{ kty: 'EC' }] });

      await expect(service.jwks()).resolves.toEqual({ keys: [{ kty: 'EC' }] });
    });

    it('returns {keys: []} instead of throwing when getJwks rejects', async () => {
      const service = new TokenService(identity);
      identity.getJwks.mockRejectedValue(new Error('unreachable'));

      await expect(service.jwks()).resolves.toEqual({ keys: [] });
    });
  });
});
