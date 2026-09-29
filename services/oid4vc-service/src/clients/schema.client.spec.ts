import { SchemaClient } from './schema.client';

describe('SchemaClient', () => {
  const ORIGINAL_ENV = process.env;
  let http: any;
  let client: SchemaClient;

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    process.env.SCHEMA_BASE_URL = 'http://schema.example';
    http = { axiosRef: { get: jest.fn(), post: jest.fn() } };
    client = new SchemaClient(http as any);
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  describe('getOid4vciConfigs', () => {
    it('GETs the configs URL and returns res.data when populated', async () => {
      const configs = [{ schemaId: 's1', version: '1.0' }];
      http.axiosRef.get.mockResolvedValue({ data: configs });
      const result = await client.getOid4vciConfigs();
      expect(http.axiosRef.get).toHaveBeenCalledWith(
        'http://schema.example/credential-schema/oid4vci-configs',
      );
      expect(result).toEqual(configs);
    });

    it('returns [] when res.data is falsy', async () => {
      http.axiosRef.get.mockResolvedValue({ data: undefined });
      const result = await client.getOid4vciConfigs();
      expect(result).toEqual([]);
    });

    it('returns [] on rejection instead of throwing', async () => {
      http.axiosRef.get.mockRejectedValue(new Error('boom'));
      const result = await client.getOid4vciConfigs();
      expect(result).toEqual([]);
    });
  });

  describe('getSchema', () => {
    it('GETs the URL-encoded id and version, returns res.data', async () => {
      http.axiosRef.get.mockResolvedValue({ data: { schemaId: 's1' } });
      const result = await client.getSchema('schema:1', '2.0');
      expect(http.axiosRef.get).toHaveBeenCalledWith(
        'http://schema.example/credential-schema/schema%3A1/2.0',
      );
      expect(result).toEqual({ schemaId: 's1' });
    });

    it('does not catch a rejection — it propagates uncaught', async () => {
      const err = new Error('network down');
      http.axiosRef.get.mockRejectedValue(err);
      await expect(client.getSchema('id-1', '1.0')).rejects.toBe(err);
    });
  });
});