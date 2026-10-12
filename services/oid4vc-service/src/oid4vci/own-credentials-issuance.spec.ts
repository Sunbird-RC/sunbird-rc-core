// Advertising only your own credentials is a display decision. MINTING only your
// own is an authorization decision, and until 2 September 2026 this build made
// the first and not the second.
//
// credential-schema signs with the key of the DID that AUTHORED the schema, so an
// authenticated holder could ask any instance for any published configuration id
// and be answered with a credential signed by a different issuer, carrying the
// claims the answering instance resolved from its own registry entity. Reported
// live: the school instance answered a request for the college configuration with
// a College-signed credential carrying the learner's school percentage.
//
// These assertions are about what an instance will MINT, not about what it lists.
describe('issuing only an issuer\'s own credentials', () => {
  const ORIGINAL_ENV = process.env;
  afterEach(() => {
    process.env = ORIGINAL_ENV;
    jest.resetModules();
  });

  const SCHOOL = 'did:web:issuer.example:school';
  const COLLEGE = 'did:web:issuer.example:college';

  const CONFIGS = [
    {
      schemaId: 'did:schema:school',
      version: '1.0.0',
      name: 'School Record Credential',
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      tags: ['education'],
      formats: ['vc+sd-jwt'],
      vct: 'school-record-credential',
      display: [{ name: 'School Record Credential', locale: 'en-US' }],
      schema: { properties: {}, required: [] },
      author: SCHOOL,
    },
    {
      schemaId: 'did:schema:college',
      version: '1.0.0',
      name: 'College Record Credential',
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      tags: ['education'],
      formats: ['vc+sd-jwt'],
      vct: 'college-record-credential',
      display: [{ name: 'College Record Credential', locale: 'en-US' }],
      schema: { properties: {}, required: [] },
      author: COLLEGE,
    },
  ];

  /**
   * Drives the SCHOOL instance's self-service credential path with whatever the
   * request names, and reports which schema it resolved.
   *
   * buildSelfServiceSession is private, which is the point: it is the step that
   * decides WHICH credential a request gets, and it is reached from the
   * wallet-facing endpoint. Calling it directly keeps the assertion on that
   * decision rather than on everything issuance does afterwards.
   */
  async function resolveAs(env: Record<string, string>, body: Record<string, any>) {
    process.env = {
      ...ORIGINAL_ENV,
      PUBLIC_URL: 'https://issuer.example',
      KEYCLOAK_SUBJECT_CLAIM: 'nationalId',
      ISSUER_DID: SCHOOL,
      ...env,
    };
    jest.resetModules();

    const { Oid4vciService } = await import('./oid4vci.service');
    const { MemoryStoreService } = await import('../session/memory-store.service');
    // The claim source is stubbed rather than reached: everything past type
    // resolution talks to the registry and to credentials-service, neither of
    // which this is about.
    const claimSources = {
      for: () => ({
        resolve: async () => ({ claims: { learnerId: 'EDU-L-006733' }, missing: [] }),
      }),
    } as any;
    const service: any = new Oid4vciService(
      new MemoryStoreService() as any,
      {} as any,
      { getOid4vciConfigs: jest.fn().mockResolvedValue(CONFIGS) } as any,
      { authorizationServers: () => ['https://issuer.example'], getIssuerDid: () => SCHOOL } as any,
      {} as any,
      claimSources,
    );
    return service.buildSelfServiceSession(
      { subjectId: 'NAT-70022345', raw: {} } as any,
      body,
    );
  }

  const RESTRICTED = { ADVERTISE_OWN_CREDENTIALS_ONLY: 'true' };

  it('issues its own credential type', async () => {
    const session = await resolveAs(RESTRICTED, {
      credential_configuration_id: 'did:schema:school',
    });
    expect(session.credentialConfigurationId).toBe('did:schema:school');
  });

  it('refuses to mint another institution\'s credential type', async () => {
    // Asserted on the message, not on BadRequestException: jest.resetModules()
    // gives this spec a different class object from the one the freshly-imported
    // service throws, so `toThrow(BadRequestException)` fails for a reason that
    // has nothing to do with the behaviour. Same trap own-credentials.spec.ts
    // documents for Logger.
    await expect(
      resolveAs(RESTRICTED, { credential_configuration_id: 'did:schema:college' }),
    ).rejects.toThrow(/not issued by this issuer/);
  });

  it('says whose credential it is, so a refusal is diagnosable', async () => {
    // "Could not determine the credential type" would send an operator looking
    // for a missing schema. The schema is there; it belongs to someone else.
    await expect(
      resolveAs(RESTRICTED, { credential_configuration_id: 'did:schema:college' }),
    ).rejects.toThrow(/College Record Credential.*not issued by this issuer/s);
    await expect(
      resolveAs(RESTRICTED, { credential_configuration_id: 'did:schema:college' }),
    ).rejects.toThrow(new RegExp(COLLEGE));
  });

  it('refuses the other issuer\'s vct too, not just its configuration id', async () => {
    // A wallet on the authorization_code path sends neither a configuration id
    // nor a credential identifier — Credo sends `vct`. Narrowing only the id
    // would leave the same hole open under a different key.
    await expect(
      resolveAs(RESTRICTED, { vct: 'https://issuer.example/vct/college-record-credential' }),
    ).rejects.toThrow(/College Record Credential.*not issued by this issuer/s);
  });

  it('still resolves the single published type when the request names nothing', async () => {
    // With the narrowing on, this instance publishes exactly one credential, so
    // a request that identifies nothing is unambiguous — and must stay that way,
    // because that is the path a minimal wallet takes.
    const session = await resolveAs(RESTRICTED, {});
    expect(session.credentialConfigurationId).toBe('did:schema:school');
  });

  it('mints anything published when the flag is off, as it always has', async () => {
    // Unchanged for a single-issuer deployment that never set the flag.
    const session = await resolveAs({}, { credential_configuration_id: 'did:schema:college' });
    expect(session.credentialConfigurationId).toBe('did:schema:college');
  });

  it('ignores the flag when no issuer DID is configured', async () => {
    // Same reasoning as the metadata path: filtering on an empty DID would issue
    // nothing at all, which looks like the schema service being down.
    const session = await resolveAs(
      { ...RESTRICTED, ISSUER_DID: '' },
      { credential_configuration_id: 'did:schema:college' },
    );
    expect(session.credentialConfigurationId).toBe('did:schema:college');
  });
});
