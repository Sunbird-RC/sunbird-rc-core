import { Injectable, Logger } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import * as jose from 'jose';
import { loadConfig } from '../config/configuration';

// The tail Keycloak appends to a realm URL to reach its key set. Stripping it
// off JWKS_URI is how the accepted `iss` is derived when AUTH_ISSUER is unset,
// so an operator only has to configure one URL for the common case.
const CERTS_SUFFIX = '/protocol/openid-connect/certs';

/** How long a /health probe result is reused before Keycloak is re-checked. */
const HEALTH_CACHE_MS = 30_000;

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

export interface KeycloakHealthInfo {
  enabled: boolean;
  status: 'UP' | 'DOWN';
  reason?: string;
}

/**
 * Keycloak realm trust for this service: resolves the realm's key set, verifies
 * bearer tokens against it, and reports whether Keycloak is actually there.
 *
 * Only active when ENABLE_AUTH=true. Every misconfiguration it can detect is
 * raised at construction or at boot rather than on the first request — an
 * issuance endpoint that silently accepts nothing is worse than one that
 * refuses to start.
 */
@Injectable()
export class KeycloakService {
  private readonly logger = new Logger(KeycloakService.name);
  private readonly config = loadConfig();

  readonly enabled: boolean;
  readonly jwksUri: string;
  /** Every `iss` value accepted on an incoming token. */
  readonly issuers: string[];

  private jwks: ReturnType<typeof jose.createRemoteJWKSet>;
  private reachable = false;
  private lastError: string;
  private lastProbeAt = 0;

  constructor(private readonly http: HttpService) {
    const { enabled, jwksUri, issuers } = this.config.auth;
    this.enabled = enabled;
    this.jwksUri = jwksUri;

    if (!this.enabled) {
      this.issuers = [];
      return;
    }

    if (!this.jwksUri) {
      throw new Error(
        'ENABLE_AUTH=true requires JWKS_URI, e.g. ' +
          'http://keycloak:8080/auth/realms/sunbird-rc/protocol/openid-connect/certs',
      );
    }
    this.issuers = issuers.length ? issuers : deriveIssuers(this.jwksUri);
    if (!this.issuers.length) {
      throw new Error(
        `Cannot derive the token issuer from JWKS_URI '${this.jwksUri}' ` +
          `(it does not end in '${CERTS_SUFFIX}'). Set AUTH_ISSUER to the realm ` +
          'URL, e.g. http://keycloak:8080/auth/realms/sunbird-rc',
      );
    }
    this.logger.log(
      `Bearer auth ENABLED on POST /oid4vc/offer — accepting tokens from ${this.issuers.join(', ')}`,
    );
  }

  /**
   * Verifies a Keycloak-issued access token. Throws on anything but a token
   * this realm signed and that is currently valid.
   *
   * jwtVerify checks the signature, `exp` and `nbf`; `issuer` is checked here
   * so a correctly-signed token from another realm on the same Keycloak is
   * still rejected. `aud` is deliberately NOT checked: Keycloak's default
   * access token carries `aud: account`, so an audience rule would reject every
   * ordinary realm token until each client grows a custom mapper.
   */
  async verify(token: string): Promise<jose.JWTPayload> {
    if (!this.jwks) {
      // Built once and reused: jose caches keys by `kid` behind this handle and
      // only re-fetches on an unknown one, which is what makes key rotation
      // work without restarting. Rebuilding per request would refetch the JWKS
      // on every offer.
      this.jwks = jose.createRemoteJWKSet(new URL(this.jwksUri));
    }
    const { payload } = await jose.jwtVerify(token, this.jwks, {
      issuer: this.issuers,
    });
    return payload;
  }

  /**
   * Confirms Keycloak is actually running and serving keys for this realm.
   *
   * Probes JWKS_URI itself, NOT the realm's discovery document. AUTH_ISSUER
   * holds token `iss` VALUES, which are not necessarily URLs this process can
   * reach — in the very split it exists for, the internal-vs-public one, an
   * `iss` of http://keycloak:8080/... does not resolve outside the compose
   * network. JWKS_URI is by definition reachable, because it is the URL jose
   * fetches keys from at verify time, so probing it tests the thing that has
   * to work.
   *
   * Retries because compose brings this service up alongside Keycloak, and the
   * WildFly-based image takes roughly a minute to accept requests — a single
   * attempt would fail on nearly every cold start.
   */
  async probe(attempts = 12, delayMs = 5000): Promise<boolean> {
    if (!this.enabled) return true;

    for (let attempt = 1; attempt <= attempts; attempt++) {
      try {
        const res = await this.http.axiosRef.get(this.jwksUri, { timeout: 5000 });
        // A realm that does not exist answers 404, so reaching this with keys
        // present means Keycloak is up AND the realm is right.
        if (!res.data?.keys?.length) {
          throw new Error('no keys in the JWK set');
        }
        this.markReachable();
        this.logger.log(
          `Keycloak reachable — ${res.data.keys.length} key(s) at ${this.jwksUri}`,
        );
        return true;
      } catch (err) {
        this.markUnreachable(errorMessage(err));
        this.logger.warn(
          `Keycloak not reachable at ${this.jwksUri} (attempt ${attempt}/${attempts}): ${this.lastError}`,
        );
        if (attempt < attempts) await sleep(delayMs);
      }
    }
    return false;
  }

  /**
   * Boot gate. Refusing to start is deliberate: with ENABLE_AUTH=true and no
   * reachable Keycloak this service cannot authorise anything, so serving is
   * strictly worse than restarting until Keycloak appears.
   */
  async probeOrExit(): Promise<void> {
    if (!this.enabled) {
      this.logger.log('ENABLE_AUTH is not true — POST /oid4vc/offer is UNAUTHENTICATED');
      return;
    }
    this.logger.log('ENABLE_AUTH=true — checking Keycloak is running');
    if (await this.probe()) return;

    this.logger.error(
      `Keycloak did not serve a key set at ${this.jwksUri}. ` +
        'Refusing to start with ENABLE_AUTH=true and no authorization server. ' +
        'Check that Keycloak is running and that JWKS_URI is correct and ' +
        'reachable from this container.',
    );
    process.exit(1);
  }

  /**
   * Current Keycloak status for GET /health, re-probed at most every 30s.
   *
   * When the flag is off this reports UP with a reason rather than omitting
   * itself, mirroring the Java registry's
   * `ComponentHealthInfo(name, true, "AUTHENTICATION_ENABLED", "false")`.
   */
  async healthInfo(): Promise<KeycloakHealthInfo> {
    if (!this.enabled) {
      return { enabled: false, status: 'UP', reason: 'ENABLE_AUTH=false' };
    }
    if (Date.now() - this.lastProbeAt > HEALTH_CACHE_MS) {
      // One attempt, no retry: /health answers now, it does not wait a minute.
      await this.probe(1);
    }
    return this.reachable
      ? { enabled: true, status: 'UP' }
      : { enabled: true, status: 'DOWN', reason: this.lastError };
  }

  private markReachable() {
    this.reachable = true;
    this.lastError = undefined;
    this.lastProbeAt = Date.now();
  }

  private markUnreachable(reason: string) {
    this.reachable = false;
    this.lastError = reason;
    this.lastProbeAt = Date.now();
  }
}

/** `<realm>/protocol/openid-connect/certs` -> `<realm>`. */
function deriveIssuers(jwksUri: string): string[] {
  const trimmed = jwksUri.replace(/\/+$/, '');
  return trimmed.endsWith(CERTS_SUFFIX)
    ? [trimmed.slice(0, -CERTS_SUFFIX.length)]
    : [];
}

function errorMessage(err: any): string {
  const status = err?.response?.status;
  return String(err?.code || (status ? `HTTP ${status}` : '') || err?.message || err);
}
