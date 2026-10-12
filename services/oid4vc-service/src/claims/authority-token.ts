import { Logger } from '@nestjs/common';

/**
 * OAuth2 client-credentials tokens for the Authority Service, re-acquired as they expire.
 *
 * WHY THIS EXISTS: the first version of the Authority claim source read a bearer token
 * from `AUTHORITY_TOKEN` and sent it forever. A token in an environment variable cannot
 * outlive its own expiry, so a deployment configured on Monday stops issuing on Monday
 * plus the token lifetime — and it fails as a 401 from the Authority, which reads like a
 * permissions problem rather than an expiry. Nothing restarts, nothing alarms, and
 * issuance is simply gone.
 *
 * So the issuing service holds CREDENTIALS rather than a token, and exchanges them
 * whenever the token it has is close to expiring.
 *
 * The static `AUTHORITY_TOKEN` is still honoured when no client credentials are
 * configured, because a deployment fronting the Authority with something other than
 * OAuth2 should not be forced through this path.
 */
export interface ClientCredentials {
  tokenUrl: string;
  clientId: string;
  clientSecret: string;
  /** Optional `scope`, for an Authority that scopes its administrative API. */
  scope?: string;
  /** Bounded: a wallet is waiting further down the call chain. */
  timeoutMs?: number;
}

export class AuthorityTokenProvider {
  private readonly logger = new Logger(AuthorityTokenProvider.name);

  /** The current token and the moment it stops being offered. */
  private token = '';
  private expiresAt = 0;

  /**
   * The exchange in flight, if any.
   *
   * Several credential requests can arrive together — a wallet collecting two credentials
   * does exactly that. Without this they would each see an expired token and each start
   * their own exchange, so one expiry would produce a burst of identical requests against
   * the IdP. They share one.
   */
  private inFlight: Promise<string> | null = null;

  constructor(private readonly credentials: ClientCredentials) {}

  /** A token that will still be valid when the caller uses it. */
  async get(): Promise<string> {
    if (this.token && Date.now() < this.expiresAt) {
      return this.token;
    }
    if (!this.inFlight) {
      this.inFlight = this.exchange().finally(() => {
        this.inFlight = null;
      });
    }
    return this.inFlight;
  }

  /**
   * Discards the current token.
   *
   * Called when the Authority rejects a request as unauthorised: the token may have been
   * revoked, or the clocks may disagree by more than the safety margin. Re-acquiring once
   * turns a transient mismatch into a retry rather than a failed issuance.
   */
  invalidate(): void {
    this.token = '';
    this.expiresAt = 0;
  }

  private async exchange(): Promise<string> {
    const { tokenUrl, clientId, clientSecret, scope, timeoutMs } = this.credentials;

    const body = new URLSearchParams({
      grant_type: 'client_credentials',
      client_id: clientId,
      client_secret: clientSecret,
    });
    if (scope) body.set('scope', scope);

    let response: Response;
    try {
      response = await fetch(tokenUrl, {
        method: 'POST',
        headers: { 'content-type': 'application/x-www-form-urlencoded' },
        body,
        signal: AbortSignal.timeout(timeoutMs ?? 8000),
      });
    } catch (err: any) {
      throw new Error(`could not reach the token endpoint: ${err?.message ?? err}`);
    }

    if (!response.ok) {
      // The body of a failed token response carries `error` and `error_description`, and
      // neither contains the secret. Worth surfacing: "invalid_client" and "unauthorized
      // client" send an operator to quite different places.
      const detail = await response.text().catch(() => '');
      throw new Error(
        `token endpoint answered ${response.status}${detail ? `: ${detail.slice(0, 200)}` : ''}`,
      );
    }

    let payload: any;
    try {
      payload = await response.json();
    } catch {
      throw new Error('the token endpoint returned unreadable JSON');
    }

    const token = typeof payload?.access_token === 'string' ? payload.access_token : '';
    if (!token) throw new Error('the token response carried no access_token');

    // Renew EARLY. A token used at the instant it expires is refused by the time it
    // arrives, and clocks between two containers are not identical. Ten seconds of margin,
    // or a third of the lifetime for tokens shorter than that, so a 5-second token still
    // leaves a usable window instead of being treated as already dead.
    const lifetime = Number(payload?.expires_in) > 0 ? Number(payload.expires_in) : 60;
    const margin = Math.min(10, Math.floor(lifetime / 3));
    this.token = token;
    this.expiresAt = Date.now() + (lifetime - margin) * 1000;

    this.logger.log(
      `acquired an Authority token for ${clientId}, valid ${lifetime}s, renewing in ${
        lifetime - margin
      }s`,
    );
    return token;
  }
}

/**
 * Providers, shared by (token endpoint, client).
 *
 * The claim source is constructed PER RESOLUTION by ClaimSourceFactory, so a provider held
 * as an instance field would be a fresh, empty cache on every credential request — every
 * call would mint a new token and the cache would never once be read. That is not a
 * performance detail: it turns one issuance into several token exchanges and makes the IdP
 * a hard dependency of every step rather than of the first.
 *
 * Keyed rather than a single global so a deployment issuing as two different officers
 * keeps two tokens, which is the whole point of separate clients.
 */
const providers = new Map<string, AuthorityTokenProvider>();

/** Testing seam: the module cache outlives a test's environment changes otherwise. */
export function resetAuthorityTokenProviders(): void {
  providers.clear();
}

/**
 * Builds a provider from configuration, or returns null when this deployment does not use
 * client credentials. Null means "fall back to whatever static header was configured".
 */
export function authorityTokenProvider(config: {
  tokenUrl?: string;
  clientId?: string;
  clientSecret?: string;
  scope?: string;
  timeoutMs?: number;
}): AuthorityTokenProvider | null {
  const { tokenUrl, clientId, clientSecret } = config;
  if (!tokenUrl && !clientId && !clientSecret) return null;

  // A HALF-configured client is a deployment mistake, and failing here is much kinder than
  // falling back to an anonymous call that the Authority refuses on every issuance.
  if (!tokenUrl || !clientId || !clientSecret) {
    const missing = [
      !tokenUrl && 'AUTHORITY_TOKEN_URL',
      !clientId && 'AUTHORITY_CLIENT_ID',
      !clientSecret && 'AUTHORITY_CLIENT_SECRET',
    ].filter(Boolean);
    throw new Error(
      `Authority client credentials are partly configured; missing ${missing.join(', ')}. ` +
        'Set all three, or none of them to use AUTHORITY_TOKEN / AUTHORITY_HEADERS instead.',
    );
  }

  const key = `${tokenUrl}|${clientId}`;
  let provider = providers.get(key);
  if (!provider) {
    provider = new AuthorityTokenProvider({
      tokenUrl,
      clientId,
      clientSecret,
      scope: config.scope,
      timeoutMs: config.timeoutMs,
    });
    providers.set(key, provider);
  }
  return provider;
}
