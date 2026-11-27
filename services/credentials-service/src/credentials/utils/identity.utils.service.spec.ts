import { IdentityUtilsService } from './identity.utils.service';

describe('IdentityUtilsService', () => {
  let service: IdentityUtilsService;
  let axiosRef: any;

  beforeEach(() => {
    axiosRef = { post: jest.fn(), get: jest.fn() };
    process.env.IDENTITY_BASE_URL = 'http://identity:3332';
    service = new IdentityUtilsService({ axiosRef } as any);
    service.identityBaseUrl = 'http://identity:3332';
  });

  describe('signVC', () => {
    it('returns signed VC on success', async () => {
      const signedVC = { proof: { type: 'Ed25519Signature2020' } };
      axiosRef.post.mockResolvedValue({ data: signedVC });
      const result = await service.signVC({ credential: {} } as any, 'did:rcw:issuer');
      expect(result).toEqual(signedVC);
      expect(axiosRef.post).toHaveBeenCalledWith(
        'http://identity:3332/utils/sign',
        { DID: 'did:rcw:issuer', payload: { credential: {} } }
      );
    });

    it('throws InternalServerErrorException on HTTP error', async () => {
      axiosRef.post.mockRejectedValue(new Error('network'));
      await expect(service.signVC({} as any, 'did:rcw:issuer')).rejects.toThrow('Error signing VC');
    });
  });

  describe('signJwt', () => {
    it('returns JWT string on success', async () => {
      axiosRef.post.mockResolvedValue({ data: { jwt: 'header.payload.signature' } });
      const result = await service.signJwt('did:rcw:issuer', { sub: 'test' });
      expect(result).toBe('header.payload.signature');
    });

    it('throws InternalServerErrorException on HTTP error', async () => {
      axiosRef.post.mockRejectedValue(new Error('network'));
      await expect(service.signJwt('did:rcw:issuer', {})).rejects.toThrow('Error signing JWT');
    });
  });

  describe('verifyJwt', () => {
    it('returns verified result on success', async () => {
      axiosRef.post.mockResolvedValue({ data: { verified: true, payload: { sub: 'test' } } });
      const result = await service.verifyJwt('some.jwt.value');
      expect(result).toEqual({ verified: true, payload: { sub: 'test' } });
    });

    it('returns error object on HTTP failure (does NOT throw)', async () => {
      axiosRef.post.mockRejectedValue(new Error('network'));
      const result = await service.verifyJwt('bad.jwt');
      expect(result.verified).toBe(false);
      expect(result.error).toBeDefined();
    });
  });

  describe('signSdJwt', () => {
    it('returns SD-JWT string on success', async () => {
      axiosRef.post.mockResolvedValue({ data: { sdJwt: 'sd.jwt.value~disclosure' } });
      const result = await service.signSdJwt('did:rcw:issuer', { sub: 'test' }, ['name']);
      expect(result).toBe('sd.jwt.value~disclosure');
    });

    it('throws InternalServerErrorException on HTTP error', async () => {
      axiosRef.post.mockRejectedValue(new Error('network'));
      await expect(service.signSdJwt('did:rcw:issuer', {})).rejects.toThrow('Error signing SD-JWT');
    });
  });

  describe('verifySdJwt', () => {
    it('returns verified result on success', async () => {
      axiosRef.post.mockResolvedValue({ data: { verified: true, claims: { sub: 'test' } } });
      const result = await service.verifySdJwt('sd.jwt', undefined, { nonce: 'n1', audience: 'aud' });
      expect(result.verified).toBe(true);
    });

    it('returns error object on HTTP failure (does NOT throw)', async () => {
      axiosRef.post.mockRejectedValue(new Error('network'));
      const result = await service.verifySdJwt('bad.sdjwt');
      expect(result.verified).toBe(false);
      expect(result.error).toBeDefined();
    });
  });

  describe('signMdoc', () => {
    it('returns mdoc string on success', async () => {
      axiosRef.post.mockResolvedValue({ data: { mdoc: 'mdoc_base64' } });
      const result = await service.signMdoc('did:rcw:issuer', 'org.iso.18013.5.1', { ns: { k: 'v' } });
      expect(result).toBe('mdoc_base64');
    });

    it('throws InternalServerErrorException on HTTP error', async () => {
      axiosRef.post.mockRejectedValue(new Error('network'));
      await expect(service.signMdoc('did:rcw:issuer', 'doc', {})).rejects.toThrow('Error signing mdoc');
    });
  });

  describe('verifyMdoc', () => {
    it('returns verified result on success', async () => {
      axiosRef.post.mockResolvedValue({ data: { verified: true, claims: {}, docType: 'org.iso.18013.5.1' } });
      const result = await service.verifyMdoc('mdoc_data');
      expect(result.verified).toBe(true);
      expect(result.docType).toBe('org.iso.18013.5.1');
    });

    it('returns error object on HTTP failure (does NOT throw)', async () => {
      axiosRef.post.mockRejectedValue(new Error('network'));
      const result = await service.verifyMdoc('bad_mdoc');
      expect(result.verified).toBe(false);
      expect(result.error).toBeDefined();
    });
  });

  describe('resolveDID', () => {
    it('returns DID document on success', async () => {
      const didDoc = { id: 'did:rcw:123', verificationMethod: [] };
      axiosRef.get.mockResolvedValue({ data: didDoc });
      const result = await service.resolveDID('did:rcw:123');
      expect(result).toEqual(didDoc);
      expect(axiosRef.get).toHaveBeenCalledWith('http://identity:3332/did/resolve/did%3Arcw%3A123');
    });

    it('URL-encodes special characters in DID', async () => {
      axiosRef.get.mockResolvedValue({ data: { id: 'did:web:example.com' } });
      await service.resolveDID('did:web:example.com');
      expect(axiosRef.get).toHaveBeenCalledWith('http://identity:3332/did/resolve/did%3Aweb%3Aexample.com');
    });

    it('throws InternalServerErrorException on HTTP error', async () => {
      axiosRef.get.mockRejectedValue(new Error('not found'));
      await expect(service.resolveDID('did:rcw:bad')).rejects.toThrow('Error resolving DID');
    });
  });

  describe('generateDID', () => {
    it('returns DID documents on success', async () => {
      const dids = [{ id: 'did:rcw:new-1' }];
      axiosRef.post.mockResolvedValue({ data: dids });
      const result = await service.generateDID(['test'], 'rcw');
      expect(result).toEqual(dids);
    });

    it('throws InternalServerErrorException on HTTP error', async () => {
      axiosRef.post.mockRejectedValue(new Error('error'));
      await expect(service.generateDID([])).rejects.toThrow('Error generating DID');
    });

    it('asks for a key pair by default', async () => {
      axiosRef.post.mockResolvedValue({ data: [{ id: 'did:rcw:new-1' }] });
      await service.generateDID(['issuer']);
      expect(axiosRef.post.mock.calls[0][1].content[0]).toMatchObject({ method: 'rcw', withKey: true });
    });
  });

  describe('generateIdentifier', () => {
    const UUID = '[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}';
    afterEach(() => delete process.env.RESOLVABLE_CREDENTIAL_IDS);

    it.each([[undefined, 'rcw'], ['', 'rcw'], [' custom ', 'custom']])('mints did:<method>:<uuid> locally for method %p', async (method, expected) => {
      const [doc] = await service.generateIdentifier([], method as any);
      expect(doc.id).toMatch(new RegExp(`^did:${expected}:${UUID}$`));
      expect(axiosRef.post).not.toHaveBeenCalled();
    });

    it('asks identity-service for a keyless DID when RESOLVABLE_CREDENTIAL_IDS=true', async () => {
      process.env.RESOLVABLE_CREDENTIAL_IDS = 'true';
      axiosRef.post.mockResolvedValue({ data: [{ id: 'did:rcw:new-1' }] });
      const result = await service.generateIdentifier(['status list']);
      expect(result).toEqual([{ id: 'did:rcw:new-1' }]);
      expect(axiosRef.post.mock.calls[0][1].content[0]).toMatchObject({ alsoKnownAs: ['status list'], method: 'rcw', withKey: false });
    });

    it('always creates did:web ids in identity-service, without a key', async () => {
      axiosRef.post.mockResolvedValue({ data: [{ id: 'did:web:example.com:1' }] });
      await service.generateIdentifier([], 'web');
      expect(axiosRef.post.mock.calls[0][1].content[0]).toMatchObject({ method: 'web', withKey: false });
    });
  });
});
