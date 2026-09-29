import { BadRequestException, NotFoundException } from '@nestjs/common';
import { Oid4vciService } from './oid4vci.service';
import { MemoryStoreService } from '../session/memory-store.service';
import { Oid4vciSchemaConfig } from '../clients/schema.client';
import { normalizeVct } from './vct.util';

describe('Oid4vciService', () => {
  const ORIGINAL_ENV = process.env;
  let store: MemoryStoreService;
  let credentials: any;
  let schema: any;
  let tokens: any;
  let pop: any;

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    process.env.PUBLIC_URL = 'https://issuer.example';
    store = new MemoryStoreService();
    credentials = { issue: jest.fn(), verify: jest.fn(), getStatusList: jest.fn() };
    schema = { getOid4vciConfigs: jest.fn(), getSchema: jest.fn() };
    tokens = {
      getIssuerDid: jest.fn().mockReturnValue('did:rcw:issuer1'),
      mintAccessToken: jest.fn().mockResolvedValue('minted.access.token'),
      validateAccessToken: jest.fn(),
    };
    pop = { verifyJwtProof: jest.fn() };
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  function makeService() {
    return new Oid4vciService(store as any, credentials, schema, tokens, pop);
  }

  function makeSchemaConfig(overrides: Partial<Oid4vciSchemaConfig> = {}): Oid4vciSchemaConfig {
    return {
      schemaId: 'did:schema:teacher',
      version: '1.0',
      name: 'Teacher Credential',
      type: 'record',
      tags: ['teacher'],
      formats: ['ldp_vc'],
      vct: 'Teacher Credential',
      display: [],
      schema: {},
      author: 'did:rcw:issuer1',
      ...overrides,
    };
  }

  describe('issuerMetadata', () => {
    it('keys a single-format schema by the bare schemaId', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig()]);
      const service = makeService();

      const meta: any = await service.issuerMetadata();

      expect(meta.credential_configurations_supported['did:schema:teacher']).toBeDefined();
      expect(meta.credential_configurations_supported['did:schema:teacher'].format).toBe('ldp_vc');
    });

    it('keys a multi-format schema by schemaId_format, with vct present only on vc+sd-jwt', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([
        makeSchemaConfig({ formats: ['ldp_vc', 'vc+sd-jwt'] }),
      ]);
      const service = makeService();

      const meta: any = await service.issuerMetadata();
      const supported = meta.credential_configurations_supported;

      expect(supported['did:schema:teacher_ldp_vc']).toBeDefined();
      expect(supported['did:schema:teacher_ldp_vc']).not.toHaveProperty('vct');
      expect(supported['did:schema:teacher_vc+sd-jwt']).toBeDefined();
      expect(supported['did:schema:teacher_vc+sd-jwt'].vct).toBe(
        normalizeVct('Teacher Credential', 'https://issuer.example'),
      );
    });

    it('mso_mdoc format has doctype/claims, no credential_definition', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([
        makeSchemaConfig({
          formats: ['mso_mdoc'],
          mdoc: { docType: 'org.iso.18013.5.1.mDL', namespace: 'org.iso.18013.5.1' },
        }),
      ]);
      const service = makeService();

      const meta: any = await service.issuerMetadata();
      const entry = meta.credential_configurations_supported['did:schema:teacher'];

      expect(entry.doctype).toBe('org.iso.18013.5.1.mDL');
      expect(entry.claims).toEqual({ 'org.iso.18013.5.1': {} });
      expect(entry).not.toHaveProperty('credential_definition');
    });

    it('draft13CompatMode uses credentials_supported instead of credential_configurations_supported', async () => {
      process.env.DRAFT13_COMPAT_MODE = 'true';
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig()]);
      const service = makeService();

      const meta: any = await service.issuerMetadata();

      expect(meta.credentials_supported).toBeDefined();
      expect(meta.credential_configurations_supported).toBeUndefined();
    });
  });

  describe('getVctTypeMetadata', () => {
    it('returns vct/name/display for a matching vc+sd-jwt config', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([
        makeSchemaConfig({ formats: ['vc+sd-jwt'], vct: 'Teacher Credential', display: [{ name: 'Teacher' }] }),
      ]);
      const service = makeService();

      const meta = await service.getVctTypeMetadata('teacher-credential');
      expect(meta.vct).toBe(normalizeVct('Teacher Credential', 'https://issuer.example'));
      expect(meta.name).toBe('Teacher Credential');
      expect(meta.display).toEqual([{ name: 'Teacher' }]);
    });

    it('throws NotFoundException when no vc+sd-jwt config matches the slug', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig({ formats: ['ldp_vc'] })]);
      const service = makeService();

      await expect(service.getVctTypeMetadata('teacher-credential')).rejects.toThrow(
        NotFoundException,
      );
    });
  });

  describe('createOffer', () => {
    it('derives format from cfg.formats[0] on an exact single-format schemaId match', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig()]);
      const service = makeService();

      const res: any = await service.createOffer({
        credential_configuration_id: 'did:schema:teacher',
        claims: { name: 'Alice' },
      });

      expect(res.offer_id).toBeDefined();
      expect(res.credential_offer.credential_configuration_ids).toEqual(['did:schema:teacher']);
      expect(res.qr_data).toContain('openid-credential-offer://');
    });

    it('matches a schemaId_format suffix when the bare schemaId does not match', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([
        makeSchemaConfig({ formats: ['ldp_vc', 'vc+sd-jwt'] }),
      ]);
      const service = makeService();

      const res: any = await service.createOffer({
        credential_configuration_id: 'did:schema:teacher_vc+sd-jwt',
        claims: {},
      });

      expect(res.credential_offer.credential_configuration_ids).toEqual([
        'did:schema:teacher_vc+sd-jwt',
      ]);
    });

    it('falls back to name-based match, preferring a candidate supporting body.format', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([
        makeSchemaConfig({ schemaId: 'did:schema:old', formats: ['ldp_vc'] }),
        makeSchemaConfig({ schemaId: 'did:schema:new', formats: ['ldp_vc', 'vc+sd-jwt'] }),
      ]);
      const service = makeService();

      const res: any = await service.createOffer({
        credential_configuration_id: 'Teacher Credential',
        format: 'vc+sd-jwt',
        claims: {},
      });

      expect(res.credential_offer.credential_configuration_ids).toEqual([
        'did:schema:new_vc+sd-jwt',
      ]);
    });

    it('throws NotFoundException for an unknown id/name', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig()]);
      const service = makeService();

      await expect(
        service.createOffer({ credential_configuration_id: 'nope', claims: {} }),
      ).rejects.toThrow(NotFoundException);
    });

    it('throws BadRequestException when the requested format is not in cfg.formats', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig({ formats: ['ldp_vc'] })]);
      const service = makeService();

      await expect(
        service.createOffer({
          credential_configuration_id: 'did:schema:teacher',
          format: 'vc+sd-jwt',
          claims: {},
        }),
      ).rejects.toThrow(BadRequestException);
    });

    it('throws BadRequestException for mso_mdoc format when cfg.mdoc is missing', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig({ formats: ['mso_mdoc'] })]);
      const service = makeService();

      await expect(
        service.createOffer({ credential_configuration_id: 'did:schema:teacher', claims: {} }),
      ).rejects.toThrow(BadRequestException);
    });

    it('rejects tx_code_required: true (not implemented)', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig()]);
      const service = makeService();

      await expect(
        service.createOffer({
          credential_configuration_id: 'did:schema:teacher',
          claims: {},
          tx_code_required: true,
        }),
      ).rejects.toThrow(/tx_code_required is not yet implemented/);
    });

    it('happy path stores the session and returns offer_id/credential_offer/qr_data', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig()]);
      const service = makeService();

      const res: any = await service.createOffer({
        credential_configuration_id: 'did:schema:teacher',
        claims: { name: 'Alice' },
      });

      const session = await store.get(`oid4vc:offer:${res.offer_id}`);
      expect(session).not.toBeNull();
      expect(res.credential_offer.grants['urn:ietf:params:oauth:grant-type:pre-authorized_code']).toBeDefined();
    });

    it('draft13CompatMode returns `credentials` instead of `credential_configuration_ids`', async () => {
      process.env.DRAFT13_COMPAT_MODE = 'true';
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig()]);
      const service = makeService();

      const res: any = await service.createOffer({
        credential_configuration_id: 'did:schema:teacher',
        claims: {},
      });

      expect(res.credential_offer.credentials).toEqual(['did:schema:teacher']);
      expect(res.credential_offer.credential_configuration_ids).toBeUndefined();
    });
  });

  describe('getOffer', () => {
    it('returns the offer object when found', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig()]);
      const service = makeService();
      const created = await service.createOffer({
        credential_configuration_id: 'did:schema:teacher',
        claims: {},
      });

      const offer: any = await service.getOffer(created.offer_id);
      expect(offer.credential_configuration_ids).toEqual(['did:schema:teacher']);
    });

    it('throws NotFoundException when not found', async () => {
      const service = makeService();
      await expect(service.getOffer('nonexistent')).rejects.toThrow(NotFoundException);
    });
  });

  describe('token', () => {
    async function createSession(overrides: Record<string, any> = {}) {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig()]);
      const service = makeService();
      const created = await service.createOffer({
        credential_configuration_id: 'did:schema:teacher',
        claims: { name: 'Alice' },
      });
      if (Object.keys(overrides).length) {
        const session = await store.get<any>(`oid4vc:offer:${created.offer_id}`);
        await store.set(`oid4vc:offer:${created.offer_id}`, { ...session, ...overrides }, 600);
      }
      return { service, created };
    }

    it('rejects an unsupported grant_type', async () => {
      const { service } = await createSession();
      await expect(service.token({ grant_type: 'authorization_code' })).rejects.toThrow(
        'unsupported_grant_type',
      );
    });

    it('rejects a missing pre-authorized_code', async () => {
      const { service } = await createSession();
      await expect(
        service.token({ grant_type: 'urn:ietf:params:oauth:grant-type:pre-authorized_code' }),
      ).rejects.toThrow(BadRequestException);
    });

    it('rejects an unknown/already-used code', async () => {
      const { service } = await createSession();
      await expect(
        service.token({
          grant_type: 'urn:ietf:params:oauth:grant-type:pre-authorized_code',
          'pre-authorized_code': 'bogus',
        }),
      ).rejects.toThrow(BadRequestException);
    });

    it('rejects a valid code whose session has txCodeRequired: true', async () => {
      const { service, created } = await createSession({ txCodeRequired: true });
      const offer: any = await service.getOffer(created.offer_id);
      const preAuthCode = offer.grants['urn:ietf:params:oauth:grant-type:pre-authorized_code'][
        'pre-authorized_code'
      ];

      await expect(
        service.token({
          grant_type: 'urn:ietf:params:oauth:grant-type:pre-authorized_code',
          'pre-authorized_code': preAuthCode,
        }),
      ).rejects.toThrow(BadRequestException);
    });

    it('happy path mints an access token and returns the token response; code is single-use', async () => {
      const { service, created } = await createSession();
      const offer: any = await service.getOffer(created.offer_id);
      const preAuthCode = offer.grants['urn:ietf:params:oauth:grant-type:pre-authorized_code'][
        'pre-authorized_code'
      ];

      const res = await service.token({
        grant_type: 'urn:ietf:params:oauth:grant-type:pre-authorized_code',
        'pre-authorized_code': preAuthCode,
      });

      expect(res.access_token).toBe('minted.access.token');
      expect(res.token_type).toBe('Bearer');
      expect(res.expires_in).toBe(300);
      expect(typeof res.c_nonce).toBe('string');
      expect(res.c_nonce_expires_in).toBe(300);
      expect(tokens.mintAccessToken).toHaveBeenCalledWith({
        sub: created.offer_id,
        credential_configuration_id: 'did:schema:teacher',
      });

      await expect(
        service.token({
          grant_type: 'urn:ietf:params:oauth:grant-type:pre-authorized_code',
          'pre-authorized_code': preAuthCode,
        }),
      ).rejects.toThrow(BadRequestException);
    });
  });

  describe('issueNonce', () => {
    it('returns a plausible base64url string and stores it', async () => {
      const service = makeService();
      const nonce = await service.issueNonce();

      expect(typeof nonce).toBe('string');
      expect(nonce).toMatch(/^[A-Za-z0-9_-]+$/);
      const stored = await store.getdel(`oid4vc:nonce:${nonce}`);
      expect(stored).toBe('1');
    });
  });

  describe('credential', () => {
    async function seedOfferAndToken() {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig()]);
      const service = makeService();
      const created = await service.createOffer({
        credential_configuration_id: 'did:schema:teacher',
        claims: { name: 'Alice' },
      });
      tokens.validateAccessToken.mockResolvedValue({ sub: created.offer_id });
      return { service, created };
    }

    it('throws BadRequestException when body.proof.jwt is missing', async () => {
      const { service } = await seedOfferAndToken();
      await expect(service.credential('Bearer tok', {})).rejects.toThrow(BadRequestException);
    });

    it('throws BadRequestException with a fresh c_nonce when the proof nonce is not a live c_nonce', async () => {
      const { service } = await seedOfferAndToken();
      const proofJwt = `${Buffer.from(JSON.stringify({ alg: 'ES256' })).toString('base64url')}.${Buffer.from(
        JSON.stringify({ nonce: 'not-a-real-nonce' }),
      ).toString('base64url')}.sig`;

      try {
        await service.credential('Bearer tok', { proof: { jwt: proofJwt } });
        fail('expected rejection');
      } catch (err: any) {
        expect(err).toBeInstanceOf(BadRequestException);
        const response = err.getResponse();
        expect(response.error).toBe('invalid_or_missing_proof');
        expect(typeof response.c_nonce).toBe('string');
      }
      expect(pop.verifyJwtProof).not.toHaveBeenCalled();
    });

    it('throws BadRequestException("invalid_proof: ...") when pop.verifyJwtProof returns valid:false', async () => {
      const { service, created } = await seedOfferAndToken();
      const nonce = await service.issueNonce();
      const proofJwt = `${Buffer.from(JSON.stringify({ alg: 'ES256' })).toString('base64url')}.${Buffer.from(
        JSON.stringify({ nonce }),
      ).toString('base64url')}.sig`;
      pop.verifyJwtProof.mockResolvedValue({ valid: false, error: 'bad signature' });

      await expect(
        service.credential('Bearer tok', { proof: { jwt: proofJwt } }),
      ).rejects.toThrow('invalid_proof: bad signature');
      expect(created).toBeDefined();
    });

    it('a session with deferredClaimId still issues directly since isClaimReady is a stub returning true', async () => {
      const { service, created } = await seedOfferAndToken();
      const session = await store.get<any>(`oid4vc:offer:${created.offer_id}`);
      await store.set(`oid4vc:offer:${created.offer_id}`, { ...session, deferredClaimId: 'claim-1' }, 600);

      const nonce = await service.issueNonce();
      const proofJwt = `${Buffer.from(JSON.stringify({ alg: 'ES256' })).toString('base64url')}.${Buffer.from(
        JSON.stringify({ nonce }),
      ).toString('base64url')}.sig`;
      pop.verifyJwtProof.mockResolvedValue({ valid: true, holderDid: 'did:jwk:xyz' });
      credentials.issue.mockResolvedValue({ credential: { id: 'vc-1' } });

      const res = await service.credential('Bearer tok', { proof: { jwt: proofJwt } });

      expect(res.credential).toEqual({ id: 'vc-1' });
      expect(res).not.toHaveProperty('transaction_id');
      expect(credentials.issue).toHaveBeenCalledTimes(1);
    });

    it('happy path calls credentials.issue with the right shape and returns {credential, c_nonce, format}', async () => {
      const { service, created } = await seedOfferAndToken();
      const nonce = await service.issueNonce();
      const proofJwt = `${Buffer.from(JSON.stringify({ alg: 'ES256' })).toString('base64url')}.${Buffer.from(
        JSON.stringify({ nonce }),
      ).toString('base64url')}.sig`;
      pop.verifyJwtProof.mockResolvedValue({
        valid: true,
        holderDid: 'did:jwk:holder1',
        holderJwk: { kty: 'EC' },
        holderKid: undefined,
      });
      credentials.issue.mockResolvedValue({ credential: { id: 'vc-1' } });

      const res = await service.credential('Bearer tok', { proof: { jwt: proofJwt } });

      expect(res).toEqual({ credential: { id: 'vc-1' }, c_nonce: expect.any(String), format: 'ldp_vc' });
      const issueArg = credentials.issue.mock.calls[0][0];
      expect(issueArg.credential['@context']).toEqual(
        expect.arrayContaining(['https://www.w3.org/2018/credentials/v1']),
      );
      expect(issueArg.credential.type).toEqual(['VerifiableCredential', 'Teacher Credential']);
      expect(issueArg.credential.issuer).toBe('did:rcw:issuer1');
      expect(issueArg.credential.credentialSubject).toEqual({ id: 'did:jwk:holder1', name: 'Alice' });
      expect(issueArg.credentialSchemaId).toBe('did:schema:teacher');
      expect(issueArg.format).toBe('ldp_vc');
      expect(created).toBeDefined();
    });
  });

  describe('deferred', () => {
    async function seedDeferredTxn() {
      schema.getOid4vciConfigs.mockResolvedValue([makeSchemaConfig()]);
      const service = makeService();
      const created = await service.createOffer({
        credential_configuration_id: 'did:schema:teacher',
        claims: { name: 'Alice' },
      });
      const session = await store.get<any>(`oid4vc:offer:${created.offer_id}`);
      await store.set(`oid4vc:offer:${created.offer_id}`, { ...session, deferredClaimId: 'claim-1' }, 600);
      tokens.validateAccessToken.mockResolvedValue({ sub: created.offer_id });

      const txId = 'txn-1';
      await store.set(
        `oid4vc:deferred:${txId}`,
        { offerId: created.offer_id, holderDid: 'did:jwk:holder1', holderJwk: { kty: 'EC' } },
        600,
      );
      return { service, txId };
    }

    it('throws BadRequestException when transaction_id is missing', async () => {
      const { service } = await seedDeferredTxn();
      await expect(service.deferred('Bearer tok', {})).rejects.toThrow(BadRequestException);
    });

    it('throws NotFoundException for an unknown transaction_id', async () => {
      const { service } = await seedDeferredTxn();
      await expect(
        service.deferred('Bearer tok', { transaction_id: 'nonexistent' }),
      ).rejects.toThrow(NotFoundException);
    });

    it('throws BadRequestException when the underlying offer is gone', async () => {
      const { service, txId } = await seedDeferredTxn();
      // Simulate the offer expiring after the deferred txn was created.
      const txn = await store.get<any>(`oid4vc:deferred:${txId}`);
      await store.del(`oid4vc:offer:${txn.offerId}`);

      await expect(service.deferred('Bearer tok', { transaction_id: txId })).rejects.toThrow(
        BadRequestException,
      );
    });

    it('happy path issues the credential and deletes the deferred txn (single-use)', async () => {
      const { service, txId } = await seedDeferredTxn();
      credentials.issue.mockResolvedValue({ credential: { id: 'vc-deferred' } });

      const res = await service.deferred('Bearer tok', { transaction_id: txId });
      expect(res.credential).toEqual({ id: 'vc-deferred' });
      expect(res.format).toBe('ldp_vc');

      await expect(
        service.deferred('Bearer tok', { transaction_id: txId }),
      ).rejects.toThrow(NotFoundException);
    });
  });

  describe('notification', () => {
    it('resolves without throwing when validateAccessToken succeeds', async () => {
      const service = makeService();
      tokens.validateAccessToken.mockResolvedValue({ sub: 'offer-1' });

      await expect(service.notification('Bearer tok', { event: 'credential_accepted' })).resolves.toBeUndefined();
    });

    it('propagates rejection when validateAccessToken rejects', async () => {
      const service = makeService();
      tokens.validateAccessToken.mockRejectedValue(new Error('bad token'));

      await expect(service.notification('Bearer tok', {})).rejects.toThrow('bad token');
    });
  });

  describe('buildMdocNamespaces (nice-to-have direct coverage)', () => {
    it('maps claims into namespace buckets, honoring elementMapping overrides', async () => {
      schema.getOid4vciConfigs.mockResolvedValue([
        makeSchemaConfig({
          formats: ['mso_mdoc'],
          mdoc: {
            docType: 'org.iso.18013.5.1.mDL',
            namespace: 'org.iso.18013.5.1',
            elementMapping: { custom_claim: { namespace: 'org.iso.18013.5.1.aamva', elementIdentifier: 'custom' } },
          },
        }),
      ]);
      const service = makeService();
      const created = await service.createOffer({
        credential_configuration_id: 'did:schema:teacher',
        claims: { given_name: 'Alice', custom_claim: 'value1' },
      });
      const session = await store.get<any>(`oid4vc:offer:${created.offer_id}`);

      const namespaces = (service as any).buildMdocNamespaces(session);

      expect(namespaces).toEqual({
        'org.iso.18013.5.1': { given_name: 'Alice' },
        'org.iso.18013.5.1.aamva': { custom: 'value1' },
      });
    });
  });
});