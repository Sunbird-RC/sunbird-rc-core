import { Injectable, Logger, OnModuleInit, UnauthorizedException } from '@nestjs/common';
import { IdentityClient } from '../clients/identity.client';
import { loadConfig } from '../config/configuration';
import * as jose from 'jose';

// Mints and validates the façade's OAuth access tokens for the
// pre-authorized_code grant. The signing key is NOT held here — signing is
// delegated to identity-service (keys stay in Vault). Validation uses the
// issuer DID's published JWK (via identity-service JWKS / DID resolution).
@Injectable()
export class TokenService implements OnModuleInit {
  private readonly logger = new Logger(TokenService.name);
  private readonly config = loadConfig();
  private issuerDid: string;

  constructor(private readonly identity: IdentityClient) {}

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

  // Validates a bearer access token minted by this service.
  async validateAccessToken(authHeader?: string): Promise<any> {
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new UnauthorizedException('Missing bearer token');
    }
    const token = authHeader.substring(7);
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
      return payload;
    } catch (err) {
      this.logger.warn(`Access token validation failed: ${err}`);
      throw new UnauthorizedException('Invalid access token');
    }
  }

  // OAuth 2.0 AS metadata so the Java registry's JwtIssuerAuthenticationManager
  // Resolver can trust our tokens config-only.
  asMetadata() {
    return {
      issuer: this.config.publicUrl,
      token_endpoint: `${this.config.publicUrl}/oid4vc/token`,
      jwks_uri: `${this.config.publicUrl}/.well-known/jwks.json`,
      grant_types_supported: ['urn:ietf:params:oauth:grant-type:pre-authorized_code'],
      response_types_supported: ['token'],
      token_endpoint_auth_methods_supported: ['none'],
    };
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
