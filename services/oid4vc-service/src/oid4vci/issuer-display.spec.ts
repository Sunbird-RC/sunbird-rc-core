// Issuer-level `display` exists for one journey: a wallet listing issuers has
// nothing else to label the entry with, and shows a bare hostname without it. So
// these assertions are about what a wallet would render.
describe('credential issuer display metadata', () => {
  const ORIGINAL_ENV = process.env;

  afterEach(() => {
    process.env = ORIGINAL_ENV;
    jest.resetModules();
  });

  /**
   * The service reads its configuration at construction, so the environment is
   * set first and the module re-required afterwards.
   */
  async function issuerMetadata(env: Record<string, string>) {
    process.env = { ...ORIGINAL_ENV, PUBLIC_URL: 'https://issuer.example', ...env };
    jest.resetModules();

    const { Oid4vciService } = await import('./oid4vci.service');
    const { MemoryStoreService } = await import('../session/memory-store.service');

    const schema = { getOid4vciConfigs: jest.fn().mockResolvedValue([]) } as any;
    const tokens = {
      authorizationServers: () => ['https://issuer.example'],
      getIssuerDid: () => 'did:web:issuer.example:authority',
    } as any;

    const service = new Oid4vciService(
      new MemoryStoreService() as any,
      {} as any,
      schema,
      tokens,
      {} as any,
      {} as any,
    );
    return (await service.issuerMetadata()) as Record<string, any>;
  }

  it('publishes the configured issuer name, so a wallet can label the entry', async () => {
    const metadata = await issuerMetadata({
      ISSUER_DISPLAY_NAME: 'National Identity Authority',
      ISSUER_DISPLAY_LOGO_URI: 'https://issuer.example/logo.png',
    });

    expect(metadata.display).toEqual([
      {
        name: 'National Identity Authority',
        locale: 'en-US',
        logo: { uri: 'https://issuer.example/logo.png' },
      },
    ]);
  });

  it('omits display entirely when no name is configured', async () => {
    // Absent, not blank: an empty name looks deliberate to a wallet, while
    // absence lets it fall back to something of its own choosing.
    const metadata = await issuerMetadata({});
    expect('display' in metadata).toBe(false);
  });
});
