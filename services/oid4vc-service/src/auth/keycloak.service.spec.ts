import { KeycloakService } from './keycloak.service';

// The "is Keycloak actually running?" half of ENABLE_AUTH: the boot probe and
// what GET /health reports. The JWKS fetch is mocked at the axios level — token
// verification is covered against a real key set in auth.guard.spec.ts.
describe('KeycloakService reachability', () => {
  const ORIGINAL_ENV = process.env;
  const ISSUER = 'http://keycloak:8080/auth/realms/sunbird-rc';
  const JWKS = `${ISSUER}/protocol/openid-connect/certs`;

  let get: jest.Mock;
  let http: any;

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    delete process.env.ENABLE_AUTH;
    delete process.env.JWKS_URI;
    delete process.env.AUTH_ISSUER;
    get = jest.fn();
    http = { axiosRef: { get } };
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  const enable = () => {
    process.env.ENABLE_AUTH = 'true';
    process.env.JWKS_URI = JWKS;
  };

  const keySet = { data: { keys: [{ kid: 'k1', kty: 'RSA', alg: 'RS256' }] } };

  it('is a no-op when auth is off, and health says why', async () => {
    const kc = new KeycloakService(http);
    await expect(kc.probe()).resolves.toBe(true);
    expect(get).not.toHaveBeenCalled();
    await expect(kc.healthInfo()).resolves.toEqual({
      enabled: false,
      status: 'UP',
      reason: 'ENABLE_AUTH=false',
    });
  });

  it('probes JWKS_URI itself', async () => {
    enable();
    get.mockResolvedValue(keySet);

    await expect(new KeycloakService(http).probe()).resolves.toBe(true);
    expect(get).toHaveBeenCalledWith(JWKS, expect.anything());
  });

  // Found in real verification: AUTH_ISSUER holds token `iss` VALUES, and in
  // the public/internal split it exists for, the first one is an in-cluster
  // hostname that does not resolve from wherever the probe runs. Probing an
  // issuer instead of JWKS_URI made the service fail to boot against a
  // perfectly healthy Keycloak.
  it('probes JWKS_URI even when AUTH_ISSUER names an unreachable internal host', async () => {
    // JWKS is fetched from the URL that actually resolves here; the first
    // accepted `iss` is the in-cluster name, which does not.
    const reachableJwks = 'https://gateway.example/auth/realms/sunbird-rc/protocol/openid-connect/certs';
    process.env.ENABLE_AUTH = 'true';
    process.env.JWKS_URI = reachableJwks;
    process.env.AUTH_ISSUER = `${ISSUER},https://gateway.example/auth/realms/sunbird-rc`;
    get.mockResolvedValue(keySet);

    await expect(new KeycloakService(http).probe()).resolves.toBe(true);
    expect(get).toHaveBeenCalledTimes(1);
    expect(get).toHaveBeenCalledWith(reachableJwks, expect.anything());
    expect(get).not.toHaveBeenCalledWith(
      expect.stringContaining('keycloak:8080'),
      expect.anything(),
    );
  });

  // The reason for retrying at all: compose starts this service beside a
  // Keycloak image that takes ~a minute to answer.
  it('retries a refused connection and succeeds once Keycloak comes up', async () => {
    enable();
    get
      .mockRejectedValueOnce({ code: 'ECONNREFUSED' })
      .mockRejectedValueOnce({ code: 'ECONNREFUSED' })
      .mockResolvedValue(keySet);

    await expect(new KeycloakService(http).probe(5, 1)).resolves.toBe(true);
    expect(get).toHaveBeenCalledTimes(3);
  });

  it('gives up after the configured number of attempts', async () => {
    enable();
    get.mockRejectedValue({ code: 'ECONNREFUSED' });

    const kc = new KeycloakService(http);
    await expect(kc.probe(3, 1)).resolves.toBe(false);
    expect(get).toHaveBeenCalledTimes(3);
    await expect(kc.healthInfo()).resolves.toMatchObject({
      enabled: true,
      status: 'DOWN',
      reason: 'ECONNREFUSED',
    });
  });

  // A wrong realm in JWKS_URI answers 404 or an empty set rather than refusing
  // the connection, so "responded" is not the same as "usable".
  it('treats an empty JWK set as unreachable', async () => {
    enable();
    get.mockResolvedValue({ data: { keys: [] } });

    await expect(new KeycloakService(http).probe(1)).resolves.toBe(false);
  });

  it('reports UP once Keycloak answers', async () => {
    enable();
    get.mockResolvedValue(keySet);

    const kc = new KeycloakService(http);
    await expect(kc.healthInfo()).resolves.toEqual({ enabled: true, status: 'UP' });
  });

  it('caches the health probe so the docker healthcheck cannot hammer Keycloak', async () => {
    enable();
    get.mockResolvedValue(keySet);

    const kc = new KeycloakService(http);
    await kc.healthInfo();
    await kc.healthInfo();
    await kc.healthInfo();
    expect(get).toHaveBeenCalledTimes(1);
  });

  // A bad JWKS_URI is the most likely misconfiguration, so the log has to name
  // the URL that failed rather than just the errno.
  it('names the JWKS URL in the failure log', async () => {
    enable();
    get.mockRejectedValue({ code: 'ENOTFOUND' });

    const kc = new KeycloakService(http);
    const logged: string[] = [];
    jest.spyOn((kc as any).logger, 'warn').mockImplementation((m: any) => logged.push(String(m)));

    await expect(kc.probe(1)).resolves.toBe(false);
    expect(logged.join('\n')).toContain(JWKS);
  });
});
