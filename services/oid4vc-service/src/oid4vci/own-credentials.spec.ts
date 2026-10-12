// credential-schema's /oid4vci-configs is deployment-wide: it returns every
// published config and takes no filter. With one issuer that is invisible. With
// two sharing a schema service, each issuer's metadata lists the other's
// credentials, and a wallet's issuer directory then shows the same credential
// under whichever issuer the holder happened to open.
//
// These assertions are about what a wallet would list.
describe('advertising only an issuer\'s own credentials', () => {
  const ORIGINAL_ENV = process.env;

  afterEach(() => {
    process.env = ORIGINAL_ENV;
    jest.resetModules();
  });

  const FARMER = 'did:web:issuer.example:farmer';
  const LAND = 'did:web:issuer.example:land';

  const CONFIGS = [
    {
      schemaId: 'did:schema:farmer',
      version: '1.0.0',
      name: 'Farmer Identity Credential',
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      tags: ['agriculture'],
      formats: ['vc+sd-jwt'],
      vct: 'farmer-identity-credential',
      display: [{ name: 'Farmer Identity Credential', locale: 'en-US' }],
      schema: { properties: {}, required: [] },
      author: FARMER,
    },
    {
      schemaId: 'did:schema:land',
      version: '1.0.0',
      name: 'Land Ownership Credential',
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      tags: ['agriculture'],
      formats: ['vc+sd-jwt'],
      vct: 'land-ownership-credential',
      display: [{ name: 'Land Ownership Credential', locale: 'en-US' }],
      schema: { properties: {}, required: [] },
      author: LAND,
    },
  ];

  /** The service reads configuration at construction, so env is set first. */
  async function advertisedNames(env: Record<string, string>) {
    process.env = { ...ORIGINAL_ENV, PUBLIC_URL: 'https://issuer.example', ...env };
    jest.resetModules();

    const { Oid4vciService } = await import('./oid4vci.service');
    const { MemoryStoreService } = await import('../session/memory-store.service');

    const schema = { getOid4vciConfigs: jest.fn().mockResolvedValue(CONFIGS) } as any;
    const tokens = {
      authorizationServers: () => ['https://issuer.example'],
      getIssuerDid: () => FARMER,
    } as any;

    const service = new Oid4vciService(
      new MemoryStoreService() as any,
      {} as any,
      schema,
      tokens,
      {} as any,
      {} as any,
    );
    const metadata = (await service.issuerMetadata()) as Record<string, any>;
    return Object.values(metadata.credential_configurations_supported as Record<string, any>).map(
      (c: any) => c.display[0].name,
    );
  }

  it('advertises every published credential by default, as it always has', async () => {
    // The flag is opt-in precisely so that a single-issuer deployment upgrading to
    // this build sees no change at all.
    const names = await advertisedNames({ ISSUER_DID: FARMER });
    expect(names).toEqual(['Farmer Identity Credential', 'Land Ownership Credential']);
  });

  it('advertises only its own once asked to', async () => {
    const names = await advertisedNames({
      ISSUER_DID: FARMER,
      ADVERTISE_OWN_CREDENTIALS_ONLY: 'true',
    });
    expect(names).toEqual(['Farmer Identity Credential']);
  });

  it('the other issuer sees only its own, from the same schema service', async () => {
    process.env = {
      ...ORIGINAL_ENV,
      PUBLIC_URL: 'https://issuer.example',
      ISSUER_DID: LAND,
      ADVERTISE_OWN_CREDENTIALS_ONLY: 'true',
    };
    jest.resetModules();
    const { Oid4vciService } = await import('./oid4vci.service');
    const { MemoryStoreService } = await import('../session/memory-store.service');
    const service = new Oid4vciService(
      new MemoryStoreService() as any,
      {} as any,
      { getOid4vciConfigs: jest.fn().mockResolvedValue(CONFIGS) } as any,
      { authorizationServers: () => ['https://issuer.example'], getIssuerDid: () => LAND } as any,
      {} as any,
      {} as any,
    );
    const metadata = (await service.issuerMetadata()) as Record<string, any>;
    const names = Object.values(
      metadata.credential_configurations_supported as Record<string, any>,
    ).map((c: any) => c.display[0].name);
    expect(names).toEqual(['Land Ownership Credential']);
  });

  it('ignores the flag when no issuer DID is configured', async () => {
    // Filtering on an empty DID would advertise nothing, which is a worse failure
    // than advertising too much: it looks exactly like the schema service being
    // down, and there would be nothing in the metadata to say otherwise.
    const names = await advertisedNames({ ADVERTISE_OWN_CREDENTIALS_ONLY: 'true' });
    expect(names).toEqual(['Farmer Identity Credential', 'Land Ownership Credential']);
  });

  it('warns rather than going quiet when nothing matches', async () => {
    process.env = {
      ...ORIGINAL_ENV,
      PUBLIC_URL: 'https://issuer.example',
      ISSUER_DID: 'did:web:issuer.example:nobody',
      ADVERTISE_OWN_CREDENTIALS_ONLY: 'true',
    };
    jest.resetModules();

    // The spy has to be taken from the SAME module registry the service will
    // import from. Spying before resetModules() patches a Logger the service
    // never sees, and the assertion then fails for a reason that has nothing to
    // do with the behaviour under test.
    const { Logger } = await import('@nestjs/common');
    const warn = jest.spyOn(Logger.prototype, 'warn').mockImplementation();

    const { Oid4vciService } = await import('./oid4vci.service');
    const { MemoryStoreService } = await import('../session/memory-store.service');
    const service = new Oid4vciService(
      new MemoryStoreService() as any,
      {} as any,
      { getOid4vciConfigs: jest.fn().mockResolvedValue(CONFIGS) } as any,
      { authorizationServers: () => ['https://issuer.example'], getIssuerDid: () => FARMER } as any,
      {} as any,
      {} as any,
    );
    const metadata = (await service.issuerMetadata()) as Record<string, any>;

    expect(Object.keys(metadata.credential_configurations_supported as object)).toEqual([]);
    // An issuer advertising nothing is indistinguishable from a broken schema
    // service unless it says why.
    expect(warn).toHaveBeenCalledWith(expect.stringContaining('no published schema is authored by it'));
    warn.mockRestore();
  });
});
