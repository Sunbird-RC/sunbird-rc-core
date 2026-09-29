// oid4vp.service.ts transitively imports @auth0/mdl (via mdoc-presentation.util.ts)
// for the mso_mdoc presentation path, which this suite doesn't exercise.
// @auth0/mdl's cose-kit dependency uses a Node "imports" subpath
// (#runtime/pkijs.js) that jest's resolver can't load, so stub it out here
// rather than pull the whole COSE/CBOR chain into these request-object tests.
jest.mock('@auth0/mdl', () => ({ Verifier: class {} }), { virtual: true });
jest.mock('@auth0/mdl/lib/cbor', () => ({ cborEncode: jest.fn(), DataItem: {} }), { virtual: true });
jest.mock('./mdoc-presentation.util', () => ({
  buildSessionTranscript: jest.fn(),
  verifyMdocPresentation: jest.fn(),
}));

import { ForbiddenException, NotFoundException } from '@nestjs/common';
import * as jose from 'jose';
import { Oid4vpService } from './oid4vp.service';
import { MemoryStoreService } from '../session/memory-store.service';
import { DcqlService } from './dcql.service';
import { PexService } from './pex.service';
import { buildSessionTranscript, verifyMdocPresentation } from './mdoc-presentation.util';

// Focused on the request-object builder (createRequest/getRequestObject) —
// the three client_id/signing shapes described in the plan: signed
// (draft-23 JAR, did: client_id), unsigned (draft-23, redirect_uri: prefix,
// no client_id_scheme field), and legacy (pre-draft-22, bare client_id +
// separate client_id_scheme, unsigned). submitResponse's verification chain
// is covered by the demo e2e scripts, not unit tests, since it depends on
// real VC/mdoc fixtures.
describe('Oid4vpService request-object modes', () => {
  const ORIGINAL_ENV = process.env;
  let signJwt: jest.Mock;
  let identity: any;
  let credentials: any;
  let tokens: any;

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    process.env.PUBLIC_URL = 'https://verifier.example';
    signJwt = jest.fn().mockResolvedValue('signed.jwt.value');
    identity = { signJwt, resolveDID: jest.fn() };
    credentials = { verify: jest.fn() };
    // Real TokenService auto-provisions an issuer DID at boot when
    // ISSUER_DID is unset (see token.service.ts onModuleInit) and
    // Oid4vpService falls back to it for signing — default to "none
    // available" here; the no-VERIFIER_DID test relies on that.
    tokens = { getIssuerDid: jest.fn().mockReturnValue(undefined) };
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  function makeService() {
    return new Oid4vpService(
      new MemoryStoreService() as any,
      identity,
      credentials,
      tokens,
      new DcqlService(),
      new PexService(),
    );
  }

  const dcqlQuery = { credentials: [{ id: 'c', meta: { type_values: [['X']] }, claims: [] }] };
  const presentationDefinition = { input_descriptors: [{ id: 'c', constraints: { fields: [] } }] };

  it('signed mode (default): did: client_id, calls identity.signJwt, serves a JWS', async () => {
    process.env.VERIFIER_DID = 'did:web:verifier.example';
    const service = makeService();

    const created = await service.createRequest({ dcql_query: dcqlQuery });
    expect(created.qr_data).toContain(encodeURIComponent('did:web:verifier.example'));

    expect(signJwt).toHaveBeenCalledTimes(1);
    const [signedDid, payload] = signJwt.mock.calls[0];
    expect(signedDid).toBe('did:web:verifier.example');
    expect(payload.client_id).toBe('did:web:verifier.example');
    expect(payload.iss).toBe(payload.client_id);
    expect(payload).not.toHaveProperty('client_id_scheme');

    const id = created.request_uri.split('/').pop() as string;
    const obj = await service.getRequestObject(id);
    expect(obj.contentType).toBe('application/oauth-authz-req+jwt');
    expect(obj.body).toBe('signed.jwt.value');
  });

  it('unsigned draft-23 mode ({signed:false}): redirect_uri: prefix, no signing, plain JSON', async () => {
    const service = makeService();
    const created = await service.createRequest({ dcql_query: dcqlQuery, signed: false });

    expect(signJwt).not.toHaveBeenCalled();
    const id = created.request_uri.split('/').pop() as string;
    const obj = await service.getRequestObject(id);
    expect(obj.contentType).toBe('application/json');
    expect(obj.body.client_id).toBe('redirect_uri:https://verifier.example/vp/response');
    expect(obj.body).not.toHaveProperty('client_id_scheme');
  });

  it('legacy mode (OID4VP_LEGACY_CLIENT_ID_SCHEME=true): bare client_id + client_id_scheme, unsigned', async () => {
    process.env.OID4VP_LEGACY_CLIENT_ID_SCHEME = 'true';
    const service = makeService();
    const created = await service.createRequest({ dcql_query: dcqlQuery });

    expect(signJwt).not.toHaveBeenCalled();
    const id = created.request_uri.split('/').pop() as string;
    const obj = await service.getRequestObject(id);
    expect(obj.contentType).toBe('application/json');
    expect(obj.body.client_id).toBe('https://verifier.example/vp/response');
    expect(obj.body.client_id_scheme).toBe('redirect_uri');
  });

  it('signed mode falls back to the auto-provisioned issuer DID ONLY when it is did:web (externally resolvable)', async () => {
    delete process.env.VERIFIER_DID;
    delete process.env.ISSUER_DID;
    tokens.getIssuerDid.mockReturnValue('did:web:issuer.example');
    const service = makeService();

    await service.createRequest({ dcql_query: dcqlQuery });
    const [signedDid, payload] = signJwt.mock.calls[0];
    expect(signedDid).toBe('did:web:issuer.example');
    expect(payload.client_id).toBe('did:web:issuer.example');
  });

  it('signed mode does NOT fall back to a did:rcw auto-provisioned issuer DID (no wallet can resolve it) and throws instead', async () => {
    delete process.env.VERIFIER_DID;
    delete process.env.ISSUER_DID;
    // This is exactly the shape token.service.ts auto-provisions when
    // ISSUER_DID is unset — confirmed live to break every presentation,
    // since the wallet has no way to resolve a did:rcw verifier identity.
    tokens.getIssuerDid.mockReturnValue('did:rcw:auto123');
    const service = makeService();
    await expect(service.createRequest({ dcql_query: dcqlQuery })).rejects.toThrow(
      /VERIFIER_DID/,
    );
    expect(signJwt).not.toHaveBeenCalled();
  });

  // Regression: caught on the real VM, whose ISSUER_DID is a did:rcw. An
  // earlier version applied the did:web guard only to the auto-provisioned
  // DID while letting an ISSUER_DID-derived one through, which would have
  // signed with an unresolvable DID and broken every deployed presentation.
  it('signed mode does NOT use a did:rcw ISSUER_DID for signing', async () => {
    delete process.env.VERIFIER_DID;
    process.env.ISSUER_DID = 'did:rcw:5e36982c-e15e-4a75-9f17-7efb77ffb470';
    tokens.getIssuerDid.mockReturnValue('did:rcw:5e36982c-e15e-4a75-9f17-7efb77ffb470');
    const service = makeService();
    await expect(service.createRequest({ dcql_query: dcqlQuery })).rejects.toThrow(
      /externally-resolvable verifier DID/,
    );
    expect(signJwt).not.toHaveBeenCalled();
  });

  it('signed mode DOES use an explicitly configured VERIFIER_DID even if it is not did:web', async () => {
    process.env.VERIFIER_DID = 'did:key:zExplicitOperatorChoice';
    const service = makeService();
    await service.createRequest({ dcql_query: dcqlQuery });
    expect(signJwt.mock.calls[0][0]).toBe('did:key:zExplicitOperatorChoice');
  });

  it('accepts presentation_definition and embeds it (not dcql_query) in the request object', async () => {
    const service = makeService();
    const created = await service.createRequest({
      presentation_definition: presentationDefinition,
      signed: false,
    });
    const id = created.request_uri.split('/').pop() as string;
    const obj = await service.getRequestObject(id);
    expect(obj.body.presentation_definition).toEqual(presentationDefinition);
    expect(obj.body).not.toHaveProperty('dcql_query');
  });

  it('rejects when both dcql_query and presentation_definition are supplied', async () => {
    const service = makeService();
    await expect(
      service.createRequest({ dcql_query: dcqlQuery, presentation_definition: presentationDefinition }),
    ).rejects.toThrow(/exactly one of/);
  });

  it('rejects when neither dcql_query nor presentation_definition is supplied', async () => {
    const service = makeService();
    await expect(service.createRequest({})).rejects.toThrow(/exactly one of/);
  });

  it('signed mode with no VERIFIER_DID/ISSUER_DID/auto-provisioned DID at all throws instead of silently downgrading', async () => {
    delete process.env.VERIFIER_DID;
    delete process.env.ISSUER_DID;
    const service = makeService();
    await expect(service.createRequest({ dcql_query: dcqlQuery })).rejects.toThrow(
      /VERIFIER_DID/,
    );
    expect(signJwt).not.toHaveBeenCalled();
  });
});

function fakeJwt(payload: any, header: any = { alg: 'ES256' }): string {
  const h = Buffer.from(JSON.stringify(header)).toString('base64url');
  const p = Buffer.from(JSON.stringify(payload)).toString('base64url');
  return `${h}.${p}.sig`;
}

describe('Oid4vpService — submitResponse', () => {
  const ORIGINAL_ENV = process.env;
  const responseUri = 'https://verifier.example/vp/response';
  const clientId = `redirect_uri:${responseUri}`;
  let store: MemoryStoreService;
  let identity: any;
  let credentials: any;
  let tokens: any;
  let dcql: any;
  let pex: any;

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    process.env.PUBLIC_URL = 'https://verifier.example';
    store = new MemoryStoreService();
    identity = { signJwt: jest.fn(), resolveDID: jest.fn() };
    credentials = { verify: jest.fn() };
    tokens = { getIssuerDid: jest.fn().mockReturnValue(undefined) };
    dcql = { evaluate: jest.fn() };
    pex = { evaluate: jest.fn() };
    (buildSessionTranscript as jest.Mock).mockReset();
    (verifyMdocPresentation as jest.Mock).mockReset();
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  function makeService() {
    return new Oid4vpService(store as any, identity, credentials, tokens, dcql, pex);
  }

  async function seedTxn(overrides: any = {}) {
    const state = overrides.state || 'state-1';
    const id = overrides.id || 'txn-1';
    const txn = {
      queryMode: 'dcql',
      dcqlQuery: { credentials: [{ id: 'c', format: 'ldp_vc' }] },
      nonce: 'nonce-1',
      state,
      status: 'pending',
      requestMode: 'unsigned',
      clientId,
      responseUri,
      requestObject: { client_id: clientId, response_uri: responseUri },
      ...overrides,
    };
    await store.set(`oid4vp:txn:${id}`, txn, 60);
    await store.set(`oid4vp:state:${state}`, { id }, 60);
    return { id, state, key: `oid4vp:txn:${id}` };
  }

  async function expectForbidden(promise: Promise<any>, substring: string) {
    try {
      await promise;
      throw new Error('expected submitResponse to reject');
    } catch (e: any) {
      expect(e).toBeInstanceOf(ForbiddenException);
      expect(e.getResponse().error).toContain(substring);
    }
  }

  describe('top-level guards', () => {
    it('rejects when state is missing', async () => {
      const service = makeService();
      await expect(service.submitResponse({})).rejects.toThrow('missing state');
    });

    it('rejects an unknown/expired state', async () => {
      const service = makeService();
      await expect(service.submitResponse({ state: 'nope' })).rejects.toThrow(
        'unknown or expired state',
      );
    });

    it('rejects a transaction that is not pending', async () => {
      const service = makeService();
      const { state } = await seedTxn({ status: 'verified' });
      await expect(service.submitResponse({ state })).rejects.toThrow('transaction not pending');
    });

    it('rejects when the stored client_id does not match the unsigned redirect_uri invariant', async () => {
      const service = makeService();
      const { state } = await seedTxn({ clientId: 'redirect_uri:https://evil.example' });
      await expect(service.submitResponse({ state, vp_token: '{}' })).rejects.toThrow(
        'client_id/response_uri invariant violated',
      );
    });

    it('rejects a legacy-mode transaction whose client_id is not the bare response_uri', async () => {
      const service = makeService();
      const { state } = await seedTxn({ requestMode: 'legacy', clientId: 'https://wrong.example' });
      await expect(service.submitResponse({ state, vp_token: '{}' })).rejects.toThrow(
        'client_id/response_uri invariant violated',
      );
    });

    it('rejects when vp_token is missing', async () => {
      const service = makeService();
      const { state } = await seedTxn();
      await expectForbidden(service.submitResponse({ state }), 'missing vp_token');
    });
  });

  describe('DCQL branch', () => {
    it('happy path: ldp_vc VP object with an embedded credential', async () => {
      const service = makeService();
      const { state, key } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'ldp_vc' }] },
      });
      const vp = {
        type: ['VerifiablePresentation'],
        holder: 'did:rcw:holder-1',
        proof: { type: 'Ed25519Signature2020', proofPurpose: 'authentication', challenge: 'nonce-1', domain: clientId },
        verifiableCredential: [
          { type: ['VerifiableCredential'], credentialSubject: { id: 'did:rcw:holder-1', name: 'Alice' } },
        ],
      };
      credentials.verify.mockResolvedValue({ checks: [{ proof: 'OK', revoked: 'OK' }] });
      dcql.evaluate.mockReturnValue({ satisfied: true, matched: { c: { name: 'Alice' } } });

      const result = await service.submitResponse({ state, vp_token: JSON.stringify({ c: [vp] }) });
      expect(result).toEqual({ redirect_uri: null, status: 'ok' });
      expect(credentials.verify).toHaveBeenCalledWith(vp, { challenge: 'nonce-1', domain: clientId });

      const stored = await store.get<any>(key);
      expect(stored.status).toBe('verified');
      expect(stored.result.checks).toMatchObject({
        nonce: 'OK',
        audience: 'OK',
        holderSignature: 'OK',
        credentialSignatures: 'OK',
        holderBinding: 'OK',
        revocation: 'OK',
        dcql: 'OK',
      });
    });

    it('rejects when vp_token is not a DCQL-keyed object', async () => {
      const service = makeService();
      const { state } = await seedTxn();
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify(['bare-array']) }),
        'vp_token must be a DCQL-keyed object',
      );
    });

    it('rejects when a query id has no submitted presentation', async () => {
      const service = makeService();
      const { state } = await seedTxn({ dcqlQuery: { credentials: [{ id: 'c', format: 'ldp_vc' }] } });
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({}) }),
        "no presentation submitted for query 'c'",
      );
    });

    it('rejects when dcql.evaluate reports not satisfied', async () => {
      const service = makeService();
      const { state } = await seedTxn({ dcqlQuery: { credentials: [{ id: 'c', format: 'ldp_vc' }] } });
      const vp = {
        holder: 'did:rcw:holder-1',
        proof: {},
        verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: 'did:rcw:holder-1' } }],
      };
      credentials.verify.mockResolvedValue({ checks: [{ proof: 'OK', revoked: 'OK' }] });
      dcql.evaluate.mockReturnValue({ satisfied: false, reason: 'missing claim x' });
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [vp] }) }),
        'DCQL not satisfied: missing claim x',
      );
    });

    it('rejects when the embedded VC signature is invalid', async () => {
      const service = makeService();
      const { state } = await seedTxn({ dcqlQuery: { credentials: [{ id: 'c', format: 'ldp_vc' }] } });
      const vp = {
        holder: 'did:rcw:holder-1',
        proof: {},
        verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: 'did:rcw:holder-1' } }],
      };
      // First call = the VP's own proof (must pass to reach the embedded-VC loop); second = the embedded VC.
      credentials.verify
        .mockResolvedValueOnce({ checks: [{ proof: 'OK', revoked: 'OK' }] })
        .mockResolvedValueOnce({ checks: [{ proof: 'NOK', revoked: 'OK' }] });
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [vp] }) }),
        'embedded VC signature invalid',
      );
    });

    it('rejects when the embedded VC is revoked', async () => {
      const service = makeService();
      const { state } = await seedTxn({ dcqlQuery: { credentials: [{ id: 'c', format: 'ldp_vc' }] } });
      const vp = {
        holder: 'did:rcw:holder-1',
        proof: {},
        verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: 'did:rcw:holder-1' } }],
      };
      credentials.verify
        .mockResolvedValueOnce({ checks: [{ proof: 'OK', revoked: 'OK' }] })
        .mockResolvedValueOnce({ checks: [{ proof: 'OK', revoked: 'NOK' }] });
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [vp] }) }),
        'embedded VC revoked',
      );
    });

    it('rejects on holder-binding mismatch (subject id differs from the VP holder)', async () => {
      const service = makeService();
      const { state } = await seedTxn({ dcqlQuery: { credentials: [{ id: 'c', format: 'ldp_vc' }] } });
      const vp = {
        holder: 'did:rcw:holder-1',
        proof: {},
        verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: 'did:rcw:someone-else' } }],
      };
      credentials.verify.mockResolvedValue({ checks: [{ proof: 'OK', revoked: 'OK' }] });
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [vp] }) }),
        'holder binding failed',
      );
    });

    it('rejects on a VP proof nonce mismatch', async () => {
      const service = makeService();
      const { state } = await seedTxn({ dcqlQuery: { credentials: [{ id: 'c', format: 'ldp_vc' }] } });
      const vp = {
        holder: 'did:rcw:holder-1',
        proof: { challenge: 'wrong-nonce' },
        verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: 'did:rcw:holder-1' } }],
      };
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [vp] }) }),
        'nonce mismatch',
      );
    });

    it('rejects on a VP proof domain mismatch', async () => {
      const service = makeService();
      const { state } = await seedTxn({ dcqlQuery: { credentials: [{ id: 'c', format: 'ldp_vc' }] } });
      const vp = {
        holder: 'did:rcw:holder-1',
        proof: { domain: 'https://someone-else.example' },
        verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: 'did:rcw:holder-1' } }],
      };
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [vp] }) }),
        'audience mismatch',
      );
    });

    it('rejects a VP with no embedded verifiable credentials', async () => {
      const service = makeService();
      const { state } = await seedTxn({ dcqlQuery: { credentials: [{ id: 'c', format: 'ldp_vc' }] } });
      const vp = { holder: 'did:rcw:holder-1', proof: {}, verifiableCredential: [] };
      credentials.verify.mockResolvedValue({ checks: [{ proof: 'OK', revoked: 'OK' }] });
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [vp] }) }),
        'no verifiable credentials in VP',
      );
    });
  });

  describe('DCQL branch — real JWT-VP crypto (jwt_vc_json, did:jwk holder)', () => {
    async function makeHolder() {
      const { publicKey, privateKey } = await jose.generateKeyPair('ES256', { extractable: true });
      const publicJwk = await jose.exportJWK(publicKey);
      const holderDid = `did:jwk:${Buffer.from(JSON.stringify(publicJwk)).toString('base64url')}`;
      return { privateKey, holderDid };
    }

    it('verifies a real signed JWT-VP bound via a did:jwk kid', async () => {
      const service = makeService();
      const { state, key } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'jwt_vc_json' }] },
      });
      const { privateKey, holderDid } = await makeHolder();
      const vpJwt = await new jose.SignJWT({
        nonce: 'nonce-1',
        aud: clientId,
        vp: {
          verifiableCredential: [
            { type: ['VerifiableCredential'], credentialSubject: { id: holderDid, name: 'Alice' } },
          ],
        },
      })
        .setProtectedHeader({ alg: 'ES256', kid: `${holderDid}#0` })
        .sign(privateKey);

      credentials.verify.mockResolvedValue({ checks: [{ proof: 'OK', revoked: 'OK' }] });
      dcql.evaluate.mockReturnValue({ satisfied: true, matched: {} });

      const result = await service.submitResponse({ state, vp_token: JSON.stringify({ c: [vpJwt] }) });
      expect(result).toEqual({ redirect_uri: null, status: 'ok' });
      const stored = await store.get<any>(key);
      expect(stored.result.holderDid).toBe(holderDid);
      expect(stored.result.checks).toMatchObject({
        holderSignature: 'OK',
        nonce: 'OK',
        audience: 'OK',
      });
    });

    it('rejects a tampered JWT-VP signature', async () => {
      const service = makeService();
      const { state } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'jwt_vc_json' }] },
      });
      const { privateKey, holderDid } = await makeHolder();
      const vpJwt = await new jose.SignJWT({
        nonce: 'nonce-1',
        aud: clientId,
        vp: { verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: holderDid } }] },
      })
        .setProtectedHeader({ alg: 'ES256', kid: `${holderDid}#0` })
        .sign(privateKey);
      const [h, p, sig] = vpJwt.split('.');
      const sigBytes = Buffer.from(sig, 'base64url');
      sigBytes[Math.floor(sigBytes.length / 2)] ^= 0xff;
      const tampered = `${h}.${p}.${sigBytes.toString('base64url')}`;

      await expect(service.submitResponse({ state, vp_token: JSON.stringify({ c: [tampered] }) })).rejects.toBeInstanceOf(
        ForbiddenException,
      );
    });

    it('rejects a JWT-VP with the wrong nonce', async () => {
      const service = makeService();
      const { state } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'jwt_vc_json' }] },
      });
      const { privateKey, holderDid } = await makeHolder();
      const vpJwt = await new jose.SignJWT({
        nonce: 'wrong-nonce',
        aud: clientId,
        vp: { verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: holderDid } }] },
      })
        .setProtectedHeader({ alg: 'ES256', kid: `${holderDid}#0` })
        .sign(privateKey);

      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [vpJwt] }) }),
        'nonce mismatch',
      );
    });

    it('rejects a JWT-VP with the wrong audience', async () => {
      const service = makeService();
      const { state } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'jwt_vc_json' }] },
      });
      const { privateKey, holderDid } = await makeHolder();
      const vpJwt = await new jose.SignJWT({
        nonce: 'nonce-1',
        aud: 'redirect_uri:https://someone-else.example',
        vp: { verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: holderDid } }] },
      })
        .setProtectedHeader({ alg: 'ES256', kid: `${holderDid}#0` })
        .sign(privateKey);

      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [vpJwt] }) }),
        'audience mismatch',
      );
    });

    it('rejects when the embedded VC holder-binding subject differs from the JWT-VP signer', async () => {
      const service = makeService();
      const { state } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'jwt_vc_json' }] },
      });
      const { privateKey, holderDid } = await makeHolder();
      const vpJwt = await new jose.SignJWT({
        nonce: 'nonce-1',
        aud: clientId,
        vp: {
          verifiableCredential: [
            { type: ['VerifiableCredential'], credentialSubject: { id: 'did:rcw:someone-else' } },
          ],
        },
      })
        .setProtectedHeader({ alg: 'ES256', kid: `${holderDid}#0` })
        .sign(privateKey);
      credentials.verify.mockResolvedValue({ checks: [{ proof: 'OK', revoked: 'OK' }] });

      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [vpJwt] }) }),
        'holder binding failed',
      );
    });
  });

  describe('SD-JWT+KB branch (vc+sd-jwt)', () => {
    it('happy path delegates the whole check to credentials.verify', async () => {
      const service = makeService();
      const { state, key } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'vc+sd-jwt' }] },
      });
      const sdJwt = `${fakeJwt({ vct: 'TestVct', sub: 'did:rcw:holder-1' })}~`;
      credentials.verify.mockResolvedValue({ checks: [{ proof: 'OK', revoked: 'OK' }] });
      dcql.evaluate.mockReturnValue({ satisfied: true, matched: {} });

      const result = await service.submitResponse({ state, vp_token: JSON.stringify({ c: [sdJwt] }) });
      expect(result).toEqual({ redirect_uri: null, status: 'ok' });
      expect(credentials.verify).toHaveBeenCalledWith(sdJwt, { challenge: 'nonce-1', domain: clientId });
      const stored = await store.get<any>(key);
      expect(stored.result.holderDid).toBe('did:rcw:holder-1');
    });

    it('rejects when the SD-JWT+KB proof/nonce/audience check fails', async () => {
      const service = makeService();
      const { state } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'vc+sd-jwt' }] },
      });
      const sdJwt = `${fakeJwt({ vct: 'TestVct' })}~`;
      credentials.verify.mockResolvedValue({ checks: [{ proof: 'NOK' }] });
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [sdJwt] }) }),
        'SD-JWT+KB presentation invalid',
      );
    });

    it('rejects when the embedded VC is revoked', async () => {
      const service = makeService();
      const { state } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'vc+sd-jwt' }] },
      });
      const sdJwt = `${fakeJwt({ vct: 'TestVct' })}~`;
      credentials.verify.mockResolvedValue({ checks: [{ proof: 'OK', revoked: 'NOK' }] });
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: [sdJwt] }) }),
        'embedded VC revoked',
      );
    });
  });

  describe('mso_mdoc branch', () => {
    it('happy path delegates to buildSessionTranscript/verifyMdocPresentation', async () => {
      const service = makeService();
      const { state, key } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'mso_mdoc' }] },
      });
      (buildSessionTranscript as jest.Mock).mockReturnValue(Buffer.from('transcript'));
      (verifyMdocPresentation as jest.Mock).mockResolvedValue({
        verified: true,
        documents: [{ docType: 'org.iso.18013.5.1.mDL', claims: { 'org.iso.18013.5.1': { given_name: 'Alice' } } }],
      });
      dcql.evaluate.mockReturnValue({ satisfied: true, matched: {} });

      const result = await service.submitResponse({
        state,
        vp_token: JSON.stringify({ c: ['bW9ja19tZG9j'] }),
        mdoc_generated_nonce: 'mgn-1',
      });
      expect(result).toEqual({ redirect_uri: null, status: 'ok' });
      expect(buildSessionTranscript).toHaveBeenCalledWith('mgn-1', clientId, responseUri, 'nonce-1');
      const stored = await store.get<any>(key);
      expect(stored.result.checks).toMatchObject({ holderBinding: 'OK', credentialSignatures: 'OK' });
    });

    it('rejects when mdoc_generated_nonce is missing', async () => {
      const service = makeService();
      const { state } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'mso_mdoc' }] },
      });
      await expectForbidden(
        service.submitResponse({ state, vp_token: JSON.stringify({ c: ['bW9ja19tZG9j'] }) }),
        'missing mdoc_generated_nonce',
      );
    });

    it('rejects when verifyMdocPresentation reports not verified', async () => {
      const service = makeService();
      const { state } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'mso_mdoc' }] },
      });
      (buildSessionTranscript as jest.Mock).mockReturnValue(Buffer.from('transcript'));
      (verifyMdocPresentation as jest.Mock).mockResolvedValue({
        verified: false,
        documents: [],
        error: 'issuer signature invalid',
      });
      await expectForbidden(
        service.submitResponse({
          state,
          vp_token: JSON.stringify({ c: ['bW9ja19tZG9j'] }),
          mdoc_generated_nonce: 'mgn-1',
        }),
        'mdoc presentation invalid: issuer signature invalid',
      );
    });

    it('rejects when the mdoc DeviceResponse has no documents', async () => {
      const service = makeService();
      const { state } = await seedTxn({
        dcqlQuery: { credentials: [{ id: 'c', format: 'mso_mdoc' }] },
      });
      (buildSessionTranscript as jest.Mock).mockReturnValue(Buffer.from('transcript'));
      (verifyMdocPresentation as jest.Mock).mockResolvedValue({ verified: true, documents: [] });
      await expectForbidden(
        service.submitResponse({
          state,
          vp_token: JSON.stringify({ c: ['bW9ja19tZG9j'] }),
          mdoc_generated_nonce: 'mgn-1',
        }),
        'no documents in mdoc presentation',
      );
    });
  });

  describe('PEX branch', () => {
    function pexTxn(overrides: any = {}) {
      return seedTxn({
        queryMode: 'pex',
        dcqlQuery: undefined,
        presentationDefinition: { input_descriptors: [{ id: 'd1', constraints: { fields: [] } }] },
        ...overrides,
      });
    }

    it('happy path: single ldp_vc descriptor, satisfied', async () => {
      const service = makeService();
      const { state, key } = await pexTxn();
      const vp = {
        holder: 'did:rcw:holder-1',
        proof: { challenge: 'nonce-1', domain: clientId },
        verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: 'did:rcw:holder-1' } }],
      };
      credentials.verify.mockResolvedValue({ checks: [{ proof: 'OK', revoked: 'OK' }] });
      pex.evaluate.mockReturnValue({ satisfied: true, matched: { d1: {} } });

      const result = await service.submitResponse({
        state,
        vp_token: vp,
        presentation_submission: { descriptor_map: [{ id: 'd1', format: 'ldp_vc', path: '$' }] },
      });
      expect(result).toEqual({ redirect_uri: null, status: 'ok' });
      expect(pex.evaluate).toHaveBeenCalled();
      const stored = await store.get<any>(key);
      expect(stored.result.checks.pex).toBe('OK');
    });

    it('accepts a JSON-string presentation_submission (form-encoded shape)', async () => {
      const service = makeService();
      const { state } = await pexTxn();
      const vp = {
        holder: 'did:rcw:holder-1',
        proof: {},
        verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: 'did:rcw:holder-1' } }],
      };
      credentials.verify.mockResolvedValue({ checks: [{ proof: 'OK', revoked: 'OK' }] });
      pex.evaluate.mockReturnValue({ satisfied: true, matched: {} });

      const result = await service.submitResponse({
        state,
        vp_token: vp,
        presentation_submission: JSON.stringify({ descriptor_map: [{ id: 'd1', format: 'ldp_vc', path: '$' }] }),
      });
      expect(result).toEqual({ redirect_uri: null, status: 'ok' });
    });

    it('rejects an invalid presentation_submission (not JSON)', async () => {
      const service = makeService();
      const { state } = await pexTxn();
      await expectForbidden(
        service.submitResponse({ state, vp_token: {}, presentation_submission: '{not json' }),
        'presentation_submission is not valid JSON',
      );
    });

    it('rejects a missing/malformed presentation_submission', async () => {
      const service = makeService();
      const { state } = await pexTxn();
      await expectForbidden(
        service.submitResponse({ state, vp_token: {} }),
        'missing or invalid presentation_submission',
      );
    });

    it("rejects an unknown descriptor id in the submission", async () => {
      const service = makeService();
      const { state } = await pexTxn();
      await expectForbidden(
        service.submitResponse({
          state,
          vp_token: {},
          presentation_submission: { descriptor_map: [{ id: 'not-a-real-descriptor', format: 'ldp_vc', path: '$' }] },
        }),
        "unknown descriptor id 'not-a-real-descriptor'",
      );
    });

    it('rejects when the descriptor_map path does not resolve', async () => {
      const service = makeService();
      const { state } = await pexTxn();
      await expectForbidden(
        service.submitResponse({
          state,
          vp_token: {},
          presentation_submission: { descriptor_map: [{ id: 'd1', format: 'ldp_vc', path: '$[3]' }] },
        }),
        "presentation_submission path '$[3]' did not resolve",
      );
    });

    it('rejects when pex.evaluate reports not satisfied', async () => {
      const service = makeService();
      const { state } = await pexTxn();
      const vp = {
        holder: 'did:rcw:holder-1',
        proof: {},
        verifiableCredential: [{ type: ['VerifiableCredential'], credentialSubject: { id: 'did:rcw:holder-1' } }],
      };
      credentials.verify.mockResolvedValue({ checks: [{ proof: 'OK', revoked: 'OK' }] });
      pex.evaluate.mockReturnValue({ satisfied: false, reason: 'input_descriptor d1 not matched' });
      await expectForbidden(
        service.submitResponse({
          state,
          vp_token: vp,
          presentation_submission: { descriptor_map: [{ id: 'd1', format: 'ldp_vc', path: '$' }] },
        }),
        'PEX not satisfied: input_descriptor d1 not matched',
      );
    });
  });

  describe('private helpers', () => {
    function helperService() {
      return makeService() as any;
    }

    describe('resolveTopLevelPath', () => {
      it('$ resolves the first element', () => {
        const s = helperService();
        expect(s.resolveTopLevelPath(['a', 'b'], '$')).toBe('a');
      });

      it('$[n] resolves the nth element', () => {
        const s = helperService();
        expect(s.resolveTopLevelPath(['a', 'b', 'c'], '$[2]')).toBe('c');
      });

      it('throws for an unsupported path shape', () => {
        const s = helperService();
        expect(() => s.resolveTopLevelPath(['a'], '$.foo')).toThrow('unsupported presentation_submission path');
      });
    });

    describe('decodeForTraversal', () => {
      it('decodes a compact JWT to its vp claim', () => {
        const s = helperService();
        const jwt = fakeJwt({ vp: { foo: 'bar' } });
        expect(s.decodeForTraversal(jwt)).toEqual({ foo: 'bar' });
      });

      it('falls back to the whole claims object when there is no vp claim', () => {
        const s = helperService();
        const jwt = fakeJwt({ foo: 'bar' });
        expect(s.decodeForTraversal(jwt)).toEqual({ foo: 'bar' });
      });

      it('returns a non-string entry unchanged', () => {
        const s = helperService();
        const obj = { foo: 'bar' };
        expect(s.decodeForTraversal(obj)).toBe(obj);
      });
    });

    describe('normalizePexVpToken', () => {
      it('wraps a single object in an array', () => {
        const s = helperService();
        const obj = { a: 1 };
        expect(s.normalizePexVpToken(obj)).toEqual([obj]);
      });

      it('passes an array through unchanged', () => {
        const s = helperService();
        expect(s.normalizePexVpToken([1, 2])).toEqual([1, 2]);
      });

      it('parses a JSON-array string', () => {
        const s = helperService();
        expect(s.normalizePexVpToken('[1,2]')).toEqual([1, 2]);
      });

      it('treats a non-JSON string as a bare compact presentation', () => {
        const s = helperService();
        const raw = 'header.payload.signature';
        expect(s.normalizePexVpToken(raw)).toEqual([raw]);
      });
    });

    describe('resolveDescriptorMapEntry', () => {
      it('resolves a simple $ path with no nesting', () => {
        const s = helperService();
        expect(s.resolveDescriptorMapEntry(['presentation-1'], { id: 'd1', format: 'ldp_vc', path: '$' })).toEqual({
          value: 'presentation-1',
          format: 'ldp_vc',
        });
      });

      it('renames ldp_vp to ldp_vc and skips nesting entirely, even if path_nested is present', () => {
        const s = helperService();
        const result = s.resolveDescriptorMapEntry(['vp-1'], {
          id: 'd1',
          format: 'ldp_vp',
          path: '$',
          path_nested: { path: '$.credentialSubject' },
        });
        expect(result).toEqual({ value: 'vp-1', format: 'ldp_vc' });
      });

      it('walks a path_nested chain against a decoded JWT-VP', () => {
        const s = helperService();
        const jwt = fakeJwt({ vp: { verifiableCredential: ['vc-payload'] } });
        const result = s.resolveDescriptorMapEntry([jwt], {
          id: 'd1',
          format: 'jwt_vp',
          path: '$',
          path_nested: { path: '$.verifiableCredential[0]', format: 'jwt_vc_json' },
        });
        expect(result).toEqual({ value: 'vc-payload', format: 'jwt_vc_json' });
      });

      it('throws when path_nested is used with mso_mdoc', () => {
        const s = helperService();
        expect(() =>
          s.resolveDescriptorMapEntry(['mdoc-1'], {
            id: 'd1',
            format: 'mso_mdoc',
            path: '$',
            path_nested: { path: '$.x' },
          }),
        ).toThrow("path_nested is not supported for format 'mso_mdoc'");
      });

      it('throws when path_nested is used with vc+sd-jwt', () => {
        const s = helperService();
        expect(() =>
          s.resolveDescriptorMapEntry(['sdjwt-1'], {
            id: 'd1',
            format: 'vc+sd-jwt',
            path: '$',
            path_nested: { path: '$.x' },
          }),
        ).toThrow("path_nested is not supported for format 'vc+sd-jwt'");
      });

      it('throws when the top-level path does not resolve', () => {
        const s = helperService();
        expect(() => s.resolveDescriptorMapEntry([], { id: 'd1', format: 'ldp_vc', path: '$' })).toThrow(
          "presentation_submission path '$' did not resolve",
        );
      });

      it('throws when a nested path does not resolve', () => {
        const s = helperService();
        const jwt = fakeJwt({ vp: {} });
        expect(() =>
          s.resolveDescriptorMapEntry([jwt], {
            id: 'd1',
            format: 'jwt_vp',
            path: '$',
            path_nested: { path: '$.missing' },
          }),
        ).toThrow("presentation_submission path_nested '$.missing' did not resolve");
      });
    });
  });

  describe('getStatus', () => {
    it('returns the stored status and result', async () => {
      const service = makeService();
      const { id } = await seedTxn({ status: 'verified', result: { verified: true, checks: {} } });
      const status = await service.getStatus(id);
      expect(status).toEqual({ status: 'verified', verified: true, checks: {} });
    });

    it('throws NotFoundException for an unknown transaction id', async () => {
      const service = makeService();
      await expect(service.getStatus('nope')).rejects.toThrow(NotFoundException);
    });
  });
});
