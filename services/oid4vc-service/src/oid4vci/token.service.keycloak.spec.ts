import { UnauthorizedException } from '@nestjs/common';
import { createServer, Server } from 'node:http';
import * as jose from 'jose';
import { TokenService } from './token.service';

// Verifies Keycloak token acceptance against a REAL JWK set served over HTTP,
// rather than by stubbing jose. That matters because the thing most likely to be
// wrong here is not the happy path but the plumbing around it: which URL the keys
// are fetched from, which `iss` is expected, and whether a token that is merely
// well-formed gets in.
//
// The public/internal URL split is exercised deliberately — `iss` is checked
// against the browser-facing URL while keys are fetched from the in-cluster one,
// and conflating those is the classic Keycloak-behind-a-gateway failure.
describe('TokenService Keycloak validation', () => {
  const ORIGINAL_ENV = process.env;
  const REALM = 'sunbird-rc';
  // The browser-facing base, as published in issuer metadata. Intentionally NOT
  // the address the keys are served from.
  const PUBLIC_BASE = 'https://issuer.example/auth';
  const ISSUER = `${PUBLIC_BASE}/realms/${REALM}`;

  let server: Server;
  let internalBase: string;
  let signKey: jose.KeyLike;
  let otherKey: jose.KeyLike;
  let identity: any;

  beforeAll(async () => {
    const pair = await jose.generateKeyPair('RS256');
    signKey = pair.privateKey;
    const jwk = await jose.exportJWK(pair.publicKey);
    jwk.kid = 'realm-key-1';
    jwk.alg = 'RS256';
    jwk.use = 'sig';

    // A second, unrelated key — used to sign a token that must NOT be accepted
    // even though it is otherwise perfectly formed.
    const rogue = await jose.generateKeyPair('RS256');
    otherKey = rogue.privateKey;

    // Serve the realm's certs endpoint at the path the service actually requests.
    server = createServer((req, res) => {
      if (req.url === `/realms/${REALM}/protocol/openid-connect/certs`) {
        res.writeHead(200, { 'content-type': 'application/json' });
        res.end(JSON.stringify({ keys: [jwk] }));
        return;
      }
      res.writeHead(404).end();
    });
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
    const addr = server.address();
    const port = typeof addr === 'object' && addr ? addr.port : 0;
    internalBase = `http://127.0.0.1:${port}`;
  });

  afterAll(async () => {
    await new Promise<void>((resolve) => server.close(() => resolve()));
  });

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    process.env.PUBLIC_URL = 'https://issuer.example';
    process.env.KEYCLOAK_PUBLIC_URL = PUBLIC_BASE;
    process.env.KEYCLOAK_INTERNAL_URL = internalBase;
    process.env.KEYCLOAK_REALM = REALM;
    process.env.ISSUER_DID = 'did:web:issuer.example:authority';
    // Set explicitly: there is no default subject claim, because the field name
    // belongs to the deployment's realm rather than to this service. Individual
    // tests override it to prove the name really is configurable.
    process.env.KEYCLOAK_SUBJECT_CLAIM = 'farmerId';
    // A pre-auth token would be verified through identity-service; these tests
    // only need it to be reachable and to say "no".
    identity = {
      verifyJwt: jest.fn().mockResolvedValue({ verified: false, error: 'not a preauth token' }),
      generateDID: jest.fn(),
    };
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  const makeService = () => new TokenService(identity);

  /** Mints a realm-style access token. */
  async function mint(
    claims: Record<string, unknown> = {},
    opts: { key?: jose.KeyLike; iss?: string; expSeconds?: number } = {},
  ) {
    const now = Math.floor(Date.now() / 1000);
    return new jose.SignJWT({
      realm_access: { roles: ['citizen'] },
      farmerId: 'FRM-000123',
      ...claims,
    })
      .setProtectedHeader({ alg: 'RS256', kid: 'realm-key-1', typ: 'JWT' })
      .setIssuer(opts.iss ?? ISSUER)
      .setSubject('kc-user-1')
      .setAudience('account')
      .setIssuedAt(now)
      .setExpirationTime(now + (opts.expSeconds ?? 300))
      .sign(opts.key ?? signKey);
  }

  it('accepts a realm-signed token and extracts the subject claim and roles', async () => {
    const service = makeService();
    const token = await mint();

    const result = await service.validateAccessToken(`Bearer ${token}`);

    expect(result.source).toBe('keycloak');
    // This is the value the credential endpoint uses to pick a registry record —
    // the single input that decides whose credential gets issued.
    expect(result.subjectId).toBe('FRM-000123');
    expect(result.roles).toEqual(['citizen']);
    expect(result.payload.sub).toBe('kc-user-1');
  });

  it('reads the subject from a configurable claim name', async () => {
    process.env.KEYCLOAK_SUBJECT_CLAIM = 'registryId';
    const service = makeService();
    const token = await mint({ registryId: 'REG-42', farmerId: 'FRM-000123' });

    const result = await service.validateAccessToken(`Bearer ${token}`);
    expect(result.subjectId).toBe('REG-42');
  });

  it('reports no subject when the claim is absent, rather than guessing', async () => {
    // The credential endpoint turns this into an explicit "your account is not
    // linked to a record" message. Falling back to `sub` here would silently look
    // up a registry record keyed by a Keycloak user id and fail confusingly.
    const service = makeService();
    const token = await mint({ farmerId: undefined });

    const result = await service.validateAccessToken(`Bearer ${token}`);
    expect(result.source).toBe('keycloak');
    expect(result.subjectId).toBeUndefined();
  });

  it('rejects a token signed by a key the realm does not publish', async () => {
    // The core forgery case: correct issuer, correct shape, wrong key.
    const service = makeService();
    const token = await mint({}, { key: otherKey });

    await expect(service.validateAccessToken(`Bearer ${token}`)).rejects.toThrow(
      UnauthorizedException,
    );
  });

  it('rejects a token from a different issuer', async () => {
    // An attacker-controlled realm must not be able to mint tokens we accept,
    // even if its own signature checks out.
    const service = makeService();
    const token = await mint({}, { iss: 'https://evil.example/auth/realms/sunbird-rc' });

    await expect(service.validateAccessToken(`Bearer ${token}`)).rejects.toThrow(
      UnauthorizedException,
    );
  });

  it('rejects an expired token', async () => {
    const service = makeService();
    const token = await mint({}, { expSeconds: -60 });

    await expect(service.validateAccessToken(`Bearer ${token}`)).rejects.toThrow(
      UnauthorizedException,
    );
  });

  it('enforces the audience only when one is configured', async () => {
    // Keycloak's default access token carries aud=account, so an unconditional
    // audience check would reject every real token. Opt-in, and effective.
    const token = await mint();
    await expect(makeService().validateAccessToken(`Bearer ${token}`)).resolves.toMatchObject({
      source: 'keycloak',
    });

    process.env.KEYCLOAK_AUDIENCE = 'oid4vc-service';
    await expect(makeService().validateAccessToken(`Bearer ${token}`)).rejects.toThrow(
      UnauthorizedException,
    );
  });

  it('does not treat realm tokens as trusted when Keycloak is not configured', async () => {
    // Without KEYCLOAK_PUBLIC_URL the feature is off, so a realm token must fall
    // through to pre-auth validation — which rejects it — rather than being
    // accepted by a half-enabled code path.
    delete process.env.KEYCLOAK_PUBLIC_URL;
    delete process.env.KEYCLOAK_INTERNAL_URL;
    const service = makeService();
    const token = await mint();

    await expect(service.validateAccessToken(`Bearer ${token}`)).rejects.toThrow(
      UnauthorizedException,
    );
    // It was routed to the pre-auth verifier, confirming the switch is the URL.
    expect(identity.verifyJwt).toHaveBeenCalled();
  });

  it('rejects a missing or malformed Authorization header', async () => {
    const service = makeService();
    await expect(service.validateAccessToken(undefined)).rejects.toThrow(UnauthorizedException);
    await expect(service.validateAccessToken('Basic abc')).rejects.toThrow(UnauthorizedException);
    await expect(service.validateAccessToken('Bearer not.a.jwt')).rejects.toThrow(
      UnauthorizedException,
    );
  });

  it('advertises the realm first in authorization_servers', async () => {
    // Wallets pick the first authorization server they can use, and the realm is
    // the only one of the two implementing authorization_code.
    const service = makeService();
    expect(service.authorizationServers()).toEqual([ISSUER, 'https://issuer.example']);
  });

  it('advertises only itself when Keycloak is not configured', async () => {
    delete process.env.KEYCLOAK_PUBLIC_URL;
    expect(makeService().authorizationServers()).toEqual(['https://issuer.example']);
  });

  it('fetches keys from the internal URL while trusting the public issuer', async () => {
    // The split is the point: if `iss` were compared against the internal URL,
    // every real token would be rejected; if keys were fetched from the public
    // URL, the request would leave the cluster unnecessarily. A token issued by
    // the PUBLIC issuer, verified with keys from the INTERNAL server, proves both
    // halves are wired the right way round.
    const service = makeService();
    const token = await mint();
    await expect(service.validateAccessToken(`Bearer ${token}`)).resolves.toMatchObject({
      source: 'keycloak',
    });
    expect(ISSUER.startsWith('https://issuer.example')).toBe(true);
    expect(internalBase.startsWith('http://127.0.0.1')).toBe(true);
  });
});
