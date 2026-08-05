import { BadRequestException, ForbiddenException } from '@nestjs/common';
import { Oid4vciService } from './oid4vci.service';
import { MemoryStoreService } from '../session/memory-store.service';

// The transaction code is what makes a staff-handed-over QR safe: the offer URI
// is a bearer token, so anyone who photographs the QR can redeem it unless a
// second factor is required. Until this change the PIN check accepted ANY
// non-empty value, which meant the flow only looked protected.
//
// Also covers the issuer-staff gate on offer creation, since both live on the
// same endpoint pair and both fail dangerously if they fail open.
describe('Oid4vciService transaction codes', () => {
  const ORIGINAL_ENV = process.env;
  let schema: any;
  let tokens: any;
  let credentials: any;
  let pop: any;
  let registry: any;

  const SCHEMA_ID = 'did:schema:farmer';

  beforeEach(() => {
    process.env = { ...ORIGINAL_ENV };
    process.env.PUBLIC_URL = 'https://issuer.example';
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
            properties: { farmerId: { type: 'string' }, name: { type: 'string' } },
            required: ['farmerId', 'name'],
          },
        },
      ]),
    };
    tokens = {
      getIssuerDid: jest.fn().mockReturnValue('did:web:issuer.example:authority'),
      mintAccessToken: jest.fn().mockResolvedValue('minted.access.token'),
      validateAccessToken: jest.fn(),
      authorizationServers: jest.fn().mockReturnValue(['https://issuer.example']),
    };
    credentials = { issue: jest.fn() };
    pop = { verifyJwtProof: jest.fn() };
    registry = { enabled: false, holderBundle: jest.fn() };
  });

  afterEach(() => {
    process.env = ORIGINAL_ENV;
  });

  const makeService = () =>
    new Oid4vciService(
      new MemoryStoreService() as any,
      credentials,
      schema,
      tokens,
      pop,
      registry,
    );

  const offerBody = (txCodeRequired: boolean) => ({
    credential_configuration_id: SCHEMA_ID,
    claims: { farmerId: 'FRM-000123', name: 'Ravi Kumar' },
    tx_code_required: txCodeRequired,
  });

  async function redeem(service: Oid4vciService, offer: any, pin?: string) {
    const code =
      offer.credential_offer.grants['urn:ietf:params:oauth:grant-type:pre-authorized_code'][
        'pre-authorized_code'
      ];
    return service.token({
      grant_type: 'urn:ietf:params:oauth:grant-type:pre-authorized_code',
      'pre-authorized_code': code,
      ...(pin === undefined ? {} : { tx_code: pin }),
    });
  }

  it('generates a 6-digit PIN and returns it to the creator only', async () => {
    const service = makeService();
    const offer = await service.createOffer(offerBody(true));

    expect(offer.tx_code).toMatch(/^\d{6}$/);
    // The PIN must NOT ride along inside the offer object: the wallet
    // dereferences that from the QR, so a PIN in there would prove nothing about
    // who received the code.
    expect(JSON.stringify(offer.credential_offer)).not.toContain(offer.tx_code);
    // It also must not leak through the wallet-facing dereference endpoint.
    const dereferenced = await service.getOffer(offer.offer_id);
    expect(JSON.stringify(dereferenced)).not.toContain(offer.tx_code);
    // The offer does still advertise that a code is needed.
    expect(dereferenced.grants['urn:ietf:params:oauth:grant-type:pre-authorized_code'].tx_code)
      .toEqual({ input_mode: 'numeric', length: 6 });
  });

  it('accepts the correct PIN', async () => {
    const service = makeService();
    const offer = await service.createOffer(offerBody(true));
    const res = await redeem(service, offer, offer.tx_code);
    expect(res.access_token).toBe('minted.access.token');
  });

  it('rejects a wrong PIN', async () => {
    const service = makeService();
    const offer = await service.createOffer(offerBody(true));
    const wrong = offer.tx_code === '000000' ? '111111' : '000000';
    await expect(redeem(service, offer, wrong)).rejects.toThrow(/bad transaction code/);
  });

  it('rejects a missing PIN when one is required', async () => {
    const service = makeService();
    const offer = await service.createOffer(offerBody(true));
    await expect(redeem(service, offer, undefined)).rejects.toThrow(/tx_code required/);
  });

  it('burns the offer on a wrong PIN, so the code cannot be brute-forced', async () => {
    // A 6-digit secret would fall to a few hundred thousand guesses if the
    // pre-auth code survived a failed attempt. It is consumed before the PIN is
    // checked, so one wrong guess ends the offer.
    const service = makeService();
    const offer = await service.createOffer(offerBody(true));
    const wrong = offer.tx_code === '000000' ? '111111' : '000000';

    await expect(redeem(service, offer, wrong)).rejects.toThrow(/bad transaction code/);
    // Even the RIGHT PIN now fails, because the code is spent.
    await expect(redeem(service, offer, offer.tx_code)).rejects.toThrow(/bad or used code/);
  });

  it('issues no PIN, and requires none, when tx_code_required is false', async () => {
    const service = makeService();
    const offer = await service.createOffer(offerBody(false));
    expect(offer.tx_code).toBeUndefined();
    const res = await redeem(service, offer, undefined);
    expect(res.access_token).toBe('minted.access.token');
  });

  it('produces different PINs across offers', async () => {
    const service = makeService();
    const pins = new Set<string>();
    for (let i = 0; i < 12; i++) {
      const offer = await service.createOffer(offerBody(true));
      pins.add(offer.tx_code as string);
    }
    // A fixed or weakly-seeded generator would collapse these. 12 draws from
    // 10^6 colliding at all is ~0.007%, so more than one repeat means a bug.
    expect(pins.size).toBeGreaterThanOrEqual(11);
  });

  it('publishes a space-free OAuth scope', async () => {
    // Space is the scope delimiter, so a raw display name makes Keycloak reject
    // the wallet's authorization request with invalid_scope before any login.
    const service = makeService();
    // The return type is a draft13/final-1.0 union, so narrow via any.
    const md = (await service.issuerMetadata()) as any;
    const cfg = Object.values(md.credential_configurations_supported as Record<string, any>)[0] as any;
    expect(cfg.scope).toBe('farmer-land-holding-credential');
    expect(cfg.scope).not.toMatch(/\s/);
  });

  describe('authorization_code offers (holder signs in, no PIN)', () => {
    const KC = 'https://issuer.example/auth/realms/sunbird-rc';
    beforeEach(() => {
      process.env.KEYCLOAK_PUBLIC_URL = 'https://issuer.example/auth';
      tokens.authorizationServers.mockReturnValue([KC, 'https://issuer.example']);
    });

    it('emits an authorization_code grant pointing at Keycloak, and no pre-auth code', async () => {
      const service = makeService();
      const offer = await service.createOffer({ ...offerBody(false), grant: 'authorization_code' });
      const grants = offer.credential_offer.grants;

      expect(Object.keys(grants)).toEqual(['authorization_code']);
      // Keycloak owns this grant — naming this service would send the wallet to
      // an /authorize endpoint that does not exist here.
      expect(grants.authorization_code.authorization_server).toBe(KC);
      expect(grants.authorization_code.issuer_state).toBe(offer.offer_id);
      // A bearer pre-auth code must NOT also be offered: that would be a second,
      // unauthenticated way to collect the same credential.
      expect(grants['urn:ietf:params:oauth:grant-type:pre-authorized_code']).toBeUndefined();
      expect(JSON.stringify(offer.credential_offer)).not.toContain('pre-authorized_code');
    });

    it('issues no PIN, since the login is the authentication', async () => {
      const service = makeService();
      const offer = await service.createOffer({
        ...offerBody(true), // asks for a tx_code…
        grant: 'authorization_code',
      });
      // …and is deliberately ignored: the wallet has nowhere to obtain one, and
      // a PIN exists only to substitute for authentication.
      expect(offer.tx_code).toBeUndefined();
      expect(JSON.stringify(offer.credential_offer)).not.toContain('tx_code');
    });

    it('survives the wallet-facing dereference unchanged', async () => {
      const service = makeService();
      const created = await service.createOffer({
        ...offerBody(false),
        grant: 'authorization_code',
      });
      const fetched = await service.getOffer(created.offer_id);
      expect(Object.keys(fetched.grants)).toEqual(['authorization_code']);
      expect(fetched.grants.authorization_code.authorization_server).toBe(KC);
    });

    it('refuses when no Keycloak is configured, rather than emitting a dead grant', async () => {
      delete process.env.KEYCLOAK_PUBLIC_URL;
      const service = makeService();
      await expect(
        service.createOffer({ ...offerBody(false), grant: 'authorization_code' }),
      ).rejects.toThrow(/authorization_code offers require KEYCLOAK_PUBLIC_URL/);
    });

    it('still defaults to pre-authorized_code when no grant is named', async () => {
      const service = makeService();
      const offer = await service.createOffer(offerBody(true));
      expect(
        offer.credential_offer.grants['urn:ietf:params:oauth:grant-type:pre-authorized_code'],
      ).toBeDefined();
      expect(offer.tx_code).toMatch(/^\d{6}$/);
    });
  });

  describe('authorization_server in the offer grant', () => {
    // A Credo-based wallet REFUSES an offer whose grant omits
    // `authorization_server` when issuer metadata advertises more than one —
    // surfacing to the user only as "something went wrong". This is the
    // regression test for that.
    it('names the authorization server when metadata advertises several', async () => {
      tokens.authorizationServers.mockReturnValue([
        'https://issuer.example/auth/realms/sunbird-rc',
        'https://issuer.example',
      ]);
      const service = makeService();
      const offer = await service.createOffer(offerBody(false));
      const grant =
        offer.credential_offer.grants['urn:ietf:params:oauth:grant-type:pre-authorized_code'];
      // Must be THIS service: it mints the pre-authorized token, not the realm.
      expect(grant.authorization_server).toBe('https://issuer.example');
    });

    it('omits it when only one authorization server is advertised', async () => {
      // Sending it needlessly is legal but noise; more importantly this pins the
      // single-AS case so the field cannot start pointing somewhere wrong.
      tokens.authorizationServers.mockReturnValue(['https://issuer.example']);
      const service = makeService();
      const offer = await service.createOffer(offerBody(false));
      const grant =
        offer.credential_offer.grants['urn:ietf:params:oauth:grant-type:pre-authorized_code'];
      expect(grant.authorization_server).toBeUndefined();
    });

    it('is present on the wallet-facing dereference too, not just creation', async () => {
      // The wallet reads the offer from GET /oid4vc/offer/:id, so the field has
      // to survive that path as well.
      tokens.authorizationServers.mockReturnValue(['https://a.example', 'https://issuer.example']);
      const service = makeService();
      const created = await service.createOffer(offerBody(false));
      const fetched = await service.getOffer(created.offer_id);
      const grant = fetched.grants['urn:ietf:params:oauth:grant-type:pre-authorized_code'];
      expect(grant.authorization_server).toBe('https://issuer.example');
    });
  });

  describe('issuer-staff gate on offer creation', () => {
    it('allows unauthenticated creation when OFFER_REQUIRES_STAFF is unset', async () => {
      // Default is open for backwards compatibility; the service logs a warning.
      const service = makeService();
      await expect(service.createOffer(offerBody(false))).resolves.toHaveProperty('offer_id');
      expect(tokens.validateAccessToken).not.toHaveBeenCalled();
    });

    it('refuses a token without the issuer-staff role', async () => {
      process.env.OFFER_REQUIRES_STAFF = 'true';
      process.env.KEYCLOAK_PUBLIC_URL = 'https://issuer.example/auth';
      tokens.validateAccessToken.mockResolvedValue({
        payload: { sub: 'user-1' },
        source: 'keycloak',
        roles: ['citizen'],
      });
      const service = makeService();
      await expect(service.createOffer(offerBody(false), 'Bearer x')).rejects.toThrow(
        ForbiddenException,
      );
    });

    it('accepts a token with the issuer-staff role', async () => {
      process.env.OFFER_REQUIRES_STAFF = 'true';
      process.env.KEYCLOAK_PUBLIC_URL = 'https://issuer.example/auth';
      tokens.validateAccessToken.mockResolvedValue({
        payload: { sub: 'staff-1' },
        source: 'keycloak',
        roles: ['issuer-staff'],
      });
      const service = makeService();
      await expect(service.createOffer(offerBody(false), 'Bearer x')).resolves.toHaveProperty(
        'offer_id',
      );
    });

    it('refuses a pre-auth token, which carries no roles at all', async () => {
      // An offer-scoped access token must not be reusable to create more offers.
      process.env.OFFER_REQUIRES_STAFF = 'true';
      process.env.KEYCLOAK_PUBLIC_URL = 'https://issuer.example/auth';
      tokens.validateAccessToken.mockResolvedValue({
        payload: { sub: 'some-offer-id' },
        source: 'preauth',
      });
      const service = makeService();
      await expect(service.createOffer(offerBody(false), 'Bearer x')).rejects.toThrow(
        ForbiddenException,
      );
    });

    it('honours a realm that names the staff role differently', async () => {
      // The role lives in the deployment's realm, not here. It used to be a
      // literal `issuer-staff`, so a realm calling it anything else could not use
      // this gate at all without a code change.
      process.env.OFFER_REQUIRES_STAFF = 'true';
      process.env.KEYCLOAK_PUBLIC_URL = 'https://issuer.example/auth';
      process.env.OFFER_STAFF_ROLE = 'credential-issuer';
      tokens.validateAccessToken.mockResolvedValue({
        payload: { sub: 'staff-1' },
        source: 'keycloak',
        roles: ['credential-issuer'],
      });
      const service = makeService();
      await expect(service.createOffer(offerBody(false), 'Bearer x')).resolves.toHaveProperty(
        'offer_id',
      );
    });

    it('stops accepting the default role once another is configured', async () => {
      // The configured name REPLACES the default rather than adding to it —
      // otherwise configuring the gate would silently widen who can issue.
      process.env.OFFER_REQUIRES_STAFF = 'true';
      process.env.KEYCLOAK_PUBLIC_URL = 'https://issuer.example/auth';
      process.env.OFFER_STAFF_ROLE = 'credential-issuer';
      tokens.validateAccessToken.mockResolvedValue({
        payload: { sub: 'staff-1' },
        source: 'keycloak',
        roles: ['issuer-staff'],
      });
      const service = makeService();
      await expect(service.createOffer(offerBody(false), 'Bearer x')).rejects.toThrow(
        ForbiddenException,
      );
    });

    it('fails closed when the gate is on but Keycloak is not configured', async () => {
      // Asking for a role check with no way to check roles must not degrade into
      // no check at all.
      process.env.OFFER_REQUIRES_STAFF = 'true';
      delete process.env.KEYCLOAK_PUBLIC_URL;
      const service = makeService();
      await expect(service.createOffer(offerBody(false), 'Bearer x')).rejects.toThrow(
        BadRequestException,
      );
    });
  });
});
