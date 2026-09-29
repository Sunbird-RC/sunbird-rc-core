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

describe('CredentialFormatService — signInFormat dispatch', () => {
  const cred = {
    id: 'urn:uuid:cred-1',
    type: ['VerifiableCredential'],
    issuer: { id: 'did:rcw:issuer-1' },
    issuanceDate: '2026-01-01T00:00:00Z',
    credentialSubject: { id: 'did:key:holder' },
  };

  it('routes ldp_vc to signLdp (identityUtilsService.signVC)', async () => {
    const signVC = jest.fn().mockResolvedValue({ proof: { type: 'Ed25519Signature2020' } });
    const service = new CredentialFormatService({ signVC } as any);

    const result = await service.signInFormat(cred as any, 'did:rcw:issuer-1', 'ldp_vc');
    expect(signVC).toHaveBeenCalled();
    expect(result.signed).toHaveProperty('proof');
    expect(result.enveloped).toBeNull();
  });

  it('routes jwt_vc_json to signJwtVc', async () => {
    const signJwt = jest.fn().mockResolvedValue('jwt.value');
    const service = new CredentialFormatService({ signJwt } as any);

    const result = await service.signInFormat(cred as any, 'did:rcw:issuer-1', 'jwt_vc_json');
    expect(signJwt).toHaveBeenCalled();
    expect(result.enveloped).toBe('jwt.value');
    expect(result.signed).toHaveProperty('@context');
  });

  it('routes vc+sd-jwt to signSdJwtVc', async () => {
    const signSdJwt = jest.fn().mockResolvedValue('sd.jwt');
    const service = new CredentialFormatService({ signSdJwt } as any);

    const result = await service.signInFormat(cred as any, 'did:rcw:issuer-1', 'vc+sd-jwt');
    expect(signSdJwt).toHaveBeenCalled();
    expect(result.enveloped).toBe('sd.jwt');
  });

  it('routes mso_mdoc to signMdoc', async () => {
    const signMdoc = jest.fn().mockResolvedValue('mdoc_data');
    const service = new CredentialFormatService({ signMdoc } as any);

    const result = await service.signInFormat(cred as any, 'did:rcw:issuer-1', 'mso_mdoc', {
      docType: 'org.iso.18013.5.1',
      namespaces: { ns: { k: 'v' } },
    });
    expect(signMdoc).toHaveBeenCalled();
    expect(result.enveloped).toBe('mdoc_data');
  });

  it('throws for unsupported format', async () => {
    const service = new CredentialFormatService({} as any);
    await expect(service.signInFormat(cred as any, 'did:rcw:issuer-1', 'unsupported' as any)).rejects.toThrow('Unsupported format');
  });

  it('defaults to ldp_vc when format is omitted', async () => {
    const signVC = jest.fn().mockResolvedValue({ proof: {} });
    const service = new CredentialFormatService({ signVC } as any);

    await service.signInFormat(cred as any, 'did:rcw:issuer-1');
    expect(signVC).toHaveBeenCalled();
  });
});

describe('CredentialFormatService — signMdoc validation', () => {
  it('throws when docType is missing', async () => {
    const service = new CredentialFormatService({} as any);
    const cred = { id: 'urn:uuid:1', type: ['VerifiableCredential'], credentialSubject: {} };
    await expect(
      service.signInFormat(cred as any, 'did:rcw:issuer', 'mso_mdoc', { namespaces: { ns: {} } })
    ).rejects.toThrow('mso_mdoc requires docType and namespaces');
  });

  it('throws when namespaces is missing', async () => {
    const service = new CredentialFormatService({} as any);
    const cred = { id: 'urn:uuid:1', type: ['VerifiableCredential'], credentialSubject: {} };
    await expect(
      service.signInFormat(cred as any, 'did:rcw:issuer', 'mso_mdoc', { docType: 'org.iso.18013.5.1' })
    ).rejects.toThrow('mso_mdoc requires docType and namespaces');
  });
});

describe('CredentialFormatService — toEpoch', () => {
  it('returns current epoch when dateStr is undefined', () => {
    const service = new CredentialFormatService({} as any);
    const before = Math.floor(Date.now() / 1000);
    const result = (service as any).toEpoch(undefined);
    const after = Math.floor(Date.now() / 1000);
    expect(result).toBeGreaterThanOrEqual(before);
    expect(result).toBeLessThanOrEqual(after);
  });

  it('converts ISO date string to epoch seconds', () => {
    const service = new CredentialFormatService({} as any);
    const result = (service as any).toEpoch('2026-01-01T00:00:00Z');
    expect(result).toBe(1767225600);
  });

  it('floors to whole seconds', () => {
    const service = new CredentialFormatService({} as any);
    const result = (service as any).toEpoch('2026-01-01T00:00:00.999Z');
    expect(result).toBe(1767225600);
  });

  it('falls back to current time for invalid date string', () => {
    const service = new CredentialFormatService({} as any);
    const before = Math.floor(Date.now() / 1000);
    const result = (service as any).toEpoch('not-a-date');
    const after = Math.floor(Date.now() / 1000);
    expect(result).toBeGreaterThanOrEqual(before);
    expect(result).toBeLessThanOrEqual(after);
  });
});

describe('CredentialFormatService — envelope', () => {
  it('returns W3C VC Data Model 2.0 EnvelopedVerifiableCredential', () => {
    const service = new CredentialFormatService({} as any);
    const result = (service as any).envelope('urn:uuid:cred-1', 'data:application/vc+jwt,abc');

    expect(result).toEqual({
      '@context': ['https://www.w3.org/ns/credentials/v2'],
      id: 'data:application/vc+jwt,abc',
      type: 'EnvelopedVerifiableCredential',
      credentialId: 'urn:uuid:cred-1',
    });
  });
});

describe('CredentialFormatService — signSdJwtVc disclosable & vct', () => {
  let signSdJwt: jest.Mock;

  beforeEach(() => {
    signSdJwt = jest.fn().mockResolvedValue('sd.jwt');
  });

  const makeService = () => new CredentialFormatService({ signSdJwt } as any);

  it('uses explicit disclosable array when provided', async () => {
    const service = makeService();
    const cred = {
      id: 'urn:uuid:1',
      type: ['VerifiableCredential'],
      issuer: { id: 'did:rcw:issuer' },
      issuanceDate: '2026-01-01T00:00:00Z',
      credentialSubject: { id: 'did:key:h', name: 'Alice', grade: 'A' },
    };

    await (service as any).signSdJwtVc(cred, { id: 'did:rcw:issuer' }, { disclosable: ['name'] });
    const disclosable = signSdJwt.mock.calls[0][2];
    expect(disclosable).toEqual(['name']);
  });

  it('defaults disclosable to all subject keys except id', async () => {
    const service = makeService();
    const cred = {
      id: 'urn:uuid:1',
      type: ['VerifiableCredential'],
      issuer: { id: 'did:rcw:issuer' },
      issuanceDate: '2026-01-01T00:00:00Z',
      credentialSubject: { id: 'did:key:h', grade: 'A', programme: 'CS' },
    };

    await (service as any).signSdJwtVc(cred, { id: 'did:rcw:issuer' }, {});
    const disclosable = signSdJwt.mock.calls[0][2];
    expect(disclosable).toEqual(['grade', 'programme']);
  });

  it('uses explicit vct when provided', async () => {
    const service = makeService();
    const cred = {
      id: 'urn:uuid:1',
      type: ['VerifiableCredential', 'AgeCredential'],
      issuer: { id: 'did:rcw:issuer' },
      issuanceDate: '2026-01-01T00:00:00Z',
      credentialSubject: { id: 'did:key:h' },
    };

    await (service as any).signSdJwtVc(cred, { id: 'did:rcw:issuer' }, { vct: 'CustomVct' });
    const payload = signSdJwt.mock.calls[0][1];
    expect(payload.vct).toBe('CustomVct');
  });

  it('derives vct from last element of type array', async () => {
    const service = makeService();
    const cred = {
      id: 'urn:uuid:1',
      type: ['VerifiableCredential', 'AgeCredential'],
      issuer: { id: 'did:rcw:issuer' },
      issuanceDate: '2026-01-01T00:00:00Z',
      credentialSubject: { id: 'did:key:h' },
    };

    await (service as any).signSdJwtVc(cred, { id: 'did:rcw:issuer' }, {});
    const payload = signSdJwt.mock.calls[0][1];
    expect(payload.vct).toBe('AgeCredential');
  });

  it('falls back to VerifiableCredential when type is empty', async () => {
    const service = makeService();
    const cred = {
      id: 'urn:uuid:1',
      type: [],
      issuer: { id: 'did:rcw:issuer' },
      issuanceDate: '2026-01-01T00:00:00Z',
      credentialSubject: { id: 'did:key:h' },
    };

    await (service as any).signSdJwtVc(cred, { id: 'did:rcw:issuer' }, {});
    const payload = signSdJwt.mock.calls[0][1];
    expect(payload.vct).toBe('VerifiableCredential');
  });
});
