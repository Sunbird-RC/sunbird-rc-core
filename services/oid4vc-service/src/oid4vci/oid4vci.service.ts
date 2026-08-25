import {
  BadRequestException,
  ForbiddenException,
  Inject,
  Injectable,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { v4 as uuid } from 'uuid';
import * as crypto from 'crypto';
import { SESSION_STORE, SessionStore } from '../session/session-store.interface';
import { CredentialsClient } from '../clients/credentials.client';
import { SchemaClient, Oid4vciSchemaConfig } from '../clients/schema.client';
import { TokenService, ValidatedToken } from './token.service';
import { PopService } from './pop.service';
import { loadConfig } from '../config/configuration';
import { digestMultibase } from '../utils/multibase.util';
import { normalizeVct, slugifyVct, isAbsoluteHttpUri } from './vct.util';
import { ClaimSourceFactory } from '../claims/claim-source.factory';
import {
  ClaimSourceNotConfiguredError,
  ClaimSourceUnavailableError,
  SubjectNotFoundError,
  type ClaimSourceProvider,
} from '../claims/claim-source.interface';

const PREAUTH_GRANT = 'urn:ietf:params:oauth:grant-type:pre-authorized_code';


interface OfferSession {
  credentialConfigurationId: string;
  format: string;
  schemaId: string;
  schemaVersion: string;
  schemaName: string;
  // vc+sd-jwt only: the SAME normalized vct published in issuer metadata
  // (see vct.util.ts) — passed through to credentials-service at issuance so
  // the issued credential's vct matches what a wallet resolved from metadata.
  vct?: string;
  issuerDid: string;
  claims: Record<string, any>;
  preAuthCode: string;
  txCodeRequired: boolean;
  /** True when the offer carries authorization_code instead of pre-authorized_code. */
  authCodeGrant?: boolean;
  // The actual PIN, when one is required. Held server-side ONLY: it is returned
  // to the offer's creator (so a portal can display it) and never placed in the
  // offer object, which would defeat the purpose — the QR would then carry its
  // own PIN and prove nothing about who received it.
  txCode?: string;
  tags: string[];
  // Optional deferred: when set, issuance waits on this claim id.
  deferredClaimId?: string;
  // W3C VC Render Method (https://www.w3.org/TR/vc-render-method/) entry to
  // embed in the issued credential, pre-resolved at offer-creation time
  // (URL + digest) since only ldp_vc/jwt_vc_json carry a full W3C-shaped VC
  // object that has somewhere to put it.
  renderMethod?: Record<string, any>;
  // mso_mdoc (ISO/IEC 18013-5) docType/namespace config, carried from the
  // schema's oid4vciConfig.mdoc when format === 'mso_mdoc'.
  docType?: string;
  namespace?: string;
  elementMapping?: Record<string, { namespace: string; elementIdentifier: string }>;
}

// Core OID4VCI orchestration. Owns short-lived session state (offers, codes,
// nonces, deferred txns) and delegates signing/verification to existing services.
@Injectable()
export class Oid4vciService {
  private readonly logger = new Logger(Oid4vciService.name);
  private readonly config = loadConfig();
  private offerAuthWarned = false;

  constructor(
    @Inject(SESSION_STORE) private readonly store: SessionStore,
    private readonly credentials: CredentialsClient,
    private readonly schema: SchemaClient,
    private readonly tokens: TokenService,
    private readonly pop: PopService,
    // Not a RegistryClient: which system of record backs a credential type is a
    // per-type decision, so this service depends on the chooser rather than on any
    // one system of record.
    private readonly claimSources: ClaimSourceFactory,
  ) {}

  // --- Metadata ------------------------------------------------------------

  async issuerMetadata() {
    const configs = await this.schema.getOid4vciConfigs();
    const supported: Record<string, any> = {};
    for (const cfg of configs) {
      for (const format of cfg.formats) {
        // Schema *names* are user-chosen display labels, not guaranteed
        // unique (found live: three separate credential-schema records all
        // named "Age Verification Credential", onboarded at different
        // times, only some supporting vc+sd-jwt) — keying this map by
        // name+format let two different schemas compute the exact same id,
        // silently overwriting one schema's metadata entry with another's
        // whenever they shared a name+format pair. `schemaId` is assigned
        // per-record by credential-schema and is always unique, so it's the
        // only safe basis for the actual OID4VCI `credential_configuration_id`.
        const id = cfg.formats.length > 1 ? `${cfg.schemaId}_${format}` : cfg.schemaId;
        // mso_mdoc has no credentialSubject/type concept — OID4VCI's own
        // profile for it uses `doctype` and per-namespace `claims` display
        // instead of `credential_definition.type`, mirroring how `vct` is
        // the vc+sd-jwt-only field just above.
        const isMdoc = format === 'mso_mdoc';
        supported[id] = {
          format,
          // An OAuth scope, so it MUST NOT contain spaces — space is the scope
          // delimiter. Publishing the raw schema name ("Farmer Land Holding")
          // made Keycloak reject the wallet's authorization request outright with
          // `invalid_scope`, so the holder never even reached a login form.
          // Slugified to stay a single valid token.
          scope: slugifyVct(cfg.name),
          cryptographic_binding_methods_supported: ['did:web', 'did:key', 'jwk'],
          credential_signing_alg_values_supported: isMdoc ? ['ES256'] : ['ES256', 'Ed25519Signature2020'],
          proof_types_supported: { jwt: { proof_signing_alg_values_supported: ['ES256'] } },
          // vct MUST be a URI if it contains a ':', and — found live against
          // walt.id's wallet — some wallets resolve EVERY vct as a URL
          // regardless of that spec carve-out, so a bare display name like
          // "National Identity Credential" crashes them ("Illegal character
          // in path" on the space). normalizeVct() turns any non-URI schema
          // name into `<publicUrl>/vct/<slug>`, which vct.controller.ts then
          // actually serves as SD-JWT VC Type Metadata.
          ...(format === 'vc+sd-jwt' ? { vct: normalizeVct(cfg.vct, this.config.publicUrl) } : {}),
          // Same locale requirement as the vct document — see withDisplayLocale.
          display: this.withDisplayLocale(cfg.display),
          // `credential_definition` is the W3C-format parameter (ldp_vc /
          // jwt_vc_json). SD-JWT VC identifies its type with `vct` (set above)
          // and mdoc with `doctype`, and neither may carry it: a strict wallet
          // parses each configuration against its format's schema and discards
          // the whole entry when an unexpected member is present — confirmed
          // live, a real wallet dropped every vc+sd-jwt configuration, leaving
          // `offeredCredentialConfigurations` empty and the offer unusable
          // ("'credentialConfigurationIds' may not be empty").
          ...(isMdoc
            ? {
                doctype: cfg.mdoc?.docType,
                claims: { [cfg.mdoc?.namespace]: {} },
              }
            : format === 'vc+sd-jwt'
              ? {}
              : {
                  credential_definition: {
                    type: ['VerifiableCredential', cfg.name],
                  },
                }),
          // internal hint (not part of the spec response consumers care about)
          _schema: { id: cfg.schemaId, version: cfg.version, tags: cfg.tags },
        };
      }
    }

    const base = {
      credential_issuer: this.config.publicUrl,
      // With Keycloak configured this lists the realm first, so a wallet doing
      // authorization_code + PKCE discovers the realm's own metadata and
      // authenticates the holder there. This service implements no /authorize:
      // Keycloak is the authorization server, we are the resource server.
      authorization_servers: this.tokens.authorizationServers(),
      credential_endpoint: `${this.config.publicUrl}/oid4vc/credential`,
      nonce_endpoint: `${this.config.publicUrl}/oid4vc/nonce`,
      deferred_credential_endpoint: `${this.config.publicUrl}/oid4vc/deferred`,
      notification_endpoint: `${this.config.publicUrl}/oid4vc/notification`,
    };

    // Draft-13 (Inji) uses `credentials_supported`; final 1.0 uses
    // `credential_configurations_supported`.
    if (this.config.draft13CompatMode) {
      return { ...base, credentials_supported: supported };
    }
    return { ...base, credential_configurations_supported: supported };
  }

  // SD-JWT VC Type Metadata (draft-ietf-oauth-sd-jwt-vc §11) for a vct we
  // normalized into `<publicUrl>/vct/<slug>` (see vct.util.ts / issuerMetadata
  // above). Per the spec, when vct is a plain HTTPS URI (no .well-known
  // indirection), the URI itself is fetched directly for this document — so
  // this handler serves the same slug the metadata already advertises.
  // Multiple schemas can legitimately share one vct slug (same credential
  // type, different issuers/versions) — the first vc+sd-jwt match is
  // authoritative for display purposes, matching issuerMetadata()'s own
  // per-name (not per-schemaId) `vct` derivation.
  async getVctTypeMetadata(slug: string) {
    const configs = await this.schema.getOid4vciConfigs();
    const cfg = configs.find(
      (c) =>
        c.formats.includes('vc+sd-jwt') &&
        !isAbsoluteHttpUri(c.vct) &&
        slugifyVct(c.vct) === slug,
    );
    if (!cfg) {
      throw new NotFoundException(`No vc+sd-jwt credential type found for vct slug '${slug}'`);
    }
    return {
      vct: normalizeVct(cfg.vct, this.config.publicUrl),
      name: cfg.name,
      // Every display entry MUST carry a locale. SD-JWT VC Type Metadata
      // validation in @sd-jwt/sd-jwt-vc — which Credo, and therefore Paradym,
      // runs on the fetched document — rejects the whole credential with
      //   "Either locale (preferred) or lang (spec name, deprecated) MUST be
      //    defined on claim display entry"
      // and the holder sees only "something went wrong". Schemas are authored by
      // hand through the schema API and routinely omit it, so default here rather
      // than depending on every author remembering.
      display: this.withDisplayLocale(cfg.display),
    };
  }

  /**
   * Ensures each display entry has a `locale`, defaulting to en-US.
   *
   * Deliberately does not invent any other field: a missing locale is the one
   * omission that makes the document invalid rather than merely sparse.
   */
  private withDisplayLocale(display: Record<string, any>[] | undefined): Record<string, any>[] {
    const entries = Array.isArray(display) && display.length ? display : [{}];
    return entries.map((d) => ({
      ...d,
      ...(d?.locale || d?.lang ? {} : { locale: this.config.defaultDisplayLocale }),
    }));
  }

  // --- Offer ---------------------------------------------------------------

  /**
   * Requires a staff Keycloak token, when configured to.
   *
   * The role NAME is configuration (`OFFER_STAFF_ROLE`), because it lives in the
   * deployment's realm rather than here — a realm that calls it
   * `credential-issuer` should not need a code change to use this gate.
   *
   * Deliberately opt-in (`OFFER_REQUIRES_STAFF`): turning it on unconditionally
   * would break every existing caller of this endpoint, including the verifier
   * console's sample-issuance button. The one-shot warning makes the open state
   * visible in the log rather than assumed to be intentional.
   */
  private async assertStaff(authHeader?: string): Promise<void> {
    const staffRole = this.config.offerStaffRole;
    if (!this.config.offerRequiresStaff) {
      if (!this.offerAuthWarned) {
        this.offerAuthWarned = true;
        this.logger.warn(
          'POST /oid4vc/offer is UNAUTHENTICATED and trusts caller-supplied claims — ' +
            'anyone who can reach it can mint a credential about anyone. ' +
            `Set OFFER_REQUIRES_STAFF=true (with KEYCLOAK_PUBLIC_URL) to require the ${staffRole} role.`,
        );
      }
      return;
    }
    if (!this.config.keycloak.enabled) {
      // Failing closed: asking for a role check with no way to check roles must
      // not silently degrade into no check at all.
      throw new BadRequestException(
        'server_error: OFFER_REQUIRES_STAFF is set but KEYCLOAK_PUBLIC_URL is not configured',
      );
    }
    const token = await this.tokens.validateAccessToken(authHeader);
    if (token.source !== 'keycloak' || !(token.roles ?? []).includes(staffRole)) {
      throw new ForbiddenException(`insufficient_scope: the '${staffRole}' role is required`);
    }
  }

  async createOffer(
    body: {
      credential_configuration_id: string;
      claims: Record<string, any>;
      format?: string;
      tx_code_required?: boolean;
      tags?: string[];
      deferred_claim_id?: string;
      /**
       * Which grant the offer should carry.
       *
       * `pre-authorized_code` (default) hands the holder a bearer offer, gated by
       * a transaction code the issuer distributes out of band.
       *
       * `authorization_code` instead sends the holder to Keycloak to sign in:
       * the wallet runs authorization_code + PKCE against the realm, and the
       * credential is issued from the registry record belonging to whoever
       * authenticated. No PIN, because the login itself establishes identity —
       * and nobody can collect a credential without valid Keycloak credentials.
       */
      grant?: 'pre-authorized_code' | 'authorization_code';
    },
    authHeader?: string,
  ) {
    // Offer creation takes caller-supplied claims, so an unauthenticated caller
    // can mint a credential asserting anything about anyone. Gated on the
    // issuer-staff realm role when OFFER_REQUIRES_STAFF is on; left open by
    // default so existing deployments keep working, which is why the warning
    // below exists rather than a silent default.
    await this.assertStaff(authHeader);
    const configs = await this.schema.getOid4vciConfigs();
    // Prefer an exact match on the stable, always-unique schemaId — this is
    // what issuerMetadata() now actually publishes as credential_configuration_id
    // (see comment there), and the only lookup that's unambiguous regardless
    // of how many schemas happen to share a display name. Schema names are
    // kept as a convenience-only fallback for callers still passing the
    // human-readable label (e.g. hand-typed test curls).
    const rawId = body.credential_configuration_id;
    // Resolve in the order a spec-compliant wallet would: the exact config id
    // published in issuer metadata. That id is the bare schemaId for
    // single-format schemas, or `schemaId_<format>` for multi-format ones —
    // and the wallet references it directly, WITHOUT sending a separate
    // `format`. So derive the format from the suffix when it isn't supplied,
    // rather than 404ing (found live: posting the metadata key
    // `did:schema:..._jwt_vc_json` with no `format` failed the old
    // schemaId/schemaId_${format} match and was rejected).
    let derivedFormat: string | undefined = body.format;
    let cfg = configs.find((c) => c.schemaId === rawId);
    if (cfg) {
      derivedFormat = derivedFormat || cfg.formats[0];
    } else {
      for (const c of configs) {
        const suffix = c.formats.find((f) => `${c.schemaId}_${f}` === rawId);
        if (suffix) {
          cfg = c;
          derivedFormat = derivedFormat || suffix;
          break;
        }
      }
    }
    if (!cfg) {
      const candidates = configs.filter(
        (c) =>
          c.name === rawId ||
          c.formats.some((f) => `${c.name}_${f}` === rawId),
      );
      // Multiple schemas can share the same display name (found live: three
      // separate "Age Verification Credential" schemas onboarded at
      // different times, only the newer ones supporting vc+sd-jwt) —
      // picking the first name match unconditionally made the SD-JWT-capable
      // schemas permanently unreachable by name. Prefer whichever candidate
      // actually supports the requested format; fall back to the first
      // match when no format was specified or none support it (surfaces the
      // error below). Still ambiguous in principle if two same-named
      // schemas both support the requested format — pass schemaId to avoid
      // that entirely.
      cfg =
        (body.format && candidates.find((c) => c.formats.includes(body.format))) ||
        candidates[0];
      // Same suffix-derivation as above, for the name-based fallback.
      if (cfg && !derivedFormat) {
        const matched = cfg;
        derivedFormat = matched.formats.find((f) => `${matched.name}_${f}` === rawId) || matched.formats[0];
      }
    }
    if (!cfg) {
      throw new NotFoundException(
        `Credential configuration '${rawId}' not enabled for OID4VCI`,
      );
    }
    const format = derivedFormat || cfg.formats[0] || 'ldp_vc';
    if (!cfg.formats.includes(format)) {
      throw new BadRequestException(`Format '${format}' not supported for this credential`);
    }
    if (format === 'mso_mdoc' && !cfg.mdoc) {
      throw new BadRequestException(
        `Schema '${cfg.schemaId}' is missing oid4vciConfig.mdoc (docType/namespace) required for mso_mdoc`,
      );
    }
    // No real out-of-band PIN storage/comparison exists yet — token() only
    // ever checked that SOME tx_code was submitted, not that it matched
    // anything the issuer actually distributed. An issuer enabling this
    // reasonably believes they've added a second factor; in practice anyone
    // holding the pre-authorized code could redeem it with any 6 digits. A
    // silently no-op security control is worse than an unimplemented one, so
    // refuse to create the offer rather than accept a flag that does nothing.
    if (body.tx_code_required) {
      throw new BadRequestException(
        'tx_code_required is not yet implemented (no PIN storage/verification exists) — omit it',
      );
    }
    // Must match the key issuerMetadata() publishes under
    // credential_configurations_supported so wallets can correlate the offer.
    const configId = cfg.formats.length > 1 ? `${cfg.schemaId}_${format}` : cfg.schemaId;

    const id = uuid();
    const useAuthCode = body.grant === 'authorization_code';
    if (useAuthCode && !this.config.keycloak.enabled) {
      throw new BadRequestException(
        'server_error: authorization_code offers require KEYCLOAK_PUBLIC_URL to be configured',
      );
    }
    const preAuthCode = this.randomToken();
    // A transaction code exists to substitute for authentication. With
    // authorization_code the holder authenticates for real, so a PIN would be
    // redundant friction — and the wallet would have nowhere to get it.
    const txCodeRequired = !useAuthCode && !!body.tx_code_required;
    const txCode = txCodeRequired ? this.randomPin() : undefined;
    const session: OfferSession = {
      credentialConfigurationId: configId,
      format,
      schemaId: cfg.schemaId,
      schemaVersion: cfg.version,
      schemaName: cfg.name,
      vct: format === 'vc+sd-jwt' ? normalizeVct(cfg.vct, this.config.publicUrl) : undefined,
      // Sign as the schema's own author DID when it has one — falls back to
      // the single server-wide ISSUER_DID for schemas authored before this
      // field existed, or left blank. Every schema already requires an
      // `author` DID at creation time; this is the first place it's
      // actually used, rather than every issued credential (regardless of
      // schema) always carrying the same one hardcoded issuer.
      issuerDid: cfg.author || this.tokens.getIssuerDid(),
      claims: body.claims || {},
      preAuthCode,
      txCodeRequired,
      txCode,
      authCodeGrant: useAuthCode,
      tags: body.tags || cfg.tags || [cfg.name],
      deferredClaimId: body.deferred_claim_id,
      renderMethod: this.resolveRenderMethod(cfg, format),
      docType: cfg.mdoc?.docType,
      namespace: cfg.mdoc?.namespace,
      elementMapping: cfg.mdoc?.elementMapping,
    };
    await this.store.set(`oid4vc:offer:${id}`, session, this.config.ttl.offer);
    // Index by pre-auth code for the token endpoint.
    await this.store.set(`oid4vc:code:${preAuthCode}`, { offerId: id }, this.config.ttl.offer);

    const offerObject = this.buildOfferObject(
      configId,
      preAuthCode,
      session.txCodeRequired,
      useAuthCode,
      id,
    );
    const offerUri = `${this.config.publicUrl}/oid4vc/offer/${id}`;
    const qrData = `openid-credential-offer://?credential_offer_uri=${encodeURIComponent(offerUri)}`;

    return {
      offer_id: id,
      credential_offer_uri: offerUri,
      credential_offer: offerObject,
      qr_data: qrData,
      // Returned to the CREATOR only (the issuer portal, which shows it to the
      // holder out of band). Deliberately absent from `credential_offer` above:
      // a PIN travelling inside the QR would prove nothing.
      ...(txCode ? { tx_code: txCode } : {}),
    };
  }

  async getOffer(id: string) {
    const session = await this.store.get<OfferSession>(`oid4vc:offer:${id}`);
    if (!session) throw new NotFoundException('Offer not found or expired');
    return this.buildOfferObject(
      session.credentialConfigurationId,
      session.preAuthCode,
      session.txCodeRequired,
      !!session.authCodeGrant,
      id,
    );
  }

  private buildOfferObject(
    configId: string,
    preAuthCode: string,
    txCodeRequired: boolean,
    authCodeGrant = false,
    offerId?: string,
  ) {
    const authorizationServers = this.tokens.authorizationServers();

    // --- authorization_code: the holder signs in, no PIN ---------------------
    // The wallet sees this grant, runs authorization_code + PKCE against the
    // named authorization server (the Keycloak realm), and only reaches the
    // credential endpoint with a token proving who authenticated. Issuance then
    // resolves that person's own registry record, so a credential cannot be
    // collected without valid Keycloak credentials.
    if (authCodeGrant) {
      return {
        credential_issuer: this.config.publicUrl,
        ...(this.config.draft13CompatMode
          ? { credentials: [configId] }
          : { credential_configuration_ids: [configId] }),
        grants: {
          authorization_code: {
            // Ties the eventual authorization request back to this offer. The
            // wallet echoes it to the authorization server; harmless if unused.
            ...(offerId ? { issuer_state: offerId } : {}),
            // Keycloak owns this grant — NOT this service, which implements no
            // /authorize. Naming it is also mandatory whenever metadata
            // advertises more than one authorization server.
            authorization_server: this.keycloakAuthorizationServer(authorizationServers),
          },
        },
      };
    }

    const grant: any = { 'pre-authorized_code': preAuthCode };
    if (this.config.draft13CompatMode) {
      // draft-13 idiom
      grant.user_pin_required = txCodeRequired;
    } else if (txCodeRequired) {
      grant.tx_code = { input_mode: 'numeric', length: 6 };
    }

    // When issuer metadata advertises MORE THAN ONE authorization server,
    // OID4VCI requires each grant to name the one it applies to. Omitting it is
    // not a soft warning: a Credo-based wallet (Paradym included) refuses the
    // offer outright with
    //   "Credential issuer metadata has 'authorization_server' with multiple
    //    entries, but the credential offer grant did not specify which
    //    authorization server to use."
    // which the user sees only as "something went wrong".
    //
    // This became reachable the moment the Keycloak realm was added alongside
    // this service in authorizationServers(). The pre-authorized_code grant is
    // always served by THIS service — it mints the pre-auth token — so the
    // correct value is our own issuer identifier, not the realm.
    if (authorizationServers.length > 1) {
      grant.authorization_server = this.config.publicUrl;
    }
    return {
      credential_issuer: this.config.publicUrl,
      // draft-13 offers list `credentials`; final-1.0 offers list
      // `credential_configuration_ids` — matches issuerMetadata()'s split.
      ...(this.config.draft13CompatMode
        ? { credentials: [configId] }
        : { credential_configuration_ids: [configId] }),
      grants: { [PREAUTH_GRANT]: grant },
    };
  }

  // --- Token ---------------------------------------------------------------

  async token(body: Record<string, any>) {
    const grantType = body.grant_type;
    if (grantType !== PREAUTH_GRANT) {
      throw new BadRequestException('unsupported_grant_type');
    }
    const code = body['pre-authorized_code'];
    if (!code) throw new BadRequestException('invalid_request: missing pre-authorized_code');

    // Atomic single-use consume of the code index.
    const codeEntry = await this.store.getdel<{ offerId: string }>(`oid4vc:code:${code}`);
    if (!codeEntry) throw new BadRequestException('invalid_grant: bad or used code');
    const session = await this.store.get<OfferSession>(`oid4vc:offer:${codeEntry.offerId}`);
    if (!session) throw new BadRequestException('invalid_grant: offer expired');

    // tx_code / user_pin check. createOffer() now refuses to create a
    // tx_code_required offer at all (no real PIN storage/verification
    // exists), so this only fires for an offer created before that fix and
    // still within its TTL — fail closed rather than accept any non-empty
    // value, since there's nothing genuine to compare it against.
    if (session.txCodeRequired) {
      throw new BadRequestException(
        'invalid_request: tx_code_required offers are no longer supported (no PIN verification exists)',
      );
    }

    const accessToken = await this.tokens.mintAccessToken({
      sub: codeEntry.offerId,
      credential_configuration_id: session.credentialConfigurationId,
    });
    const cNonce = await this.issueNonce();

    return {
      access_token: accessToken,
      token_type: 'Bearer',
      expires_in: this.config.ttl.accessToken,
      c_nonce: cNonce,
      c_nonce_expires_in: this.config.ttl.nonce,
      // draft-13 wallets also read authorization_details / c_nonce here.
    };
  }

  // --- Nonce ---------------------------------------------------------------

  async issueNonce(): Promise<string> {
    const nonce = this.randomToken();
    await this.store.set(`oid4vc:nonce:${nonce}`, '1', this.config.ttl.nonce);
    return nonce;
  }

  // --- Credential ----------------------------------------------------------

  async credential(authHeader: string | undefined, body: Record<string, any>) {
    const token = await this.tokens.validateAccessToken(authHeader);

    // Two ways to arrive here, and they differ in WHO decided the contents.
    //
    // pre-authorized_code: an offer already exists and its claims were fixed
    // when staff created it, so the token's `sub` is just a handle to that.
    //
    // Keycloak: the caller is the holder's own wallet, signed in as a person.
    // Nothing about the credential has been decided yet — and anything the
    // wallet asserts about itself is unverified by construction — so the claims
    // are built here, from the registry record its token names. That is what
    // makes "a holder can only ever get their own credential" true structurally
    // rather than by policy.
    let session: OfferSession;
    let offerId: string;
    if (token.source === 'keycloak') {
      offerId = `self:${token.payload.sub}`;
      session = await this.buildSelfServiceSession(token, body);
    } else {
      offerId = token.payload.sub;
      const existing = await this.store.get<OfferSession>(`oid4vc:offer:${offerId}`);
      if (!existing) throw new BadRequestException('Offer session expired');
      session = existing;
    }

    // Verify holder proof-of-possession.
    const proofJwt = body?.proof?.jwt;
    if (!proofJwt) throw new BadRequestException('Missing proof.jwt');
    // Nonce inside the proof must be a live, single-use c_nonce.
    const proofClaims = this.decodeJwtClaims(proofJwt);
    const nonce = proofClaims?.nonce;
    const nonceValid = await this.store.getdel(`oid4vc:nonce:${nonce}`);
    if (!nonceValid) {
      throw new BadRequestException({ error: 'invalid_or_missing_proof', c_nonce: await this.issueNonce() });
    }
    const popResult = await this.pop.verifyJwtProof(proofJwt, {
      audience: this.config.publicUrl,
      nonce,
    });
    if (!popResult.valid) {
      throw new BadRequestException(`invalid_proof: ${popResult.error}`);
    }

    // Deferred: if the offer is tied to an unresolved claim, return a txn id.
    if (session.deferredClaimId && !(await this.isClaimReady(session.deferredClaimId))) {
      const txId = uuid();
      await this.store.set(
        `oid4vc:deferred:${txId}`,
        {
          offerId,
          holderDid: popResult.holderDid,
          holderJwk: popResult.holderJwk,
          holderKid: popResult.holderKid,
        },
        this.config.ttl.deferred,
      );
      return { transaction_id: txId, c_nonce: await this.issueNonce() };
    }

    const credential = await this.issueForSession(
      session,
      popResult.holderDid,
      popResult.holderJwk,
      popResult.holderKid,
    );
    return { credential, c_nonce: await this.issueNonce(), format: session.format };
  }

  /**
   * Builds an issuance session for a signed-in holder, with claims read from the
   * registry rather than from the request.
   *
   * The wallet chooses only WHICH credential type it wants; every value in it
   * comes from the record its token points at. A wallet that sends `claims` is
   * ignored — silently, because a spec-compliant wallet has no reason to send
   * them on this path and failing the request would be less useful than issuing
   * the correct credential.
   */
  private async buildSelfServiceSession(
    token: ValidatedToken,
    body: Record<string, any>,
  ): Promise<OfferSession> {
    // Deliberately NOT gated on the registry here. Which system of record backs a
    // credential type is resolved per type further down, once the type is known —
    // an authority may keep its records in its own database, and requiring the
    // Sunbird registry to be configured would make issuing depend on adopting it.
    //
    // Which claim identifies the holder is deployment configuration with no
    // default, so distinguish the two ways it can be absent. Reporting an
    // unconfigured service as "your account is not linked" sends an operator
    // looking through Keycloak users for a problem that is in the environment.
    if (!this.config.keycloak.subjectClaim) {
      throw new BadRequestException(
        'server_error: self-service issuance requires KEYCLOAK_SUBJECT_CLAIM — set it to ' +
          "the token claim carrying the holder's registry key (there is no default, " +
          'because the field name belongs to the deployment, not to this service)',
      );
    }
    const subjectId = token.subjectId;
    if (!subjectId) {
      // Authenticated, but their account was never linked to a record. This is a
      // provisioning gap rather than a wallet error, so say so plainly instead of
      // returning something that reads like a protocol failure.
      throw new BadRequestException(
        `credential_request_denied: this account is not linked to a registry record ` +
          `(no '${this.config.keycloak.subjectClaim}' claim). An issuer administrator must link it.`,
      );
    }

    const requestedId = body?.credential_configuration_id || body?.credential_identifier;
    const configs = await this.schema.getOid4vciConfigs();
    // Only what the request actually said. The format is derived from the
    // resolved credential further down — assuming one here (this used to default
    // to 'vc+sd-jwt') made every filter below reject a deployment that publishes
    // only ldp_vc or mso_mdoc, reporting "could not determine the credential
    // type" even with exactly one type published.
    const requestedFormat: string | undefined = body?.format;

    let cfg = configs.find((c) => c.schemaId === requestedId);
    // A multi-format credential is published as `<schemaId>_<format>`, so the id
    // the wallet sent already says which format it wants. Captured rather than
    // discarded: without it, a wallet asking for `…_mso_mdoc` would be answered
    // with whichever format happened to be first.
    let derivedFormat: string | undefined;
    if (!cfg && requestedId) {
      for (const c of configs) {
        const suffix = c.formats.find((f) => `${c.schemaId}_${f}` === requestedId);
        if (suffix) {
          cfg = c;
          derivedFormat = suffix;
          break;
        }
      }
    }
    // OID4VCI lets a credential request identify what it wants in more than one
    // way, and wallets differ. Credo sends neither
    // `credential_configuration_id` nor `credential_identifier` on the
    // authorization_code path, so fall back to the `vct` it does send — matched
    // against the SAME normalised value published in issuer metadata.
    if (!cfg && body?.vct) {
      cfg = configs.find(
        (c) =>
          c.formats.includes('vc+sd-jwt') &&
          normalizeVct(c.vct, this.config.publicUrl) === body.vct,
      );
      // `vct` exists only in SD-JWT VC, so identifying by it settles the format.
      // Required, not cosmetic: Credo sends `vct` and no format, and a credential
      // published as both ldp_vc and vc+sd-jwt would otherwise fall through to
      // its first listed format and answer a vct request with an LDP credential.
      if (cfg) derivedFormat = 'vc+sd-jwt';
    }
    // Last resort, and ONLY when the request named nothing at all: with a single
    // credential type there is no ambiguity about what was meant.
    //
    // Filtered by format only when the request asked for one. Filtering by an
    // assumed format instead would make "the deployment publishes exactly one
    // credential type" fail whenever that type is not the assumed format.
    //
    // Deliberately not applied when the request DID name an id or vct that did
    // not match. Falling back then would hand the holder a different credential
    // from the one they asked for, silently — worse than a clear error.
    const identifiedSomething = Boolean(requestedId || body?.vct);
    if (!cfg && !identifiedSomething) {
      const candidates = requestedFormat
        ? configs.filter((c) => c.formats.includes(requestedFormat))
        : configs;
      if (candidates.length === 1) cfg = candidates[0];
    }
    if (!cfg) {
      this.logger.warn(
        `Self-service credential request did not identify a type. Body keys: ${Object.keys(
          body ?? {},
        ).join(', ')}`,
      );
      throw new BadRequestException(
        `invalid_credential_request: could not determine the credential type from the request ` +
          `(no credential_configuration_id, credential_identifier or known vct)`,
      );
    }
    // Derived here, not assumed at the top, in this precedence:
    //   1. what the request asked for
    //   2. the format encoded in a `<schemaId>_<format>` configuration id
    //   3. the credential's own first published format
    // Mirrors resolveOfferConfig's `derivedFormat || cfg.formats[0]`, so the two
    // issuance paths agree about what a request without a format means.
    const format = requestedFormat || derivedFormat || cfg.formats[0];
    if (!format) {
      throw new BadRequestException(
        `invalid_credential_request: credential type '${cfg.name}' publishes no format`,
      );
    }
    if (!cfg.formats.includes(format)) {
      throw new BadRequestException(`invalid_credential_request: format '${format}' not supported`);
    }

    // Where this credential type's claims come from — the Sunbird registry, or an
    // endpoint the issuing authority hosts against its own database. Resolved per
    // type, so authorities with different systems of record coexist here.
    const properties = Object.keys(cfg.schema?.properties ?? {});
    const { claims, missing } = await this.resolveClaimsFor(cfg, subjectId, properties);
    if (missing.length) {
      throw new BadRequestException(
        `credential_request_denied: your record is missing required ${missing.join(', ')}. ` +
          `Contact the issuing authority.`,
      );
    }

    const configId = cfg.formats.length > 1 ? `${cfg.schemaId}_${format}` : cfg.schemaId;
    this.logger.log(
      `Self-service issuance: ${cfg.name} for ${this.config.keycloak.subjectClaim}=${subjectId}`,
    );

    return {
      credentialConfigurationId: configId,
      format,
      schemaId: cfg.schemaId,
      schemaVersion: cfg.version,
      schemaName: cfg.name,
      vct: format === 'vc+sd-jwt' ? normalizeVct(cfg.vct, this.config.publicUrl) : undefined,
      issuerDid: cfg.author || this.tokens.getIssuerDid(),
      claims,
      // No pre-auth code and no PIN: the Keycloak login already established who
      // this is, which is precisely what a tx_code exists to substitute for.
      preAuthCode: '',
      txCodeRequired: false,
      tags: cfg.tags || [cfg.name],
      renderMethod: this.resolveRenderMethod(cfg, format),
      docType: cfg.mdoc?.docType,
      namespace: cfg.mdoc?.namespace,
      elementMapping: cfg.mdoc?.elementMapping,
    };
  }

  /**
   * Resolves claims through whichever source backs this credential type, turning
   * each provider failure into the message that actually helps.
   *
   * The three cases are genuinely different and must not collapse into one
   * "issuance failed": nothing configured is a deployment decision, the source
   * being down is the authority's system, and no record for this subject is a
   * provisioning gap for one holder. Reporting the wrong one sends whoever is
   * debugging to the wrong system entirely.
   */
  private async resolveClaimsFor(
    cfg: { schemaId: string; name: string; schema?: { required?: string[] } },
    subjectId: string,
    properties: string[],
  ) {
    let provider: ClaimSourceProvider;
    try {
      provider = this.claimSources.for({ schemaId: cfg.schemaId, name: cfg.name });
    } catch (err) {
      if (err instanceof ClaimSourceNotConfiguredError) {
        throw new BadRequestException(`credential_request_denied: ${err.message}`);
      }
      if (err instanceof ClaimSourceUnavailableError) {
        throw new BadRequestException(`server_error: ${err.message}`);
      }
      throw err;
    }

    try {
      return await provider.resolve({
        subjectId,
        subjectClaim: this.config.keycloak.subjectClaim,
        credentialConfigurationId: cfg.schemaId,
        credentialName: cfg.name,
        properties,
        required: cfg.schema?.required ?? [],
      });
    } catch (err) {
      if (err instanceof SubjectNotFoundError) {
        throw new BadRequestException(
          `credential_request_denied: no record for ` +
            `${this.config.keycloak.subjectClaim} '${subjectId}'. Contact the issuing authority.`,
        );
      }
      if (err instanceof ClaimSourceUnavailableError) {
        // Named, because "try again" is the right advice here and is not the right
        // advice for any of the other refusals on this path.
        this.logger.error(err.message);
        throw new BadRequestException(
          `server_error: the issuing authority's records are temporarily unreachable ` +
            `(${err.source}). Try again shortly.`,
        );
      }
      throw err;
    }
  }

  async deferred(authHeader: string | undefined, body: Record<string, any>) {
    await this.tokens.validateAccessToken(authHeader);
    const txId = body?.transaction_id;
    if (!txId) throw new BadRequestException('Missing transaction_id');
    const txn = await this.store.get<any>(`oid4vc:deferred:${txId}`);
    if (!txn) throw new NotFoundException('Unknown transaction_id');
    const session = await this.store.get<OfferSession>(`oid4vc:offer:${txn.offerId}`);
    if (!session) throw new BadRequestException('Offer session expired');

    if (session.deferredClaimId && !(await this.isClaimReady(session.deferredClaimId))) {
      // Still pending — spec: 202 with interval hint (handled in controller).
      return { pending: true, interval: 60 };
    }
    const credential = await this.issueForSession(session, txn.holderDid, txn.holderJwk, txn.holderKid);
    await this.store.del(`oid4vc:deferred:${txId}`);
    return { credential, format: session.format };
  }

  async notification(authHeader: string | undefined, body: Record<string, any>) {
    await this.tokens.validateAccessToken(authHeader);
    // MVP: log accept/deny telemetry only.
    this.logger.log(`OID4VCI notification: ${JSON.stringify(body)}`);
    return;
  }

  // --- Internal ------------------------------------------------------------

  private async issueForSession(
    session: OfferSession,
    holderDid?: string,
    holderJwk?: any,
    holderKid?: string,
  ) {
    // credentialConfigurationId is now schemaId-based (see createOffer /
    // issuerMetadata) rather than name-based, so the readable VC type name
    // can no longer be derived by splitting it — it's carried separately.
    const typeName = session.schemaName;
    const credential = {
      // The base VC context alone only defines id/type/issuer/credentialSubject
      // etc. — it has no term for schema-specific claims like `name` or
      // `birthdate`, and no term for a type name containing spaces (e.g.
      // "OID4VC Pilot Credential"). For ldp_vc, identity-service's Ed25519
      // linked-data-proof signer canonicalizes in JSON-LD "safe mode" (found
      // live: undefined property terms get silently dropped and throw
      // `jsonld.ValidationError: Safe mode validation error`; a type name
      // with a space expands to a malformed IRI and throws "relative @type
      // reference" — neither ever surfaced before since every prior test in
      // this session used jwt_vc_json, which never runs JSON-LD expansion at
      // all). A `@vocab` fallback plus an explicit type-name term fixes both
      // — but found live AGAIN: inlining that as a JSON object in @context
      // crashes wallets whose parser assumes every @context entry is a plain
      // URL string. So the mapping is served as a real document (AppController's
      // `/contexts/:typeName`) and referenced by URL here.
      //
      // internalUrl, not publicUrl: this URL is dereferenced by
      // identity-service's own JSON-LD signer, from inside identity-service's
      // container — not by the wallet. The shipped compose stack's PUBLIC_URL
      // default (http://localhost:3400) resolves to identity-service's own
      // loopback there, not back to this service, which previously made every
      // ldp_vc issuance fail with an ECONNREFUSED-driven signing error.
      '@context': [
        'https://www.w3.org/2018/credentials/v1',
        `${this.config.internalUrl}/contexts/${encodeURIComponent(typeName)}`,
      ],
      type: ['VerifiableCredential', typeName],
      issuer: session.issuerDid,
      issuanceDate: new Date().toISOString(),
      credentialSubject: {
        ...(holderDid ? { id: holderDid } : {}),
        ...session.claims,
      },
      // W3C VC Render Method (https://www.w3.org/TR/vc-render-method/) —
      // resolved once at offer-creation time (resolveRenderMethod()); the
      // per-type @vocab context above already covers its terms too, since
      // it maps any undefined property to a generic IRI rather than just
      // the schema's own claim names.
      ...(session.renderMethod ? { renderMethod: [session.renderMethod] } : {}),
    };
    const res = await this.credentials.issue({
      credential,
      credentialSchemaId: session.schemaId,
      credentialSchemaVersion: session.schemaVersion,
      tags: session.tags,
      format: session.format,
      holderJwk,
      holderKid,
      ...(session.format === 'vc+sd-jwt' ? { vct: session.vct } : {}),
      // mso_mdoc-only: the generic W3C `credential` object above is still
      // built (and still schema-validated against its flat
      // credentialSubject) for consistency with every other format, but the
      // actual signed wire content for mdoc comes from these fields instead
      // — see credential-format.service.ts signMdoc().
      ...(session.format === 'mso_mdoc'
        ? { docType: session.docType, namespaces: this.buildMdocNamespaces(session) }
        : {}),
    });
    return res.credential;
  }

  // Maps flat OID4VCI claims into mdoc's {namespace: {elementIdentifier:
  // value}} shape, using elementMapping to override the default namespace
  // for specific claim names when they don't already match an ISO-registered
  // element identifier in that namespace.
  private buildMdocNamespaces(session: OfferSession): Record<string, Record<string, any>> {
    const namespaces: Record<string, Record<string, any>> = {};
    for (const [claim, value] of Object.entries(session.claims)) {
      const mapping = session.elementMapping?.[claim];
      const ns = mapping?.namespace || session.namespace;
      const elementId = mapping?.elementIdentifier || claim;
      namespaces[ns] = { ...(namespaces[ns] || {}), [elementId]: value };
    }
    return namespaces;
  }

  // Hook point for deferred issuance backed by the attestation/claim workflow.
  // Default (no claim workflow wired): treat as ready.
  private async isClaimReady(_claimId: string): Promise<boolean> {
    return true;
  }

  // W3C VC Render Method (https://www.w3.org/TR/vc-render-method/): only
  // applies to formats that carry a full W3C-shaped VC object to attach it
  // to (ldp_vc, jwt_vc_json) — vc+sd-jwt has its own, separate IETF
  // mechanism (SD-JWT VC Type Metadata) and mso_mdoc has no analog. Resolved
  // once at offer-creation time (not per-issuance) since the URL/digest are
  // fixed for a given schema.
  private resolveRenderMethod(
    cfg: Oid4vciSchemaConfig,
    format: string,
  ): Record<string, any> | undefined {
    const rm = cfg.renderMethod;
    if (!rm || (format !== 'ldp_vc' && format !== 'jwt_vc_json')) return undefined;
    const base = {
      type: rm.type || 'SvgRenderingTemplate',
      ...(rm.name ? { name: rm.name } : {}),
      ...(rm.cssMediaQuery ? { css3MediaQuery: rm.cssMediaQuery } : {}),
    };
    if (rm.svg) {
      return {
        id: `${this.config.publicUrl}/render-templates/${encodeURIComponent(cfg.schemaId)}`,
        ...base,
        digestMultibase: digestMultibase(rm.svg),
      };
    }
    if (rm.url) {
      return { id: rm.url, ...base };
    }
    return undefined;
  }

  /**
   * The Keycloak realm among the advertised authorization servers.
   *
   * Picked by matching the realm issuer rather than by position: the ordering in
   * authorizationServers() is a display choice, and silently naming this service
   * for an authorization_code grant would send the wallet to an /authorize
   * endpoint that does not exist.
   */
  private keycloakAuthorizationServer(servers: string[]): string {
    const realm = servers.find((s) => s !== this.config.publicUrl);
    if (!realm) {
      throw new BadRequestException(
        'server_error: no external authorization server is advertised for authorization_code',
      );
    }
    return realm;
  }

  private randomToken(): string {
    return crypto.randomBytes(24).toString('base64url');
  }

  /**
   * Six-digit transaction code, matching the `tx_code` shape published in the
   * offer (`input_mode: 'numeric', length: 6`).
   *
   * `randomInt` rather than `Math.random()`: this is a shared secret protecting
   * a credential, and it is short enough that a predictable generator would make
   * guessing realistic.
   */
  private randomPin(): string {
    return String(crypto.randomInt(0, 1_000_000)).padStart(6, '0');
  }

  /**
   * Length-independent constant-time compare. `crypto.timingSafeEqual` throws on
   * a length mismatch, and returning early on that would leak the PIN's length —
   * so hash both sides to a fixed width first and compare those.
   */
  private constantTimeEquals(a: string, b: string): boolean {
    const ha = crypto.createHash('sha256').update(a, 'utf8').digest();
    const hb = crypto.createHash('sha256').update(b, 'utf8').digest();
    return crypto.timingSafeEqual(ha, hb);
  }

  private decodeJwtClaims(jwt: string): any {
    try {
      const [, payload] = jwt.split('.');
      return JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
    } catch {
      return {};
    }
  }
}
