import { Logger } from '@nestjs/common';
import { loadConfig, type ClaimSourceSpec } from '../config/configuration';
import { resolveRegistryClaims } from '../oid4vci/registry-claims.util';
import {
  ClaimSourceUnavailableError,
  SubjectNotFoundError,
  type ClaimRequest,
  type ClaimSourceProvider,
} from './claim-source.interface';

/**
 * Claims from an endpoint the issuing authority hosts against its own database.
 *
 * This is what makes the Sunbird registry optional. A Department of Agriculture
 * that keeps farmers and land parcels in its own system implements one endpoint;
 * nothing about its schema, storage or queries reaches this service.
 *
 * The contract:
 *
 *   POST <url>
 *     { subjectId, subjectClaim, credentialConfigurationId, credentialName,
 *       attributes: [...] }
 *
 *   200 { "claims": { "name": "...", "landAreaAcres": 4.5 } }   (or a bare object)
 *   404                                                          no such holder
 *
 * The response is run through the SAME resolver the registry source uses, which
 * buys three things: an issuer may return their raw record and still get
 * name-matching, the configured aliases and `age_over_NN` derivation; required
 * attributes that are absent are reported by name instead of failing opaquely
 * downstream; and — the security-relevant one — the resolver only ever reads the
 * attributes the credential type DECLARES, so extra fields in the response are
 * dropped rather than copied into the credential. A compromised or simply buggy
 * endpoint cannot add claims the schema does not define.
 */
export class HttpClaimSource implements ClaimSourceProvider {
  private readonly logger = new Logger(HttpClaimSource.name);
  private readonly config = loadConfig();

  constructor(private readonly spec: ClaimSourceSpec) {}

  get name(): string {
    return this.spec.name;
  }

  async resolve(req: ClaimRequest) {
    const url = this.assertSafeUrl(this.spec.url ?? '');
    const body = await this.fetchClaims(url, req);

    // `{ claims: {...} }` if present, otherwise the whole body as the record.
    // Accepting both means the simplest possible issuer implementation — return
    // your row — works, without forcing a wrapper on anyone.
    const record =
      body && typeof body === 'object' && body.claims && typeof body.claims === 'object'
        ? body.claims
        : body;

    if (!record || typeof record !== 'object' || Array.isArray(record)) {
      throw new ClaimSourceUnavailableError(
        this.name,
        'response was not a JSON object of claims',
      );
    }

    return resolveRegistryClaims({
      properties: req.properties,
      required: req.required,
      // One source, named after the provider so provenance in the portal and in
      // the "missing" message points at the authority that owns the data.
      sources: [{ entity: this.name, record: record as Record<string, any> }],
      aliases: this.config.registrySources.claimAliases,
      birthDateField: this.config.registrySources.birthDateField,
    });
  }

  /**
   * Requires HTTPS. Loopback is the only exception.
   *
   * A claim source is a DIFFERENT ORGANISATION's system — that is the whole point
   * of it existing — so this call leaves our network: a holder's identifier goes
   * out and their personal data comes back, across hops neither party controls.
   * Plain HTTP there is readable and rewritable by every one of them.
   *
   * Not the same judgement as `REGISTRY_BASE_URL=http://registry:8081`, and the
   * difference is the trust boundary rather than the payload: the registry is a
   * sibling service on the private network whose traffic never leaves the cluster.
   * Being "consistent" across those two would mean applying the weaker standard to
   * the hop that actually crosses the internet.
   *
   * Loopback stays exempt so the flow can be developed without provisioning a
   * certificate; that traffic never leaves the host either. There is deliberately
   * NO override for anything else — an escape hatch here would be found by the
   * first deployment that hit a certificate problem, and then it would be load
   * bearing.
   */
  private assertSafeUrl(raw: string): URL {
    let url: URL;
    try {
      url = new URL(raw);
    } catch {
      throw new ClaimSourceUnavailableError(this.name, `'${raw}' is not a valid URL`);
    }
    if (url.protocol === 'https:') return url;

    const loopback = ['localhost', '127.0.0.1', '::1', '[::1]'].includes(url.hostname);
    if (loopback) return url;

    throw new ClaimSourceUnavailableError(
      this.name,
      `refusing to send holder data to '${url.hostname}' over ${url.protocol}//. ` +
        `A claim source is another organisation's system, so this call leaves our ` +
        `network: use https. Plain http is accepted only for loopback, for local ` +
        `development.`,
    );
  }

  private async fetchClaims(url: URL, req: ClaimRequest): Promise<any> {
    // A wallet is blocked on this call, so it is bounded and not retried: a slow
    // upstream must surface as a named error rather than a request that hangs
    // until the holder gives up.
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.spec.timeoutMs ?? 5000);
    let res: Response;
    try {
      res = await fetch(url, {
        method: 'POST',
        // Never follow a redirect: the operator vetted THIS origin, and a 302
        // would send the holder's identifier somewhere they did not.
        redirect: 'error',
        signal: controller.signal,
        headers: {
          'content-type': 'application/json',
          accept: 'application/json',
          ...(this.spec.token ? { authorization: `Bearer ${this.spec.token}` } : {}),
        },
        body: JSON.stringify({
          subjectId: req.subjectId,
          subjectClaim: req.subjectClaim,
          credentialConfigurationId: req.credentialConfigurationId,
          credentialName: req.credentialName,
          attributes: req.properties,
        }),
      });
    } catch (err: any) {
      const reason =
        err?.name === 'AbortError'
          ? `did not respond within ${this.spec.timeoutMs ?? 5000}ms`
          : (err?.message ?? String(err));
      throw new ClaimSourceUnavailableError(this.name, reason);
    } finally {
      clearTimeout(timer);
    }

    // 404 is the holder, not the system: a record that was never created. Kept
    // distinct so an operator is not sent to check whether the service is up.
    if (res.status === 404) throw new SubjectNotFoundError(this.name, req.subjectId);
    if (!res.ok) {
      throw new ClaimSourceUnavailableError(this.name, `responded ${res.status}`);
    }

    try {
      const parsed = await res.json();
      // Deliberately no claim values in the log — they are the holder's personal
      // data. The provider, the subject and the outcome are enough to debug with.
      this.logger.log(`Claims resolved by '${this.name}' for ${req.subjectId}`);
      return parsed;
    } catch (err: any) {
      throw new ClaimSourceUnavailableError(
        this.name,
        `response was not valid JSON (${err?.message ?? err})`,
      );
    }
  }
}
