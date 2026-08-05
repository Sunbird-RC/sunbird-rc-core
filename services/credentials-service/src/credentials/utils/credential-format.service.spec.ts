import { CredentialFormatService } from './credential-format.service';

// A strict W3C VC-JWT holder requires `nbf` to represent vc.issuanceDate
// exactly, comparing `Date.parse(vc.issuanceDate) / 1000` — unfloored — against
// the integer `nbf`. Millisecond-precision dates make that comparison
// impossible to satisfy, so the invariant is asserted here.
const assertStrictJwtDateInvariant = (claims: any) => {
  expect(Date.parse(claims.vc.issuanceDate) / 1000).toBe(claims.nbf);
  if (claims.vc.expirationDate) {
    expect(Date.parse(claims.vc.expirationDate) / 1000).toBe(claims.exp);
  }
};

describe('CredentialFormatService — jwt_vc_json envelope', () => {
  let service: CredentialFormatService;
  let signJwt: jest.Mock;

  const sign = (credential: any) =>
    (service as any).signJwtVc(credential, { id: 'did:rcw:issuer-1' });

  const credential = (overrides: Record<string, any> = {}) => ({
    id: 'urn:uuid:cred-1',
    type: ['VerifiableCredential', 'Age Verification Credential'],
    issuer: { id: 'did:rcw:issuer-1' },
    issuanceDate: '2026-07-28T07:30:12.345Z',
    credentialSubject: { id: 'did:key:zDnaeholder', birthdate: '1990-01-01' },
    ...overrides,
  });

  beforeEach(() => {
    signJwt = jest.fn().mockResolvedValue('signed.jwt.value');
    service = new CredentialFormatService({ signJwt } as any);
  });

  const claimsOf = () => signJwt.mock.calls[0][1];

  it('embeds a whole-second issuanceDate matching nbf', async () => {
    await sign(credential());
    const claims = claimsOf();

    expect(claims.nbf).toBe(Math.floor(Date.parse('2026-07-28T07:30:12.345Z') / 1000));
    expect(claims.vc.issuanceDate).toBe('2026-07-28T07:30:12.000Z');
    assertStrictJwtDateInvariant(claims);
  });

  it('embeds a whole-second expirationDate matching exp', async () => {
    await sign(
      credential({ expirationDate: '2027-01-01T00:00:00.789Z' }),
    );
    const claims = claimsOf();

    expect(claims.vc.expirationDate).toBe('2027-01-01T00:00:00.000Z');
    assertStrictJwtDateInvariant(claims);
  });

  it('omits exp entirely when the credential does not expire', async () => {
    await sign(credential());
    const claims = claimsOf();

    expect('exp' in claims).toBe(false);
    expect(claims.vc.expirationDate).toBeUndefined();
  });

  it('holds the invariant for already-whole-second dates', async () => {
    await sign(credential({ issuanceDate: '2026-07-28T07:30:12Z' }));
    assertStrictJwtDateInvariant(claimsOf());
  });

  it('does not mutate the caller’s credential', async () => {
    const original = credential();
    await sign(original);

    expect(original.issuanceDate).toBe('2026-07-28T07:30:12.345Z');
  });

  describe('vc+sd-jwt key binding', () => {
    let signSdJwt: jest.Mock;

    const signSd = (opts: Record<string, any>) =>
      (service as any).signSdJwtVc(credential(), { id: 'did:rcw:issuer-1' }, opts);

    beforeEach(() => {
      signSdJwt = jest.fn().mockResolvedValue('signed.sd.jwt');
      service = new CredentialFormatService({ signSdJwt } as any);
    });

    const payloadOf = () => signSdJwt.mock.calls[0][1];

    it('binds by reference (cnf.kid) when the proof used a DID kid', async () => {
      await signSd({ holderKid: 'did:key:zDnaeholder#zDnaeholder', holderJwk: { kty: 'EC' } });

      expect(payloadOf().cnf).toEqual({ kid: 'did:key:zDnaeholder#zDnaeholder' });
    });

    it('binds by value (cnf.jwk) for an inline-jwk proof', async () => {
      const holderJwk = { kty: 'EC', crv: 'P-256', x: 'x', y: 'y' };
      await signSd({ holderJwk });

      expect(payloadOf().cnf).toEqual({ jwk: holderJwk });
    });

    it('omits cnf entirely when the proof carried no holder key', async () => {
      await signSd({});

      expect('cnf' in payloadOf()).toBe(false);
    });
  });

  it('keeps iss/sub/jti bound to the credential', async () => {
    await sign(credential());
    const claims = claimsOf();

    expect(claims.iss).toBe('did:rcw:issuer-1');
    expect(claims.sub).toBe('did:key:zDnaeholder');
    expect(claims.jti).toBe('urn:uuid:cred-1');
  });
});
