// oid4vp.service.ts transitively imports @auth0/mdl (via mdoc-presentation.util.ts)
// for the mso_mdoc presentation path, which this suite doesn't exercise.
// @auth0/mdl's cose-kit dependency uses a Node "imports" subpath
// (#runtime/pkijs.js) that jest's resolver can't load, so stub it out here
// rather than pull the whole COSE/CBOR chain into these request-object tests.
jest.mock('@auth0/mdl', () => ({ Verifier: class {} }), { virtual: true });
jest.mock('@auth0/mdl/lib/cbor', () => ({ cborEncode: jest.fn(), DataItem: {} }), { virtual: true });

import { Oid4vpService } from './oid4vp.service';
import { MemoryStoreService } from '../session/memory-store.service';
import { DcqlService } from './dcql.service';
import { PexService } from './pex.service';

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
