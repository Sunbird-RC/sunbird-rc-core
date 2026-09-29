import { InternalServerErrorException } from '@nestjs/common';
import { CredentialsClient } from './credentials.client';

describe('CredentialsClient', () => {
  const ORIGINAL_ENV = process.env;
  let http: any;
  let client: CredentialsClient;

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    process.env.CREDENTIAL_SERVICE_BASE_URL = 'http://credentials.example';
    http = { axiosRef: { get: jest.fn(), post: jest.fn() } };
    client = new CredentialsClient(http as any);
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  describe('issue', () => {
    it('POSTs payload verbatim and returns res.data', async () => {
      const payload: any = {
        credential: { foo: 'bar' },
        credentialSchemaId: 'schema-1',
        credentialSchemaVersion: '1.0',
        tags: ['t1'],
      };
      http.axiosRef.post.mockResolvedValue({ data: { credential: { id: 'vc-1' } } });
      const result = await client.issue(payload);
      expect(http.axiosRef.post).toHaveBeenCalledWith(
        'http://credentials.example/credentials/issue',
        payload,
      );
      expect(result).toEqual({ credential: { id: 'vc-1' } });
    });

    it('throws InternalServerErrorException on rejection', async () => {
      http.axiosRef.post.mockRejectedValue(new Error('boom'));
      const payload: any = {
        credential: {},
        credentialSchemaId: 's',
        credentialSchemaVersion: '1.0',
        tags: [],
      };
      await expect(client.issue(payload)).rejects.toThrow(InternalServerErrorException);
      await expect(client.issue(payload)).rejects.toThrow('Error issuing credential');
    });
  });

  describe('verify', () => {
    it('POSTs {verifiableCredential, options} and returns res.data on success', async () => {
      http.axiosRef.post.mockResolvedValue({ data: { verified: true } });
      const vc = { id: 'vc-1' };
      const options = { challenge: 'c1', domain: 'd1' };
      const result = await client.verify(vc, options);
      expect(http.axiosRef.post).toHaveBeenCalledWith(
        'http://credentials.example/credentials/verify',
        { verifiableCredential: vc, options },
      );
      expect(result).toEqual({ verified: true });
    });

    it('returns {errors:[...]} on rejection instead of throwing', async () => {
      http.axiosRef.post.mockRejectedValue(new Error('boom'));
      const result = await client.verify({ id: 'vc-1' });
      expect(result).toEqual({ errors: ['Error verifying credential'] });
    });
  });

  describe('getStatusList', () => {
    it('GETs the URL-encoded id and returns res.data', async () => {
      http.axiosRef.get.mockResolvedValue({ data: { statusList: 'abc' } });
      const result = await client.getStatusList('status/list:1');
      expect(http.axiosRef.get).toHaveBeenCalledWith(
        'http://credentials.example/credentials/status-list/status%2Flist%3A1',
      );
      expect(result).toEqual({ statusList: 'abc' });
    });

    it('does not catch a rejection — it propagates uncaught', async () => {
      const err = new Error('network down');
      http.axiosRef.get.mockRejectedValue(err);
      await expect(client.getStatusList('id-1')).rejects.toBe(err);
    });
  });
});