import { AuthorityClaimSource } from './authority.claim-source';
import { ClaimSourceUnavailableError, SubjectNotFoundError } from './claim-source.interface';
import { resetAuthorityTokenProviders } from './authority-token';

/**
 * The Authority claim source, and specifically the linkage claim.
 *
 * The property worth defending is not that `authorityCredentialId` appears. It is
 * that it can appear ONLY from an Authority Service issuance response — a linkage a
 * caller could influence would let a credential nominate its own status anchor,
 * which is the same as having no status check at all.
 */

const PROFILE = 'p-farmer-uuid';
const BINDING = 'b-farmer-uuid';
const OSID = '1-abc';
const CREDENTIAL_ID = 'did:rcw:046a5fe1-32f2-4e43-8537-8d7177fc2269';

const REQUEST = {
  subjectId: 'NAT-91018472',
  subjectClaim: 'nationalId',
  credentialConfigurationId: 'farmer-identity-credential',
  credentialName: 'Farmer Identity Credential',
  properties: ['farmerReference', 'registrationStatus', 'authorityCredentialId'],
  required: ['farmerReference'],
};

const ISSUED = {
  credentialId: CREDENTIAL_ID,
  state: 'ISSUED',
  claims: { farmerReference: 'did:web:a#farmer/TN/FRM-TN-0041', registrationStatus: true },
};

/** Stands in for the Authority Service. Records every request for assertions. */
function authority(overrides: Record<string, any> = {}) {
  const calls: { url: string; init: any }[] = [];
  const reply = (body: any, status = 200) => ({
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
    text: async () => JSON.stringify(body),
  });
  const impl = async (url: string, init: any) => {
    calls.push({ url: String(url), init });
    const u = String(url);
    if (u.includes('/credential-profiles/') && u.endsWith('/issue')) {
      return overrides.issue ? overrides.issue(reply) : reply(ISSUED, 201);
    }
    if (u.includes('/records/search')) {
      return overrides.search ? overrides.search(reply) : reply({ data: [{ osid: OSID }] });
    }
    if (u.includes('/credential-profiles/')) {
      return overrides.profile ? overrides.profile(reply) : reply({ registryBindingId: BINDING });
    }
    return reply({}, 404);
  };
  return { calls, impl };
}

function configure(env: Record<string, string> = {}) {
  process.env.AUTHORITY_BASE_URL = 'http://authority:3334';
  // A JSON object, as every other map in this service's configuration is.
  process.env.AUTHORITY_PROFILE_MAP = JSON.stringify({ 'farmer-identity-credential': PROFILE });
  process.env.AUTHORITY_SUBJECT_FIELD = 'nationalId';
  Object.assign(process.env, env);
}

describe('AuthorityClaimSource', () => {
  const saved = { ...process.env };
  let fetchSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.resetModules();
    configure();
  });
  afterEach(() => {
    fetchSpy?.mockRestore();
    process.env = { ...saved };
  });

  const withAuthority = (o: Record<string, any> = {}) => {
    const a = authority(o);
    fetchSpy = jest.spyOn(global, 'fetch' as any).mockImplementation(a.impl as any);
    return a;
  };

  it('carries the Authority credential identifier into the claims', async () => {
    withAuthority();
    const result = await new AuthorityClaimSource().resolve(REQUEST as any);
    expect(result.claims.authorityCredentialId).toBe(CREDENTIAL_ID);
    expect(result.claims.farmerReference).toBe(ISSUED.claims.farmerReference);
    expect(result.missing).toEqual([]);
  });

  it('takes the identifier only from the issuance response, never from the request', async () => {
    // A caller-supplied value must not survive. If it could, a credential could name
    // its own status anchor and the check would confirm whatever that anchor says.
    withAuthority();
    const hostile = {
      ...REQUEST,
      authorityCredentialId: 'did:rcw:attacker-controlled',
      claims: { authorityCredentialId: 'did:rcw:attacker-controlled' },
    };
    const result = await new AuthorityClaimSource().resolve(hostile as any);
    expect(result.claims.authorityCredentialId).toBe(CREDENTIAL_ID);
  });

  it('refuses to issue when the Authority returns no identifier', async () => {
    // A credential with no linkage cannot be status-checked, and one that cannot be
    // checked must not be minted rather than silently unverifiable later.
    withAuthority({ issue: (reply: any) => reply({ state: 'ISSUED' }, 201) });
    await expect(new AuthorityClaimSource().resolve(REQUEST as any)).rejects.toThrow(
      /issued no credential identifier/,
    );
  });

  it('is idempotent: the key is derived from what is issued, not from when', async () => {
    const a = withAuthority();
    await new AuthorityClaimSource().resolve(REQUEST as any);
    const first = JSON.parse(a.calls.find((c) => c.url.endsWith('/issue'))!.init.body);

    const b = withAuthority();
    await new AuthorityClaimSource().resolve(REQUEST as any);
    const second = JSON.parse(b.calls.find((c) => c.url.endsWith('/issue'))!.init.body);

    expect(first.idempotencyKey).toBe(second.idempotencyKey);
    expect(first.osid).toBe(OSID);
    // The subject identifier is hashed into the key rather than sent as itself.
    expect(first.idempotencyKey).not.toContain(REQUEST.subjectId);
  });

  it('gives a different key for a different subject', async () => {
    const a = withAuthority();
    await new AuthorityClaimSource().resolve(REQUEST as any);
    const first = JSON.parse(a.calls.find((c) => c.url.endsWith('/issue'))!.init.body);
    const b = withAuthority();
    await new AuthorityClaimSource().resolve({ ...REQUEST, subjectId: 'NAT-OTHER' } as any);
    const second = JSON.parse(b.calls.find((c) => c.url.endsWith('/issue'))!.init.body);
    expect(first.idempotencyKey).not.toBe(second.idempotencyKey);
  });

  it('discloses only attributes the credential type declares', async () => {
    // Claim minimisation. The Authority returning more than was asked for must not
    // widen the credential, exactly as an issuer's own endpoint cannot.
    withAuthority({
      issue: (reply: any) =>
        reply(
          {
            credentialId: CREDENTIAL_ID,
            claims: {
              farmerReference: 'ref',
              registrationStatus: true,
              nationalId: 'NAT-91018472',
              district: 'Madurai',
            },
          },
          201,
        ),
    });
    const result = await new AuthorityClaimSource().resolve(REQUEST as any);
    expect(Object.keys(result.claims).sort()).toEqual(
      ['authorityCredentialId', 'farmerReference', 'registrationStatus'].sort(),
    );
    expect(JSON.stringify(result.claims)).not.toContain('Madurai');
  });

  it('reports a subject with no record as not found, not as a fault', async () => {
    withAuthority({ search: (reply: any) => reply({ data: [] }) });
    await expect(new AuthorityClaimSource().resolve(REQUEST as any)).rejects.toBeInstanceOf(
      SubjectNotFoundError,
    );
  });

  it('refuses an ambiguous match rather than choosing one', async () => {
    withAuthority({ search: (reply: any) => reply({ data: [{ osid: '1-a' }, { osid: '1-b' }] }) });
    await expect(new AuthorityClaimSource().resolve(REQUEST as any)).rejects.toThrow(
      /refusing to choose/,
    );
  });

  it('fails closed when the Authority is unreachable or slow', async () => {
    fetchSpy = jest.spyOn(global, 'fetch' as any).mockImplementation(async () => {
      throw new Error('connect ECONNREFUSED');
    });
    await expect(new AuthorityClaimSource().resolve(REQUEST as any)).rejects.toBeInstanceOf(
      ClaimSourceUnavailableError,
    );
  });

  it('fails closed when the Authority refuses the issuance', async () => {
    withAuthority({ issue: (reply: any) => reply({ code: 'FORBIDDEN' }, 403) });
    await expect(new AuthorityClaimSource().resolve(REQUEST as any)).rejects.toThrow(/403/);
  });

  it('refuses a credential type with no Authority profile mapped', async () => {
    withAuthority();
    await expect(
      new AuthorityClaimSource().resolve({
        ...REQUEST,
        credentialConfigurationId: 'unmapped',
        credentialName: 'Unmapped Credential',
      } as any),
    ).rejects.toThrow(/no Authority credential profile is mapped/);
  });

  it('does not forward a holder token, and authenticates as the service', async () => {
    process.env.AUTHORITY_TOKEN = 'service-token';
    const a = withAuthority();
    await new AuthorityClaimSource().resolve(REQUEST as any);
    for (const call of a.calls) {
      expect(call.init.headers.authorization).toBe('Bearer service-token');
    }
  });

  it('searches on the configured subject field', async () => {
    process.env.AUTHORITY_SUBJECT_FIELD = 'farmerId';
    const a = withAuthority();
    await new AuthorityClaimSource().resolve(REQUEST as any);
    const search = a.calls.find((c) => c.url.includes('/records/search'))!;
    expect(JSON.parse(search.init.body).filters).toHaveProperty('farmerId');
  });
});

describe('authenticating with client credentials', () => {
  const saved = { ...process.env };
  let fetchSpy: jest.SpyInstance;
  beforeEach(() => {
    jest.resetModules();
    // Providers are cached per (endpoint, client) for the life of the module, which is
    // what makes them shared across per-request claim sources. Tests must start from an
    // empty cache or a token minted by the previous test is still valid here.
    resetAuthorityTokenProviders();
    configure({
      AUTHORITY_TOKEN_URL: 'http://keycloak:8080/auth/realms/authority/protocol/openid-connect/token',
      AUTHORITY_CLIENT_ID: 'agri-farmer-officer',
      AUTHORITY_CLIENT_SECRET: 'sekrit',
    });
  });
  afterEach(() => {
    fetchSpy?.mockRestore();
    process.env = { ...saved };
  });

  /** The Authority, plus the IdP in front of it. */
  function stack(opts: { rejectFirst?: boolean } = {}) {
    const a = authority();
    let minted = 0;
    let rejected = false;
    const calls: string[] = [];
    const impl = async (url: string, init: any) => {
      const u = String(url);
      calls.push(u);
      if (u.includes('/openid-connect/token')) {
        minted += 1;
        const body = { access_token: `tok-${minted}`, expires_in: 60 };
        return { ok: true, status: 200, json: async () => body, text: async () => '' };
      }
      if (opts.rejectFirst && !rejected && u.endsWith('/issue')) {
        rejected = true;
        return { ok: false, status: 401, text: async () => 'expired', json: async () => ({}) };
      }
      return a.impl(u, init);
    };
    fetchSpy = jest.spyOn(global, 'fetch' as any).mockImplementation(impl as any);
    return { authorityCalls: a.calls, minted: () => minted, calls };
  }

  it('presents a token it obtained, not one from configuration', async () => {
    const s = stack();
    const result = await new AuthorityClaimSource().resolve(REQUEST as any);
    expect(result.claims.authorityCredentialId).toBe(CREDENTIAL_ID);
    for (const call of s.authorityCalls) {
      expect(call.init.headers.authorization).toBe('Bearer tok-1');
    }
  });

  it('obtains the token once and reuses it across the calls of one issuance', async () => {
    const s = stack();
    await new AuthorityClaimSource().resolve(REQUEST as any);
    expect(s.minted()).toBe(1);
  });

  it('re-acquires and retries when the Authority refuses the token', async () => {
    // A revoked token, or clocks apart by more than the renewal margin. One retry is the
    // difference between a transient mismatch and a holder being told issuance failed.
    const s = stack({ rejectFirst: true });
    const result = await new AuthorityClaimSource().resolve(REQUEST as any);
    expect(result.claims.authorityCredentialId).toBe(CREDENTIAL_ID);
    expect(s.minted()).toBe(2);
  });

  it('never sends the client secret to the Authority', async () => {
    const s = stack();
    await new AuthorityClaimSource().resolve(REQUEST as any);
    const toAuthority = JSON.stringify(s.authorityCalls);
    expect(toAuthority).not.toContain('sekrit');
  });
});

describe('authenticating to the Authority Service', () => {
  const saved = { ...process.env };
  let fetchSpy: jest.SpyInstance;
  beforeEach(() => configure());
  afterEach(() => {
    fetchSpy?.mockRestore();
    process.env = { ...saved };
  });

  it('sends whatever headers the deployment configures', async () => {
    // Not every Authority authenticates with a bearer token. The issuer must be able to
    // identify itself without this code knowing which scheme is in use.
    process.env.AUTHORITY_HEADERS = JSON.stringify({
      'x-dev-issuer': 'https://idp.test',
      'x-dev-subject': 'agri-farmer-officer',
    });
    const a = authority();
    fetchSpy = jest.spyOn(global, 'fetch' as any).mockImplementation(a.impl as any);
    await new AuthorityClaimSource().resolve(REQUEST as any);
    for (const call of a.calls) {
      expect(call.init.headers['x-dev-subject']).toBe('agri-farmer-officer');
    }
  });

  it('still never forwards anything belonging to the holder', async () => {
    process.env.AUTHORITY_HEADERS = JSON.stringify({ 'x-dev-subject': 'issuing-officer' });
    const a = authority();
    fetchSpy = jest.spyOn(global, 'fetch' as any).mockImplementation(a.impl as any);
    await new AuthorityClaimSource().resolve(REQUEST as any);
    const sent = JSON.stringify(a.calls.map((c) => c.init.headers));
    expect(sent).not.toContain(REQUEST.subjectId);
  });
});
