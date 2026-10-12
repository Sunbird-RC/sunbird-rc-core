import { Oid4vciService } from './oid4vci.service';
import { MemoryStoreService } from '../session/memory-store.service';
import { ClaimSourceFactory } from '../claims/claim-source.factory';
import { RegistryClaimSource } from '../claims/registry.claim-source';

// Wallet self-service issuance: a holder signs into Keycloak from their own
// wallet and is issued the credential for THEIR record.
//
// The property under test is the one the whole design rests on — the credential's
// contents come from the registry record named by the holder's own token, so a
// holder cannot obtain someone else's credential and cannot influence what their
// own one says. The last two tests attack exactly that.
describe('Oid4vciService wallet self-service issuance', () => {
  const ORIGINAL_ENV = process.env;
  let schema: any;
  let tokens: any;
  let credentials: any;
  let pop: any;
  let registry: any;

  const SCHEMA_ID = 'did:schema:farmer';

  const RAVI = {
    farmer: { farmerId: 'FRM-000123', name: 'Ravi Kumar', gender: 'Male' },
    parcels: [{ landRecordRef: 'LR-1', landAreaAcres: 4.5, ownershipType: 'Owned' }],
    crops: [{ cropName: 'Wheat', year: 2026 }],
  };
  const LAKSHMI = {
    farmer: { farmerId: 'FRM-000124', name: 'Lakshmi Devi', gender: 'Female' },
    parcels: [{ landRecordRef: 'LR-2', landAreaAcres: 2, ownershipType: 'Leased' }],
    crops: [],
  };

  /**
   * What RegistryClient.subjectSources returns: the subject record, plus one
   * record per configured related entity, in precedence order.
   *
   * The entity names here are this deployment's configuration, not the service's
   * — the service reads whatever it is given, which is why the resolver's own
   * tests cover a completely different domain.
   */
  const asSources = (b: { farmer?: any; parcels?: any[]; crops?: any[] }) => ({
    subject: b.farmer,
    sources: [
      { entity: 'Farmer', record: b.farmer },
      { entity: 'LandParcel', record: b.parcels?.[0] },
      { entity: 'Crop', record: b.crops?.[0] },
    ],
  });

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    process.env.PUBLIC_URL = 'https://issuer.example';
    process.env.KEYCLOAK_PUBLIC_URL = 'https://issuer.example/auth';
    process.env.REGISTRY_BASE_URL = 'http://registry:8081';
    // Which token claim names the holder has no default — the field name belongs
    // to the deployment, not to this service. A test below asserts that leaving it
    // unset reports the missing variable instead of blaming the holder's account.
    process.env.KEYCLOAK_SUBJECT_CLAIM = 'farmerId';

    schema = {
      getOid4vciConfigs: jest.fn().mockResolvedValue([
        {
          schemaId: SCHEMA_ID,
          version: '1.0.0',
          name: 'Farmer Land Holding Credential',
          type: 'FarmerCredential',
          tags: ['farmer'],
          formats: ['vc+sd-jwt'],
          vct: 'https://issuer.example/vct/farmer',
          display: [],
          author: 'did:web:issuer.example:authority',
          schema: {
            properties: {
              farmerId: { type: 'string' },
              name: { type: 'string' },
              landAreaAcres: { type: 'number' },
            },
            required: ['farmerId', 'name', 'landAreaAcres'],
          },
        },
      ]),
    };
    tokens = {
      getIssuerDid: jest.fn().mockReturnValue('did:web:issuer.example:authority'),
      mintAccessToken: jest.fn(),
      validateAccessToken: jest.fn(),
      authorizationServers: jest.fn().mockReturnValue(['https://issuer.example/auth']),
    };
    // issueForSession delegates here; capture what it was asked to sign. The
    // claims land in `credential.credentialSubject` alongside the holder's id.
    credentials = { issue: jest.fn().mockResolvedValue({ credential: 'sd.jwt~disclosures' }) };
    pop = {
      verifyJwtProof: jest.fn().mockResolvedValue({
        valid: true,
        holderDid: 'did:key:zHolder',
        holderKid: 'did:key:zHolder#0',
      }),
    };
    registry = { enabled: true, subjectSources: jest.fn() };
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  /**
   * Built with the REAL claim-source factory over the mocked registry client, not
   * a mocked factory — these tests are about registry-sourced issuance, so the
   * provider selection and the registry adapter are part of what they cover.
   */
  const makeService = () =>
    new Oid4vciService(
      new MemoryStoreService() as any,
      credentials,
      schema,
      tokens,
      pop,
      new ClaimSourceFactory(new RegistryClaimSource(registry)),
    );

  /**
   * The claims actually sent for signing, with the holder's DID stripped —
   * `credentialSubject.id` is the wallet's own key, not a registry value.
   */
  function subjectOf(c: any) {
    const { id, ...claims } = c.issue.mock.calls[0][0].credential.credentialSubject;
    return claims;
  }

  /** A Keycloak token for a signed-in holder carrying their registry key. */
  const keycloakToken = (farmerId?: string, sub = 'kc-user-1') => ({
    payload: { sub, iss: 'https://issuer.example/realms/sunbird-rc' },
    source: 'keycloak' as const,
    subjectId: farmerId,
    roles: ['citizen'],
  });

  /**
   * Drives the credential endpoint far enough to capture the resolved claims.
   * A live c_nonce is required, so mint one through the service itself.
   */
  async function requestCredential(
    service: Oid4vciService,
    body: Record<string, any> = { credential_configuration_id: SCHEMA_ID },
  ) {
    const nonce = await service.issueNonce();
    jest
      .spyOn(service as any, 'decodeJwtClaims')
      .mockReturnValue({ nonce });
    return service.credential('Bearer kc-token', {
      proof: { jwt: 'proof.jwt.value' },
      ...body,
    });
  }

  it('issues from the registry record named by the token', async () => {
    tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-000123'));
    registry.subjectSources.mockResolvedValue(asSources(RAVI));
    const service = makeService();

    const res = await requestCredential(service);

    expect(registry.subjectSources).toHaveBeenCalledWith('FRM-000123');
    expect(res.credential).toBeDefined();
    const claims = subjectOf(credentials);
    expect(claims).toEqual({
      farmerId: 'FRM-000123',
      name: 'Ravi Kumar',
      landAreaAcres: 4.5,
    });
  });

  it('gives a different holder their own credential, not the first one', async () => {
    tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-000124', 'kc-user-2'));
    registry.subjectSources.mockResolvedValue(asSources(LAKSHMI));
    const service = makeService();

    await requestCredential(service);

    expect(registry.subjectSources).toHaveBeenCalledWith('FRM-000124');
    const claims = subjectOf(credentials);
    expect(claims.name).toBe('Lakshmi Devi');
    expect(claims.farmerId).toBe('FRM-000124');
  });

  it('IGNORES claims supplied by the wallet', async () => {
    // The attack this closes: the caller is the holder's own wallet, so anything
    // it asserts about itself is unverified. A wallet asking for 900 acres under
    // someone else's name must still receive exactly its own record.
    tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-000123'));
    registry.subjectSources.mockResolvedValue(asSources(RAVI));
    const service = makeService();

    await requestCredential(service, {
      credential_configuration_id: SCHEMA_ID,
      claims: { farmerId: 'FRM-999999', name: 'Someone Else', landAreaAcres: 900 },
    });

    const claims = subjectOf(credentials);
    expect(claims).toEqual({
      farmerId: 'FRM-000123',
      name: 'Ravi Kumar',
      landAreaAcres: 4.5,
    });
  });

  it('cannot reach another holder’s record by naming it in the request', async () => {
    // Only the token decides whose record is read. `farmerId` in the body is not
    // consulted at all.
    tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-000123'));
    registry.subjectSources.mockResolvedValue(asSources(RAVI));
    const service = makeService();

    await requestCredential(service, {
      credential_configuration_id: SCHEMA_ID,
      farmerId: 'FRM-000124',
      subject: 'FRM-000124',
    });

    expect(registry.subjectSources).toHaveBeenCalledTimes(1);
    expect(registry.subjectSources).toHaveBeenCalledWith('FRM-000123');
  });

  it('refuses cleanly when the account is not linked to a record', async () => {
    // Authenticated but no farmerId claim: a provisioning gap, not a protocol
    // error, so the message has to say what an administrator must do.
    tokens.validateAccessToken.mockResolvedValue(keycloakToken(undefined));
    const service = makeService();

    await expect(requestCredential(service)).rejects.toThrow(
      /not linked to a registry record/,
    );
    expect(registry.subjectSources).not.toHaveBeenCalled();
  });

  it('names the missing variable when no subject claim is configured', async () => {
    // The regression this guards: KEYCLOAK_SUBJECT_CLAIM used to default to
    // `farmerId` — a field name borrowed from one deployment's registry. An
    // operator who never set it got "this account is not linked to a registry
    // record" and went looking through Keycloak users for a problem that was in
    // the environment. There is now no default, so the error has to name the
    // variable rather than blame the holder.
    delete process.env.KEYCLOAK_SUBJECT_CLAIM;
    tokens.validateAccessToken.mockResolvedValue(keycloakToken(undefined));
    const service = makeService();

    await expect(requestCredential(service)).rejects.toThrow(/KEYCLOAK_SUBJECT_CLAIM/);
    await expect(requestCredential(service)).rejects.not.toThrow(/not linked/);
    expect(registry.subjectSources).not.toHaveBeenCalled();
  });

  it('refuses cleanly when the linked record does not exist', async () => {
    tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-DELETED'));
    registry.subjectSources.mockResolvedValue(asSources({ farmer: undefined, parcels: [], crops: [] }));
    const service = makeService();

    // "no record", not "no registry record": the holder's records may live in the
    // issuing authority's own database, so the message must not name ours.
    await expect(requestCredential(service)).rejects.toThrow(/no record for farmerId/);
  });

  it('refuses when the record is missing a required claim, naming it', async () => {
    // Better than the opaque 500 credentials-service would otherwise return, and
    // the message tells the holder who can fix it.
    tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-000123'));
    registry.subjectSources.mockResolvedValue(asSources({ ...RAVI, parcels: [] }));
    const service = makeService();

    await expect(requestCredential(service)).rejects.toThrow(
      /missing required landAreaAcres/,
    );
  });

  it('rejects an unknown credential_configuration_id rather than substituting another', async () => {
    // A request that NAMES a type it cannot have must fail. Quietly issuing the
    // only type on file would hand the holder something they did not ask for.
    tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-000123'));
    registry.subjectSources.mockResolvedValue(asSources(RAVI));
    const service = makeService();

    await expect(
      requestCredential(service, { credential_configuration_id: 'did:schema:nope' }),
    ).rejects.toThrow(/could not determine the credential type/);
  });

  it('refuses when the chosen claim source is the registry and it is unconfigured', async () => {
    // Without a system of record there is nothing trustworthy to issue from, so
    // this must not silently fall back to caller-supplied claims. The message has
    // to name the variable AND the alternatives, because "the registry is not
    // configured" is a mistake only if this deployment meant to use the registry —
    // an authority using its own database sets CLAIM_SOURCE_MAP instead.
    registry.enabled = false;
    tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-000123'));
    const service = makeService();

    await expect(requestCredential(service)).rejects.toThrow(/REGISTRY_BASE_URL is not set/);
    await expect(requestCredential(service)).rejects.toThrow(/CLAIM_SOURCE_MAP/);
  });

  describe('format is derived from the credential, not assumed', () => {
    /** Republishes the one test credential under a different format list. */
    const publishAs = async (...formats: string[]) => {
      const [cfg] = await schema.getOid4vciConfigs();
      schema.getOid4vciConfigs.mockResolvedValue([{ ...cfg, formats }]);
    };

    it('issues for an ldp_vc-only deployment when the request names no format', async () => {
      // The regression this guards: the format used to default to 'vc+sd-jwt', so
      // the candidate filter matched nothing and a deployment publishing a single
      // ldp_vc credential was told "could not determine the credential type" —
      // with exactly one type published and nothing ambiguous about it.
      await publishAs('ldp_vc');
      tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-000123'));
      registry.subjectSources.mockResolvedValue(asSources(RAVI));
      const service = makeService();

      await expect(requestCredential(service, {})).resolves.toHaveProperty('credential');
    });

    it('answers a vct-identified request with SD-JWT even when ldp_vc is listed first', async () => {
      // Credo sends `vct` and no format. `vct` exists only in SD-JWT VC, so
      // resolving by it settles the format — taking formats[0] instead would hand
      // back an LDP credential for an SD-JWT request.
      await publishAs('ldp_vc', 'vc+sd-jwt');
      tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-000123'));
      registry.subjectSources.mockResolvedValue(asSources(RAVI));
      const service = makeService();

      await requestCredential(service, { vct: 'https://issuer.example/vct/farmer' });

      expect(credentials.issue.mock.calls[0][0].format).toBe('vc+sd-jwt');
    });

    it('honours the format encoded in a <schemaId>_<format> configuration id', async () => {
      await publishAs('ldp_vc', 'vc+sd-jwt');
      tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-000123'));
      registry.subjectSources.mockResolvedValue(asSources(RAVI));
      const service = makeService();

      await requestCredential(service, {
        credential_configuration_id: `${SCHEMA_ID}_vc+sd-jwt`,
      });

      expect(credentials.issue.mock.calls[0][0].format).toBe('vc+sd-jwt');
    });

    it('still refuses a format the credential does not publish', async () => {
      await publishAs('ldp_vc');
      tokens.validateAccessToken.mockResolvedValue(keycloakToken('FRM-000123'));
      registry.subjectSources.mockResolvedValue(asSources(RAVI));
      const service = makeService();

      await expect(
        requestCredential(service, {
          credential_configuration_id: SCHEMA_ID,
          format: 'mso_mdoc',
        }),
      ).rejects.toThrow(/format 'mso_mdoc' not supported/);
    });
  });

  it('still honours the pre-authorized_code path unchanged', async () => {
    // The staff flow must not regress: its claims were fixed at offer creation
    // and the registry is not consulted.
    tokens.validateAccessToken.mockResolvedValue({
      payload: { sub: 'offer-id-1' },
      source: 'preauth' as const,
    });
    const service = makeService();
    const offer = await service.createOffer({
      credential_configuration_id: SCHEMA_ID,
      claims: { farmerId: 'FRM-000123', name: 'Ravi Kumar', landAreaAcres: 4.5 },
    });
    tokens.validateAccessToken.mockResolvedValue({
      payload: { sub: offer.offer_id },
      source: 'preauth' as const,
    });

    await requestCredential(service);

    expect(registry.subjectSources).not.toHaveBeenCalled();
    expect(subjectOf(credentials)).toEqual({
      farmerId: 'FRM-000123',
      name: 'Ravi Kumar',
      landAreaAcres: 4.5,
    });
  });
});
