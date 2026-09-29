import { InternalServerErrorException } from '@nestjs/common';
import { IdentityClient } from './identity.client';

describe('IdentityClient', () => {
  const ORIGINAL_ENV = process.env;
  let http: any;
  let client: IdentityClient;

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    process.env.IDENTITY_BASE_URL = 'http://identity.example';
    http = { axiosRef: { get: jest.fn(), post: jest.fn() } };
    client = new IdentityClient(http as any);
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  describe('resolveDID', () => {
    it('GETs the URL-encoded DID and returns res.data', async () => {
      http.axiosRef.get.mockResolvedValue({ data: { id: 'did:rcw:123' } });
      const result = await client.resolveDID('did:rcw:123');
      expect(http.axiosRef.get).toHaveBeenCalledWith(
        'http://identity.example/did/resolve/did%3Arcw%3A123',
      );
      expect(result).toEqual({ id: 'did:rcw:123' });
    });

    it('throws InternalServerErrorException on rejection', async () => {
      http.axiosRef.get.mockRejectedValue(new Error('boom'));
      await expect(client.resolveDID('did:rcw:123')).rejects.toThrow(
        InternalServerErrorException,
      );
      await expect(client.resolveDID('did:rcw:123')).rejects.toThrow('Error resolving DID');
    });
  });

  describe('generateDID', () => {
    it('POSTs the exact body with the given method', async () => {
      http.axiosRef.post.mockResolvedValue({ data: [{ id: 'did:web:x' }] });
      await client.generateDID('web');
      expect(http.axiosRef.post).toHaveBeenCalledWith('http://identity.example/did/generate', {
        content: [
          {
            alsoKnownAs: [],
            services: [{ id: 'oid4vc', type: 'OID4VCService' }],
            method: 'web',
          },
        ],
      });
    });

    it('defaults method to "web" when omitted', async () => {
      http.axiosRef.post.mockResolvedValue({ data: [{ id: 'did:web:x' }] });
      await client.generateDID();
      expect(http.axiosRef.post).toHaveBeenCalledWith(
        'http://identity.example/did/generate',
        expect.objectContaining({ content: [expect.objectContaining({ method: 'web' })] }),
      );
    });

    it('returns res.data[0] when res.data is an array', async () => {
      http.axiosRef.post.mockResolvedValue({ data: [{ id: 'did:web:x' }, { id: 'did:web:y' }] });
      const result = await client.generateDID('web');
      expect(result).toEqual({ id: 'did:web:x' });
    });

    it('returns res.data itself when it is not an array', async () => {
      http.axiosRef.post.mockResolvedValue({ data: { id: 'did:web:x' } });
      const result = await client.generateDID('web');
      expect(result).toEqual({ id: 'did:web:x' });
    });

    it('does not catch a rejection — it propagates as-is', async () => {
      const err = new Error('network down');
      http.axiosRef.post.mockRejectedValue(err);
      await expect(client.generateDID('web')).rejects.toBe(err);
    });
  });

  describe('signJwt', () => {
    it('POSTs {DID, payload, header} and returns res.data?.jwt', async () => {
      http.axiosRef.post.mockResolvedValue({ data: { jwt: 'signed.jwt.value' } });
      const result = await client.signJwt('did:web:x', { foo: 'bar' }, { alg: 'ES256' });
      expect(http.axiosRef.post).toHaveBeenCalledWith('http://identity.example/utils/sign-jwt', {
        DID: 'did:web:x',
        payload: { foo: 'bar' },
        header: { alg: 'ES256' },
      });
      expect(result).toBe('signed.jwt.value');
    });

    it('defaults header to {} when omitted', async () => {
      http.axiosRef.post.mockResolvedValue({ data: { jwt: 'x' } });
      await client.signJwt('did:web:x', { foo: 'bar' });
      expect(http.axiosRef.post).toHaveBeenCalledWith(
        'http://identity.example/utils/sign-jwt',
        expect.objectContaining({ header: {} }),
      );
    });

    it('throws InternalServerErrorException on rejection', async () => {
      http.axiosRef.post.mockRejectedValue(new Error('boom'));
      await expect(client.signJwt('did:web:x', {})).rejects.toThrow(
        InternalServerErrorException,
      );
      await expect(client.signJwt('did:web:x', {})).rejects.toThrow('Error signing JWT');
    });
  });

  describe('verifyJwt', () => {
    it('POSTs {jwt, DID} and returns res.data on success', async () => {
      http.axiosRef.post.mockResolvedValue({ data: { verified: true, payload: { sub: 'x' } } });
      const result = await client.verifyJwt('some.jwt', 'did:web:x');
      expect(http.axiosRef.post).toHaveBeenCalledWith('http://identity.example/utils/verify-jwt', {
        jwt: 'some.jwt',
        DID: 'did:web:x',
      });
      expect(result).toEqual({ verified: true, payload: { sub: 'x' } });
    });

    it('returns {verified:false, error} on rejection instead of throwing', async () => {
      http.axiosRef.post.mockRejectedValue(new Error('boom'));
      const result = await client.verifyJwt('some.jwt');
      expect(result).toEqual({ verified: false, error: 'Error verifying JWT' });
    });
  });

  describe('getJwks', () => {
    it('GETs the jwks well-known URL and returns res.data', async () => {
      http.axiosRef.get.mockResolvedValue({ data: { keys: [] } });
      const result = await client.getJwks();
      expect(http.axiosRef.get).toHaveBeenCalledWith(
        'http://identity.example/.well-known/jwks.json',
      );
      expect(result).toEqual({ keys: [] });
    });

    it('does not catch a rejection — it propagates uncaught', async () => {
      const err = new Error('network down');
      http.axiosRef.get.mockRejectedValue(err);
      await expect(client.getJwks()).rejects.toBe(err);
    });
  });
});
