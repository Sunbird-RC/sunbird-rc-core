import { SchemaUtilsSerivce } from './schema.utils.service';

describe('SchemaUtilsSerivce', () => {
  let service: SchemaUtilsSerivce;
  let axiosRef: any;

  beforeEach(() => {
    axiosRef = { get: jest.fn() };
    service = new SchemaUtilsSerivce({ axiosRef } as any);
  });

  describe('getCredentialSchema', () => {
    it('returns schema data on 200', async () => {
      axiosRef.get.mockResolvedValue({ status: 200, data: { schema: { id: 's1', version: '1.0' } } });
      const result = await service.getCredentialSchema('s1', '1.0');
      expect(result).toEqual({ id: 's1', version: '1.0' });
      expect(axiosRef.get).toHaveBeenCalledWith(expect.stringContaining('/credential-schema/s1/1.0'));
    });

    it('throws InternalServerErrorException on network error', async () => {
      axiosRef.get.mockRejectedValue(new Error('network'));
      await expect(service.getCredentialSchema('s1', '1.0')).rejects.toThrow('Error fetching credential schema');
    });
  });

  describe('getTemplateById', () => {
    it('returns template data on 200', async () => {
      axiosRef.get.mockResolvedValue({ status: 200, data: { template: '<html></html>' } });
      const result = await service.getTemplateById('t1');
      expect(result).toEqual({ template: '<html></html>' });
    });

    it('throws BadRequestException on 404', async () => {
      const err: any = new Error('not found');
      err.response = { status: 404 };
      axiosRef.get.mockRejectedValue(err);
      await expect(service.getTemplateById('t1')).rejects.toThrow('not found');
    });

    it('throws InternalServerErrorException on 500', async () => {
      const err: any = new Error('server error');
      err.response = { status: 500 };
      axiosRef.get.mockRejectedValue(err);
      await expect(service.getTemplateById('t1')).rejects.toThrow('Error fetching template');
    });
  });

  describe('verifyCredentialSubject', () => {
    it('returns valid: true for matching subject', async () => {
      const schema = {
        type: 'object',
        properties: {
          grade: { type: 'string' },
          programme: { type: 'string' },
        },
        required: ['grade', 'programme'],
      };
      const credential = {
        credentialSubject: { id: 'did:test:1', grade: 'A', programme: 'CS' },
      };
      const result = await service.verifyCredentialSubject(credential, schema);
      expect(result.valid).toBe(true);
      expect(result.errors).toBeFalsy();
    });

    it('returns valid: false with errors for missing required field', async () => {
      const schema = {
        type: 'object',
        properties: {
          grade: { type: 'string' },
        },
        required: ['grade'],
      };
      const credential = {
        credentialSubject: { id: 'did:test:1' },
      };
      const result = await service.verifyCredentialSubject(credential, schema);
      expect(result.valid).toBe(false);
      expect(result.errors).toBeDefined();
      expect(result.errors.length).toBeGreaterThan(0);
    });

    it('accepts valid date-time format', async () => {
      const schema = {
        type: 'object',
        properties: {
          issuedAt: { type: 'string', format: 'date-time' },
        },
        required: ['issuedAt'],
      };
      const credential = {
        credentialSubject: { id: 'did:test:1', issuedAt: '2024-01-15T10:30:00Z' },
      };
      const result = await service.verifyCredentialSubject(credential, schema);
      expect(result.valid).toBe(true);
    });

    it('rejects invalid date-time format', async () => {
      const schema = {
        type: 'object',
        properties: {
          issuedAt: { type: 'string', format: 'date-time' },
        },
        required: ['issuedAt'],
      };
      const credential = {
        credentialSubject: { id: 'did:test:1', issuedAt: 'not-a-date' },
      };
      const result = await service.verifyCredentialSubject(credential, schema);
      expect(result.valid).toBe(false);
    });

    it('accepts valid date format (YYYY-MM-DD)', async () => {
      const schema = {
        type: 'object',
        properties: {
          birthdate: { type: 'string', format: 'date' },
        },
        required: ['birthdate'],
      };
      const credential = {
        credentialSubject: { id: 'did:test:1', birthdate: '2000-06-15' },
      };
      const result = await service.verifyCredentialSubject(credential, schema);
      expect(result.valid).toBe(true);
    });

    it('rejects date with slashes (not YYYY-MM-DD)', async () => {
      const schema = {
        type: 'object',
        properties: {
          birthdate: { type: 'string', format: 'date' },
        },
        required: ['birthdate'],
      };
      const credential = {
        credentialSubject: { id: 'did:test:1', birthdate: '2000/06/15' },
      };
      const result = await service.verifyCredentialSubject(credential, schema);
      expect(result.valid).toBe(false);
    });

    it('does not crash on format: "date" (production bug regression)', async () => {
      // This previously crashed AJV's strict mode
      const schema = {
        type: 'object',
        properties: {
          eventDate: { type: 'string', format: 'date' },
        },
      };
      const credential = {
        credentialSubject: { id: 'did:test:1', eventDate: '2024-01-01' },
      };
      const result = await service.verifyCredentialSubject(credential, schema);
      expect(result.valid).toBe(true);
    });

    it('handles credential with no credentialSubject gracefully', async () => {
      const schema = { type: 'object', properties: {} };
      const result = await service.verifyCredentialSubject({}, schema);
      expect(result.valid).toBe(false);
    });
  });
});
