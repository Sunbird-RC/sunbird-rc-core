import { createServer, Server } from 'node:http';
import { AddressInfo } from 'node:net';
import * as jose from 'jose';
import { KeycloakAuthGuard } from './auth.guard';
import { KeycloakService } from './keycloak.service';

// Exercises the guard against a REAL JWK set served over HTTP, so the jose
// verification path (kid lookup, signature, exp, iss) is the thing under test
// rather than a mock of it. No Keycloak required.
//
// Config is read by loadConfig() in KeycloakService's field initializer, so
// process.env must be set BEFORE construction — hence makeGuard(), matching
// the pattern in oid4vp.service.spec.ts.
describe('KeycloakAuthGuard', () => {
  const ORIGINAL_ENV = process.env;

  let server: Server;
  let jwksUri: string;
  let issuer: string;
  let key: jose.GenerateKeyPairResult;
  let otherKey: jose.GenerateKeyPairResult;

  beforeAll(async () => {
    key = await jose.generateKeyPair('RS256');
    otherKey = await jose.generateKeyPair('RS256');
    const jwk = { ...(await jose.exportJWK(key.publicKey)), kid: 'test-key', alg: 'RS256' };

    server = createServer((req, res) => {
      if (req.url?.endsWith('/protocol/openid-connect/certs')) {
        res.writeHead(200, { 'content-type': 'application/json' });
        res.end(JSON.stringify({ keys: [jwk] }));
        return;
      }
      if (req.url?.endsWith('/.well-known/openid-configuration')) {
        res.writeHead(200, { 'content-type': 'application/json' });
        res.end(JSON.stringify({ issuer, jwks_uri: jwksUri }));
        return;
      }
      res.writeHead(404).end();
    });
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));

    const { port } = server.address() as AddressInfo;
    issuer = `http://127.0.0.1:${port}/auth/realms/sunbird-rc`;
    jwksUri = `${issuer}/protocol/openid-connect/certs`;
  });

  afterAll(() => new Promise((resolve) => server.close(resolve)));

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    // .env in the service root would otherwise leak into these tests.
    delete process.env.ENABLE_AUTH;
    delete process.env.JWKS_URI;
    delete process.env.AUTH_ISSUER;
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  // Axios stand-in for the discovery probe; the guard tests never hit it.
  const http: any = { axiosRef: { get: jest.fn() } };

  function makeGuard() {
    return new KeycloakAuthGuard(new KeycloakService(http));
  }

  function makeContext(authorization?: string) {
    const request: any = { headers: authorization ? { authorization } : {} };
    const response = { header: jest.fn() };
    return {
      request,
      response,
      ctx: {
        switchToHttp: () => ({ getRequest: () => request, getResponse: () => response }),
      } as any,
    };
  }

  const sign = (
    payload: Record<string, any>,
    opts: { key?: jose.KeyLike; iss?: string; exp?: string | number } = {},
  ) =>
    new jose.SignJWT(payload)
      .setProtectedHeader({ alg: 'RS256', kid: 'test-key' })
      .setIssuedAt()
      .setIssuer(opts.iss ?? issuer)
      .setExpirationTime(opts.exp ?? '5m')
      .sign(opts.key ?? key.privateKey);

  describe('when ENABLE_AUTH is off', () => {
    it('allows a request with no token at all (unset)', async () => {
      const { ctx } = makeContext();
      await expect(makeGuard().canActivate(ctx)).resolves.toBe(true);
    });

    it('allows a request with no token (explicit false)', async () => {
      process.env.ENABLE_AUTH = 'false';
      process.env.JWKS_URI = jwksUri;
      const { ctx } = makeContext();
      await expect(makeGuard().canActivate(ctx)).resolves.toBe(true);
    });
  });

  describe('when ENABLE_AUTH=true', () => {
    beforeEach(() => {
      process.env.ENABLE_AUTH = 'true';
      process.env.JWKS_URI = jwksUri;
    });

    it('accepts a valid realm token and exposes its claims on the request', async () => {
      const { ctx, request } = makeContext(`Bearer ${await sign({ sub: 'svc-account' })}`);
      await expect(makeGuard().canActivate(ctx)).resolves.toBe(true);
      expect(request.user.sub).toBe('svc-account');
    });

    it('rejects a missing Authorization header with 401 + WWW-Authenticate', async () => {
      const { ctx, response } = makeContext();
      await expect(makeGuard().canActivate(ctx)).rejects.toMatchObject({ status: 401 });
      expect(response.header).toHaveBeenCalledWith(
        'WWW-Authenticate',
        expect.stringContaining('Bearer'),
      );
    });

    it('rejects a non-Bearer scheme', async () => {
      const { ctx } = makeContext('Basic dXNlcjpwYXNz');
      await expect(makeGuard().canActivate(ctx)).rejects.toMatchObject({ status: 401 });
    });

    it('rejects an empty Bearer value', async () => {
      const { ctx } = makeContext('Bearer ');
      await expect(makeGuard().canActivate(ctx)).rejects.toMatchObject({ status: 401 });
    });

    it('rejects a garbage token', async () => {
      const { ctx } = makeContext('Bearer not-a-jwt');
      await expect(makeGuard().canActivate(ctx)).rejects.toMatchObject({ status: 401 });
    });

    it('rejects a token signed by a different key', async () => {
      const token = await sign({ sub: 'forged' }, { key: otherKey.privateKey });
      const { ctx } = makeContext(`Bearer ${token}`);
      await expect(makeGuard().canActivate(ctx)).rejects.toMatchObject({ status: 401 });
    });

    it('rejects an expired token', async () => {
      const token = await sign({ sub: 'stale' }, { exp: Math.floor(Date.now() / 1000) - 60 });
      const { ctx } = makeContext(`Bearer ${token}`);
      await expect(makeGuard().canActivate(ctx)).rejects.toMatchObject({ status: 401 });
    });

    // The property that makes this more than a signature check: another realm
    // on the same Keycloak signs with keys this JWKS also serves.
    it('rejects a correctly-signed token from another realm', async () => {
      const token = await sign({ sub: 'admin' }, { iss: 'http://127.0.0.1/auth/realms/master' });
      const { ctx } = makeContext(`Bearer ${token}`);
      await expect(makeGuard().canActivate(ctx)).rejects.toMatchObject({ status: 401 });
    });

    it('accepts either issuer when AUTH_ISSUER lists both public and internal URLs', async () => {
      const publicIssuer = 'https://gateway.example/auth/realms/sunbird-rc';
      process.env.AUTH_ISSUER = `${issuer}, ${publicIssuer}`;
      const guard = makeGuard();

      for (const iss of [issuer, publicIssuer]) {
        const { ctx } = makeContext(`Bearer ${await sign({ sub: 'u' }, { iss })}`);
        await expect(guard.canActivate(ctx)).resolves.toBe(true);
      }
    });
  });

  describe('configuration is validated at construction', () => {
    it('refuses to start when ENABLE_AUTH=true and JWKS_URI is blank', () => {
      process.env.ENABLE_AUTH = 'true';
      expect(() => makeGuard()).toThrow(/requires JWKS_URI/);
    });

    it('refuses to start when the issuer cannot be derived and AUTH_ISSUER is unset', () => {
      process.env.ENABLE_AUTH = 'true';
      process.env.JWKS_URI = 'https://keycloak.example/some/other/keys.json';
      expect(() => makeGuard()).toThrow(/AUTH_ISSUER/);
    });

    it('derives the realm issuer from JWKS_URI', () => {
      process.env.ENABLE_AUTH = 'true';
      process.env.JWKS_URI = jwksUri;
      expect(new KeycloakService(http).issuers).toEqual([issuer]);
    });
  });
});
