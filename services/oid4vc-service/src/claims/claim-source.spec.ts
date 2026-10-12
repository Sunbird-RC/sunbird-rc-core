import * as http from 'http';
import type { AddressInfo } from 'net';
import { ClaimSourceFactory } from './claim-source.factory';
import { HttpClaimSource } from './http.claim-source';
import { RegistryClaimSource } from './registry.claim-source';
import {
  ClaimSourceNotConfiguredError,
  ClaimSourceUnavailableError,
  SubjectNotFoundError,
  type ClaimRequest,
} from './claim-source.interface';

// Claim sources: where a credential's values come from.
//
// The property these tests exist for is that ISSUING AND RECORD-KEEPING ARE
// SEPARABLE. An authority may hold its farmers in its own database and never adopt
// the Sunbird registry, and must still be able to issue — so the registry has to be
// one option here, not a precondition.
describe('claim sources', () => {
  const ORIGINAL_ENV = process.env;

  /** The credential type under test: three declared attributes, two required. */
  const REQ: ClaimRequest = {
    subjectId: 'FRM-000123',
    subjectClaim: 'farmer_id',
    credentialConfigurationId: 'did:schema:farmer',
    credentialName: 'Farmer Land Holding',
    properties: ['name', 'landAreaAcres', 'primaryCrop'],
    required: ['name', 'landAreaAcres'],
  };

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    // Not set by default: these tests must not silently depend on the registry.
    delete process.env.REGISTRY_BASE_URL;
    delete process.env.CLAIM_SOURCE_MAP;
    delete process.env.CLAIM_SOURCE_DEFAULT;
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  // --- a stand-in for the issuing authority's own system ---------------------

  let server: http.Server;
  let received: any[] = [];

  /** Starts a throwaway endpoint standing in for the authority's own API. */
  const startEndpoint = async (
    handler: (body: any, res: http.ServerResponse) => void,
  ): Promise<string> => {
    received = [];
    server = http.createServer((req, res) => {
      let raw = '';
      req.on('data', (c) => (raw += c));
      req.on('end', () => {
        const body = raw ? JSON.parse(raw) : {};
        received.push({ body, headers: req.headers });
        handler(body, res);
      });
    });
    await new Promise<void>((r) => server.listen(0, '127.0.0.1', r));
    return `http://127.0.0.1:${(server.address() as AddressInfo).port}/claims`;
  };

  const json = (res: http.ServerResponse, status: number, payload: unknown) => {
    res.writeHead(status, { 'content-type': 'application/json' });
    res.end(JSON.stringify(payload));
  };

  afterEach(async () => {
    if (server?.listening) await new Promise<void>((r) => server.close(() => r()));
  });

  const httpSource = (url: string, extra: Record<string, unknown> = {}) =>
    new HttpClaimSource({ name: 'agri', url, timeoutMs: 2000, ...extra });

  describe('http claim source', () => {
    it('issues from the authority’s own database, with no registry configured', async () => {
      // THE REQUIREMENT. Nothing about the Sunbird registry is set here.
      const url = await startEndpoint((_b, res) =>
        json(res, 200, { claims: { name: 'Ravi Kumar', landAreaAcres: 4.5, primaryCrop: 'Wheat' } }),
      );

      const result = await httpSource(url).resolve(REQ);

      expect(result.missing).toEqual([]);
      expect(result.claims).toEqual({
        name: 'Ravi Kumar',
        landAreaAcres: 4.5,
        primaryCrop: 'Wheat',
      });
    });

    it('sends the subject from the token and the attributes being asked for', async () => {
      const url = await startEndpoint((_b, res) =>
        json(res, 200, { name: 'Ravi Kumar', landAreaAcres: 4.5 }),
      );

      await httpSource(url).resolve(REQ);

      expect(received[0].body).toEqual({
        subjectId: 'FRM-000123',
        subjectClaim: 'farmer_id',
        credentialConfigurationId: 'did:schema:farmer',
        credentialName: 'Farmer Land Holding',
        attributes: ['name', 'landAreaAcres', 'primaryCrop'],
      });
    });

    it('accepts a bare record, so the simplest implementation works', async () => {
      // An authority returning its row unwrapped should not need a wrapper object.
      const url = await startEndpoint((_b, res) =>
        json(res, 200, { name: 'Ravi Kumar', landAreaAcres: 4.5 }),
      );

      const result = await httpSource(url).resolve(REQ);

      expect(result.claims.name).toBe('Ravi Kumar');
      expect(result.missing).toEqual([]);
    });

    it('DROPS fields the credential type does not declare', async () => {
      // The security property. A compromised or buggy endpoint must not be able to
      // put claims into a credential that its schema never defined — least of all
      // ones that look authoritative.
      const url = await startEndpoint((_b, res) =>
        json(res, 200, {
          name: 'Ravi Kumar',
          landAreaAcres: 4.5,
          isGovernmentOfficial: true,
          nationalIdNumber: '1234-5678',
        }),
      );

      const result = await httpSource(url).resolve(REQ);

      expect(result.claims).not.toHaveProperty('isGovernmentOfficial');
      expect(result.claims).not.toHaveProperty('nationalIdNumber');
      expect(Object.keys(result.claims).sort()).toEqual(['landAreaAcres', 'name']);
    });

    it('reports required attributes the authority did not return, by name', async () => {
      const url = await startEndpoint((_b, res) => json(res, 200, { name: 'Ravi Kumar' }));

      const result = await httpSource(url).resolve(REQ);

      expect(result.missing).toEqual(['landAreaAcres']);
    });

    it('sends a bearer token when one is configured', async () => {
      const url = await startEndpoint((_b, res) =>
        json(res, 200, { name: 'Ravi Kumar', landAreaAcres: 4.5 }),
      );

      await httpSource(url, { token: 's3cret' }).resolve(REQ);

      expect(received[0].headers.authorization).toBe('Bearer s3cret');
    });

    it('distinguishes “no such holder” from “the system is down”', async () => {
      // These need different messages: one is a provisioning gap for one holder,
      // the other is the authority's system failing. Collapsing them sends whoever
      // is debugging to the wrong place.
      const notFound = await startEndpoint((_b, res) => json(res, 404, { error: 'unknown' }));
      await expect(httpSource(notFound).resolve(REQ)).rejects.toThrow(SubjectNotFoundError);
      await new Promise<void>((r) => server.close(() => r()));

      const broken = await startEndpoint((_b, res) => json(res, 500, { error: 'boom' }));
      await expect(httpSource(broken).resolve(REQ)).rejects.toThrow(ClaimSourceUnavailableError);
    });

    it('gives up on a slow endpoint instead of hanging the wallet', async () => {
      const url = await startEndpoint(() => {
        /* never responds */
      });

      await expect(httpSource(url, { timeoutMs: 150 }).resolve(REQ)).rejects.toThrow(
        /did not respond within 150ms/,
      );
    });

    it('refuses plain http to another organisation, before sending anything', async () => {
      // A claim source is a different organisation's system, so the call leaves our
      // network: the holder's identifier goes out and their personal data comes
      // back. That is a different trust boundary from `REGISTRY_BASE_URL=
      // http://registry:8081`, which is a sibling service whose traffic never
      // leaves the cluster — so the weaker standard must not be borrowed for it.
      const source = httpSource('http://agri.example.gov/claims');

      await expect(source.resolve(REQ)).rejects.toThrow(/refusing to send holder data/);
      await expect(source.resolve(REQ)).rejects.toThrow(/use https/);
    });

    it('rejects a URL that is not a URL at all', async () => {
      const source = new HttpClaimSource({ name: 'agri', url: 'not-a-url' });

      await expect(source.resolve(REQ)).rejects.toThrow(/is not a valid URL/);
    });

    it('rejects a response that is not a JSON object of claims', async () => {
      const url = await startEndpoint((_b, res) => json(res, 200, ['not', 'an', 'object']));

      await expect(httpSource(url).resolve(REQ)).rejects.toThrow(/not a JSON object of claims/);
    });
  });

  // --- selection -------------------------------------------------------------

  describe('factory', () => {
    const registryStub = (enabled: boolean) =>
      new RegistryClaimSource({ enabled, subjectSources: jest.fn() } as any);

    const target = { schemaId: 'did:schema:farmer', name: 'Farmer Land Holding' };

    it('sends one credential type to the authority’s API and another to the registry', async () => {
      // The multi-issuer case: two authorities, two systems of record, one
      // deployment. This is why selection is per credential type.
      process.env.CLAIM_SOURCE_AGRI_URL = 'https://agri.example.gov/claims';
      process.env.CLAIM_SOURCE_MAP = JSON.stringify({ 'Farmer Land Holding': 'agri' });
      process.env.REGISTRY_BASE_URL = 'http://registry:8081';

      const factory = new ClaimSourceFactory(registryStub(true));

      expect(factory.for(target).name).toBe('agri');
      expect(factory.for({ schemaId: 'did:schema:edu', name: 'Education' }).name).toBe('registry');
    });

    it('prefers a schemaId mapping over a name mapping', async () => {
      // Two credential types can share a display name; an id cannot be ambiguous,
      // and picking the wrong one would issue from the wrong authority's data.
      process.env.CLAIM_SOURCE_AGRI_URL = 'https://agri.example.gov/claims';
      process.env.CLAIM_SOURCE_MAP = JSON.stringify({
        'did:schema:farmer': 'agri',
        'Farmer Land Holding': 'registry',
      });
      process.env.REGISTRY_BASE_URL = 'http://registry:8081';

      expect(new ClaimSourceFactory(registryStub(true)).for(target).name).toBe('agri');
    });

    it('treats CLAIM_SOURCE_DEFAULT=none as a decision, not a misconfiguration', async () => {
      // A deployment may issue only through POST /oid4vc/offer, where the caller
      // supplies claims. The error has to read as "not offered here", and must not
      // demand the registry be configured.
      process.env.CLAIM_SOURCE_DEFAULT = 'none';

      const factory = new ClaimSourceFactory(registryStub(false));

      expect(() => factory.for(target)).toThrow(ClaimSourceNotConfiguredError);
      expect(() => factory.for(target)).toThrow(/POST \/oid4vc\/offer/);
    });

    it('names the mistake when a mapping points at an undeclared source', async () => {
      process.env.CLAIM_SOURCE_MAP = JSON.stringify({ 'Farmer Land Holding': 'typo' });

      expect(() => new ClaimSourceFactory(registryStub(true)).for(target)).toThrow(
        /CLAIM_SOURCE_TYPO_URL/,
      );
    });

    it('defaults to the registry, so deployments that predate this keep working', async () => {
      process.env.REGISTRY_BASE_URL = 'http://registry:8081';

      expect(new ClaimSourceFactory(registryStub(true)).for(target).name).toBe('registry');
    });

    it('ignores an http source declared without a URL', async () => {
      // Registering it would defer the failure to the first holder who tries to
      // collect, reported as their problem rather than the operator's.
      process.env.CLAIM_SOURCE_MAP = JSON.stringify({ 'Farmer Land Holding': 'agri' });
      jest.spyOn(console, 'warn').mockImplementation(() => {});

      expect(() => new ClaimSourceFactory(registryStub(true)).for(target)).toThrow(
        /no such claim source/,
      );
    });
  });
});
