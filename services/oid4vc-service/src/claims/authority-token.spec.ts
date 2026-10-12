import {
  AuthorityTokenProvider,
  authorityTokenProvider,
  resetAuthorityTokenProviders,
} from './authority-token';

/**
 * Token acquisition for the Authority Service.
 *
 * The property under test is not "a token is obtained" — it is that the issuing service
 * keeps working AFTER the first token expires. A static token passes any test that runs
 * inside its lifetime, which is exactly why the gap survived review the first time.
 */

const CREDENTIALS = {
  tokenUrl: 'http://keycloak:8080/auth/realms/authority/protocol/openid-connect/token',
  clientId: 'agri-farmer-officer',
  clientSecret: 's3cret',
};

/** An IdP that mints a distinguishable token each time and counts the exchanges. */
function idp(overrides: { expiresIn?: number; fail?: number; body?: any } = {}) {
  let issued = 0;
  const impl = jest.fn(async (_url: string, init: any) => {
    issued += 1;
    if (overrides.fail && issued <= overrides.fail) {
      return {
        ok: false,
        status: 401,
        text: async () => '{"error":"invalid_client"}',
        json: async () => ({ error: 'invalid_client' }),
      };
    }
    const body =
      overrides.body ??
      { access_token: `token-${issued}`, expires_in: overrides.expiresIn ?? 60 };
    return { ok: true, status: 200, json: async () => body, text: async () => JSON.stringify(body) };
  });
  return { impl, calls: () => issued, lastInit: () => impl.mock.calls.at(-1)?.[1] };
}

describe('AuthorityTokenProvider', () => {
  let spy: jest.SpyInstance;
  beforeEach(() => resetAuthorityTokenProviders());
  afterEach(() => {
    spy?.mockRestore();
    jest.useRealTimers();
  });

  const withIdp = (o = {}) => {
    const i = idp(o);
    spy = jest.spyOn(global, 'fetch' as any).mockImplementation(i.impl as any);
    return i;
  };

  it('exchanges client credentials for a token', async () => {
    const i = withIdp();
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    expect(await provider.get()).toBe('token-1');

    const body = String(i.lastInit().body);
    expect(body).toContain('grant_type=client_credentials');
    expect(body).toContain('client_id=agri-farmer-officer');
  });

  it('reuses the token while it is still valid', async () => {
    const i = withIdp({ expiresIn: 300 });
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    await provider.get();
    await provider.get();
    await provider.get();
    expect(i.calls()).toBe(1);
  });

  it('re-acquires once the token has aged out', async () => {
    // The whole point. A provider that cannot do this is a static token with extra steps.
    const i = withIdp({ expiresIn: 60 });
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    expect(await provider.get()).toBe('token-1');

    // 55s later the 60s token is inside its renewal margin.
    const realNow = Date.now;
    Date.now = () => realNow() + 55_000;
    try {
      expect(await provider.get()).toBe('token-2');
      expect(i.calls()).toBe(2);
    } finally {
      Date.now = realNow;
    }
  });

  it('renews early, so a token is never used at the instant it expires', async () => {
    const i = withIdp({ expiresIn: 60 });
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    await provider.get();
    const realNow = Date.now;
    // 51s in: past the 50s renewal point, still 9s of nominal life left.
    Date.now = () => realNow() + 51_000;
    try {
      expect(await provider.get()).toBe('token-2');
    } finally {
      Date.now = realNow;
    }
    expect(i.calls()).toBe(2);
  });

  it('keeps a usable window even for a very short-lived token', async () => {
    // A 6-second token must not be treated as already dead by a fixed 10-second margin.
    const i = withIdp({ expiresIn: 6 });
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    expect(await provider.get()).toBe('token-1');
    expect(await provider.get()).toBe('token-1');
    expect(i.calls()).toBe(1);
  });

  it('collapses concurrent callers into one exchange', async () => {
    // Two credentials collected together must not each start their own exchange.
    const i = withIdp();
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    const [a, b, c] = await Promise.all([provider.get(), provider.get(), provider.get()]);
    expect([a, b, c]).toEqual(['token-1', 'token-1', 'token-1']);
    expect(i.calls()).toBe(1);
  });

  it('acquires again after invalidate()', async () => {
    const i = withIdp({ expiresIn: 300 });
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    expect(await provider.get()).toBe('token-1');
    provider.invalidate();
    expect(await provider.get()).toBe('token-2');
    expect(i.calls()).toBe(2);
  });

  it('reports a refused exchange with the IdP’s own reason', async () => {
    withIdp({ fail: 99 });
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    await expect(provider.get()).rejects.toThrow(/401.*invalid_client/s);
  });

  it('does not cache a failure: a later attempt can still succeed', async () => {
    const i = withIdp({ fail: 1 });
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    await expect(provider.get()).rejects.toThrow(/401/);
    expect(await provider.get()).toBe('token-2');
    expect(i.calls()).toBe(2);
  });

  it('reports an unreachable token endpoint rather than hanging', async () => {
    spy = jest.spyOn(global, 'fetch' as any).mockImplementation(async () => {
      throw new Error('connect ECONNREFUSED');
    });
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    await expect(provider.get()).rejects.toThrow(/could not reach the token endpoint/);
  });

  it('refuses a response with no access_token', async () => {
    withIdp({ body: { expires_in: 60 } });
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    await expect(provider.get()).rejects.toThrow(/no access_token/);
  });

  it('never puts the secret in an error', async () => {
    withIdp({ fail: 99 });
    const provider = new AuthorityTokenProvider(CREDENTIALS);
    const error = await provider.get().catch((e) => String(e));
    expect(error).not.toContain('s3cret');
  });
});

describe('authorityTokenProvider()', () => {
  beforeEach(() => resetAuthorityTokenProviders());

  it('is null when no client credentials are configured', () => {
    expect(authorityTokenProvider({})).toBeNull();
  });

  it('builds a provider when all three are present', () => {
    expect(authorityTokenProvider(CREDENTIALS)).toBeInstanceOf(AuthorityTokenProvider);
  });

  it('returns the SAME provider for the same client', () => {
    // The claim source is constructed per credential request. A per-instance provider
    // would be an empty cache every time, so the token cache would never once be read
    // and every issuance would mint fresh tokens.
    expect(authorityTokenProvider(CREDENTIALS)).toBe(authorityTokenProvider(CREDENTIALS));
  });

  it('keeps separate providers for separate clients', () => {
    // Two Authorities, two officers, two tokens. Sharing one would have an issuer acting
    // as the other's officer.
    const farmer = authorityTokenProvider(CREDENTIALS);
    const land = authorityTokenProvider({ ...CREDENTIALS, clientId: 'agri-land-officer' });
    expect(farmer).not.toBe(land);
  });

  it('a shared provider means one exchange across separate claim sources', async () => {
    const i = idp({ expiresIn: 300 });
    const spy = jest.spyOn(global, 'fetch' as any).mockImplementation(i.impl as any);
    try {
      await authorityTokenProvider(CREDENTIALS)!.get();
      await authorityTokenProvider(CREDENTIALS)!.get();
      await authorityTokenProvider(CREDENTIALS)!.get();
      expect(i.calls()).toBe(1);
    } finally {
      spy.mockRestore();
    }
  });

  it('refuses a half-configured client rather than falling back silently', () => {
    // Falling back would make every issuance fail at the Authority with a 401, which
    // points an operator at permissions instead of at the missing secret.
    expect(() =>
      authorityTokenProvider({ tokenUrl: CREDENTIALS.tokenUrl, clientId: 'x' }),
    ).toThrow(/AUTHORITY_CLIENT_SECRET/);
  });

  it('names every missing setting, not just the first', () => {
    expect(() => authorityTokenProvider({ clientId: 'x' })).toThrow(
      /AUTHORITY_TOKEN_URL.*AUTHORITY_CLIENT_SECRET/s,
    );
  });
});
