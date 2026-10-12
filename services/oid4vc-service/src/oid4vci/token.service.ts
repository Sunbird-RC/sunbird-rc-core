import { Injectable, Logger, OnModuleInit, UnauthorizedException } from '@nestjs/common';
import { IdentityClient } from '../clients/identity.client';
import { loadConfig } from '../config/configuration';
import * as jose from 'jose';

/** A validated access token, plus where it came from. */
export interface ValidatedToken {
  payload: any;
  /**
   * `preauth` — minted by this service for a pre-authorized_code offer; `sub` is
   * the offer id and the claims were fixed when the offer was created.
   * `keycloak` — issued by the external authorization server for a signed-in
   * holder; `sub` is a Keycloak user and NOTHING about the credential's contents
   * has been decided yet, so claims must be sourced from the registry.
   */
  source: 'preauth' | 'keycloak';
  /** Keycloak only: the subject's registry key, from the configured claim. */
  subjectId?: string;
  /** Keycloak only: realm roles, for the staff gate on offer creation. */
  roles?: string[];
}

// Mints and validates the façade's OAuth access tokens for the
// pre-authorized_code grant. The signing key is NOT held here — signing is
// delegated to identity-service (keys stay in Vault). Validation uses the
// issuer DID's published JWK (via identity-service JWKS / DID resolution).
//
// It ALSO validates Keycloak-issued tokens, which is what lets a holder's wallet
// authenticate directly against the realm: Keycloak is the authorization server,
// this service is a resource server. Doing it that way means no /authorize
// endpoint, no PKCE implementation and no consent UI of our own.
@Injectable()
export class TokenService implements OnModuleInit {
  private readonly logger = new Logger(TokenService.name);
  private readonly config = loadConfig();
  private issuerDid: string;
  private keycloakJwks?: ReturnType<typeof jose.createRemoteJWKSet>;

  constructor(private readonly identity: IdentityClient) {}

  private get keycloakIssuer(): string {
    return `${this.config.keycloak.publicUrl}/realms/${this.config.keycloak.realm}`;
  }

  /**
   * Cached remote JWK set for the realm. `createRemoteJWKSet` handles its own
   * caching and cooldown, so a token signed with an unknown `kid` triggers at
   * most one refetch rather than one per request — which would otherwise be a
   * trivial way to have us hammer Keycloak.
   */
  private get keycloakKeys() {
    if (!this.keycloakJwks) {
      const base = this.config.keycloak.internalUrl || this.config.keycloak.publicUrl;
      const url = `${base}/realms/${this.config.keycloak.realm}/protocol/openid-connect/certs`;
      this.keycloakJwks = jose.createRemoteJWKSet(new URL(url));
      this.logger.log(`Keycloak JWKS: ${url}`);
    }
    return this.keycloakJwks;
  }

  async onModuleInit() {
    this.issuerDid = this.config.issuerDid;
    if (!this.issuerDid) {
      // Dev convenience: auto-provision an issuer DID on first boot.
      try {
        // did:rcw is DB-resolvable without WEB_DID_BASE_URL config; production
        // deployments should set ISSUER_DID to a did:web the wallets can resolve.
        const did = await this.identity.generateDID('rcw');
        this.issuerDid = did?.id;
        this.logger.warn(
          `No ISSUER_DID configured; generated ephemeral issuer DID ${this.issuerDid}. Set ISSUER_DID in production.`,
        );
      } catch (err) {
        this.logger.error(`Could not auto-generate issuer DID: ${err}`);
      }
    }
  }

  getIssuerDid(): string {
    return this.issuerDid;
  }

  // Mints a short-lived access token bound to an offer session.
  async mintAccessToken(claims: {
    sub: string;
    credential_configuration_id?: string;
    scope?: string;
  }): Promise<string> {
    const now = Math.floor(Date.now() / 1000);
    const payload = {
      iss: this.config.publicUrl,
      aud: this.config.publicUrl,
      iat: now,
      exp: now + this.config.ttl.accessToken,
      ...claims,
    };
    return this.identity.signJwt(this.issuerDid, payload, { typ: 'at+jwt' });
  }

  /**
   * Validates a bearer access token from either authorization server.
   *
   * Which one is decided by the token's own `iss`, read WITHOUT verifying — that
   * is safe because it only routes to a verifier; the chosen verifier then
   * checks the signature and re-checks `iss` against what it expects, so a forged
   * `iss` merely picks a verifier that rejects it.
   */
  async validateAccessToken(authHeader?: string): Promise<ValidatedToken> {
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new UnauthorizedException('Missing bearer token');
    }
    const token = authHeader.substring(7);

    let unverifiedIss: string | undefined;
    try {
      unverifiedIss = jose.decodeJwt(token).iss;
    } catch {
      throw new UnauthorizedException('Malformed access token');
    }

    if (this.config.keycloak.enabled && unverifiedIss === this.keycloakIssuer) {
      return this.validateKeycloakToken(token);
    }
    return this.validatePreauthToken(token);
  }

  /** A token this service minted for a pre-authorized_code offer. */
  private async validatePreauthToken(token: string): Promise<ValidatedToken> {
    try {
      // Signature check via the issuer DID (delegated).
      const res = await this.identity.verifyJwt(token, this.issuerDid);
      if (!res.verified) throw new Error(res.error || 'invalid signature');
      const payload = res.payload;
      if (payload.exp && payload.exp * 1000 < Date.now()) {
        throw new Error('token expired');
      }
      if (payload.iss !== this.config.publicUrl) {
        throw new Error('bad issuer');
      }
      return { payload, source: 'preauth' };
    } catch (err) {
      this.logger.warn(`Access token validation failed: ${err}`);
      throw new UnauthorizedException('Invalid access token');
    }
  }

  /**
   * A token issued by the Keycloak realm to a signed-in holder (or to portal
   * staff). Verified against the realm's published keys; `exp`/`nbf` are checked
   * by jwtVerify itself.
   */
  private async validateKeycloakToken(token: string): Promise<ValidatedToken> {
    try {
      const { payload } = await jose.jwtVerify(token, this.keycloakKeys, {
        issuer: this.keycloakIssuer,
        // Keycloak's default access token carries aud: 'account', so an audience
        // check is opt-in via KEYCLOAK_AUDIENCE rather than assumed. Left unset,
        // any token from this realm is accepted — acceptable because the realm is
        // ours and the roles below are what actually authorise.
        ...(this.config.keycloak.audience ? { audience: this.config.keycloak.audience } : {}),
      });

      const subjectId = this.findSubjectClaim(payload);
      const roles = (payload as any).realm_access?.roles ?? [];
      return {
        payload,
        source: 'keycloak',
        subjectId: subjectId === undefined ? undefined : String(subjectId),
        roles,
      };
    } catch (err: any) {
      this.logger.warn(`Keycloak token validation failed: ${err?.message ?? err}`);
      throw new UnauthorizedException('Invalid access token');
    }
  }


  /**
   * The holder's registry key from the token, tolerant of naming style.
   *
   * The configured claim name is tried first. Failing that, claims are matched
   * with separators stripped and case ignored, because the same concept is
   * written `farmerId` in some realms and `farmer_id` in others — the deployed
   * realm uses snake_case while this service defaults to camelCase, and an exact
   * lookup silently yields "your account is not linked to a record" for a user
   * who is perfectly well linked.
   */
  private findSubjectClaim(payload: Record<string, any>): unknown {
    // Unconfigured is not "look harder": with an empty name the normalised
    // comparison below reduces to '' and would match any claim whose key is all
    // punctuation. Callers that actually need a subject report the missing
    // variable themselves — see buildSelfServiceSession.
    if (!this.config.keycloak.subjectClaim) return undefined;

    const configured = payload[this.config.keycloak.subjectClaim];
    if (configured !== undefined && configured !== null && configured !== '') return configured;

    const norm = (v: string) => v.toLowerCase().replace(/[^a-z0-9]/g, '');
    const want = norm(this.config.keycloak.subjectClaim);
    for (const [k, v] of Object.entries(payload)) {
      if (norm(k) === want && v !== undefined && v !== null && v !== '') return v;
    }
    return undefined;
  }

  // OAuth 2.0 AS metadata so the Java registry's JwtIssuerAuthenticationManager
  // Resolver can trust our tokens config-only.
  asMetadata() {
    return {
      issuer: this.config.publicUrl,
      token_endpoint: `${this.config.publicUrl}/oid4vc/token`,
      jwks_uri: `${this.config.publicUrl}/.well-known/jwks.json`,
      // This document describes THIS service's own AS role, which only ever
      // supports the pre-authorized_code grant. The authorization_code grant is
      // Keycloak's, and issuer metadata points wallets at the realm's own
      // metadata for it (see issuerMetadata's authorization_servers) rather than
      // advertising a grant this token endpoint does not implement.
      grant_types_supported: ['urn:ietf:params:oauth:grant-type:pre-authorized_code'],
      response_types_supported: ['token'],
      token_endpoint_auth_methods_supported: ['none'],
    };
  }

  /**
   * Authorization servers to advertise in issuer metadata.
   *
   * With Keycloak configured, the realm is listed FIRST: a wallet offered an
   * `authorization_code` grant picks the first entry it can use, and the realm is
   * the only one of the two that implements that grant.
   */
  authorizationServers(): string[] {
    return this.config.keycloak.enabled
      ? [this.keycloakIssuer, this.config.publicUrl]
      : [this.config.publicUrl];
  }

  // Proxy the issuer's public JWKS (keys live in identity-service).
  async jwks() {
    try {
      return await this.identity.getJwks();
    } catch (err) {
      this.logger.error(`Error fetching JWKS: ${err}`);
      return { keys: [] };
    }
  }
}
