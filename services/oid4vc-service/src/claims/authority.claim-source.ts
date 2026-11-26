import { Logger } from '@nestjs/common';
import { createHash } from 'node:crypto';
import { loadConfig } from '../config/configuration';
import { AuthorityTokenProvider, authorityTokenProvider } from './authority-token';
import { resolveRegistryClaims } from '../oid4vci/registry-claims.util';
import {
  ClaimSourceUnavailableError,
  SubjectNotFoundError,
  type ClaimRequest,
  type ClaimSourceProvider,
} from './claim-source.interface';

/**
 * Claims from a Sunbird RC Authority Service, and the linkage back to it.
 *
 * Two credentials exist for one subject, deliberately, and they are not the same
 * credential:
 *
 *   - The Authority Service's JSON-LD credential is the authoritative status and
 *     provenance anchor. It is what `GET /trust/credentials/{id}/status` answers
 *     about, and it never leaves the Authority.
 *   - The SD-JWT this service issues is the holder-bound presentation credential.
 *     It is what the wallet holds and what a verifier sees.
 *
 * This source ties them together. It asks the Authority Service to issue, server to
 * server, and carries the resulting credential identifier into the SD-JWT as
 * `authorityCredentialId`, so a verifier holding the presentation credential can
 * ask the Authority about the anchor's current standing.
 *
 * Why that identifier can only come from here: this provider is the only thing in
 * the issuance path that talks to the Authority Service, and `resolve` is called
 * with a subject taken from a validated token. Nothing a wallet, holder or caller
 * sends reaches this method. The claim is never accepted from anywhere. It is
 * produced, from an Authority response, or not at all.
 */
export class AuthorityClaimSource implements ClaimSourceProvider {
  private readonly logger = new Logger(AuthorityClaimSource.name);
  private readonly config = loadConfig();

  /**
   * Client credentials, exchanged for a token whenever the current one is near expiry.
   * Null when this deployment configures a static token or custom headers instead.
   */
  private readonly tokens: AuthorityTokenProvider | null = authorityTokenProvider({
    tokenUrl: this.config.authority?.tokenUrl,
    clientId: this.config.authority?.clientId,
    clientSecret: this.config.authority?.clientSecret,
    scope: this.config.authority?.scope,
    timeoutMs: this.config.authority?.timeoutMs,
  });

  /** The claim carrying the Authority credential's identifier. Technical linkage. */
  static readonly LINKAGE_CLAIM = 'authorityCredentialId';

  get name(): string {
    return 'authority';
  }

  async resolve(req: ClaimRequest) {
    const base = (this.config.authority?.baseUrl ?? '').replace(/\/$/, '');
    if (!base) {
      throw new ClaimSourceUnavailableError(
        'authority',
        'AUTHORITY_BASE_URL is not set. Set it, or map this credential type to another ' +
          'claim source in CLAIM_SOURCE_MAP.',
      );
    }

    const profileId = this.profileFor(req);
    const profile = await this.get(base, `/api/v1/credential-profiles/${profileId}`);
    const bindingId = profile?.registryBindingId;
    if (!bindingId) {
      throw new ClaimSourceUnavailableError(
        'authority',
        `credential profile ${profileId} does not name a registry binding`,
      );
    }

    const osid = await this.findRecord(base, bindingId, req.subjectId);
    const issued = await this.issue(base, profileId, osid, req.subjectId);

    const credentialId = typeof issued?.credentialId === 'string' ? issued.credentialId : '';
    if (!credentialId) {
      // Without the identifier the SD-JWT cannot be linked back to its anchor and a
      // verifier would have nothing to resolve. Refuse, rather than issue a credential
      // that silently cannot be status-checked.
      throw new ClaimSourceUnavailableError(
        'authority',
        'the Authority Service issued no credential identifier',
      );
    }

    // The Authority's mapped claims, plus the linkage. Run through the SAME resolver
    // every other source uses, so only attributes the credential type DECLARES
    // survive. The Authority cannot add claims the schema does not define, any more
    // than an issuer's own endpoint can.
    const record = {
      ...(issued.claims && typeof issued.claims === 'object' ? issued.claims : {}),
      [AuthorityClaimSource.LINKAGE_CLAIM]: credentialId,
    };

    this.logger.log(
      `issued ${credentialId} from profile ${profileId}, record ${osid}`,
    );

    return resolveRegistryClaims({
      properties: req.properties,
      required: req.required,
      // One source, named after the provider, so provenance in the portal and in the
      // "missing" message points at the Authority that issued.
      sources: [{ entity: this.name, record }],
      aliases: this.config.registrySources.claimAliases,
      birthDateField: this.config.registrySources.birthDateField,
    });
  }

  /** Which Authority credential profile issues this credential type. */
  private profileFor(req: ClaimRequest): string {
    const map = this.config.authority?.profileMap ?? {};
    const hit = map[req.credentialConfigurationId] ?? map[req.credentialName];
    if (!hit) {
      throw new ClaimSourceUnavailableError(
        'authority',
        `no Authority credential profile is mapped for "${req.credentialName}". ` +
          'Set AUTHORITY_PROFILE_MAP.',
      );
    }
    return hit;
  }

  /**
   * Finds the subject's record through the Authority Service.
   *
   * A search matching more than one record is refused rather than resolved to the
   * first. Issuing from an ambiguous match would attach a credential to whichever
   * record happened to sort first, and nothing downstream could tell.
   */
  private async findRecord(base: string, bindingId: string, subjectId: string): Promise<string> {
    const field = this.config.authority?.subjectField || 'nationalId';
    const body = await this.post(base, `/api/v1/registries/${bindingId}/records/search`, {
      filters: { [field]: { eq: subjectId } },
      limit: 2,
    });
    const rows: any[] = Array.isArray(body?.data) ? body.data : [];
    if (rows.length === 0) throw new SubjectNotFoundError('authority', subjectId);
    if (rows.length > 1) {
      throw new ClaimSourceUnavailableError(
        'authority',
        `more than one record matches ${field} for this subject; refusing to choose`,
      );
    }
    const osid = rows[0]?.osid;
    if (typeof osid !== 'string' || !osid) throw new SubjectNotFoundError('authority', subjectId);
    return osid;
  }

  /**
   * Asks the Authority Service to issue, with a key derived from what is being
   * issued rather than from the moment of asking.
   *
   * A wallet that retries, after a dropped response or a user tapping twice, must
   * get the same Authority credential back. A random key would mint a second anchor
   * for one presentation credential and quietly break the one-to-one linkage that
   * makes the status answer meaningful.
   *
   * The subject is included so two records sharing a profile cannot collide, and the
   * whole key is hashed so an identifier read from a validated token is not echoed
   * into another service's logs.
   */
  private async issue(base: string, profileId: string, osid: string, subjectId: string) {
    const idempotencyKey = createHash('sha256')
      .update(`${profileId} ${osid} ${subjectId}`)
      .digest('hex')
      .slice(0, 32);
    return this.post(base, `/api/v1/credential-profiles/${profileId}/issue`, {
      osid,
      idempotencyKey,
    });
  }

  private async headers(): Promise<Record<string, string>> {
    const h: Record<string, string> = { 'content-type': 'application/json' };
    // Client credentials first: a token acquired now, and re-acquired when it ages out.
    // The static token below remains for deployments that do not use OAuth2 at all.
    const token = this.tokens ? await this.tokens.get() : this.config.authority?.token;
    // Server to server. The holder's token is deliberately NOT forwarded: this call is
    // made as the issuing service, and a holder must not be able to act as it.
    if (token) h.authorization = `Bearer ${token}`;
    // Whatever else this deployment's Authority Service expects to identify the caller.
    // A bearer token covers the ordinary case; this covers the rest without teaching the
    // issuer about any particular authentication scheme — and without a development-only
    // branch in code that also runs in production.
    //
    // The issuing service must reach the Authority as a principal entitled to issue for the
    // tenant that owns the record. It is acting as that Authority's issuing officer, and
    // nothing here grants it more than the Authority already decided to grant.
    Object.assign(h, this.config.authority?.headers ?? {});
    return h;
  }

  private async get(base: string, path: string) {
    return this.call(base, path, { method: 'GET', headers: await this.headers() });
  }

  private async post(base: string, path: string, body: unknown) {
    return this.call(base, path, {
      method: 'POST',
      headers: await this.headers(),
      body: JSON.stringify(body),
    });
  }

  private async call(base: string, path: string, init: RequestInit, retried = false) {
    const url = `${base}${path}`;
    const timeoutMs = this.config.authority?.timeoutMs ?? 8000;
    let response: Response;
    try {
      response = await fetch(url, { ...init, signal: AbortSignal.timeout(timeoutMs) });
    } catch (err: any) {
      throw new ClaimSourceUnavailableError('authority', `${path}: ${err?.message ?? err}`);
    }
    if ((response.status === 401 || response.status === 403) && this.tokens && !retried) {
      // The token was refused. It may have been revoked, or the two clocks may disagree by
      // more than the renewal margin. Drop it and try once with a fresh one — a retry here
      // is the difference between a transient mismatch and a failed issuance for a holder
      // who is standing there waiting.
      this.logger.warn(`${path} answered ${response.status}; re-acquiring a token and retrying`);
      this.tokens.invalidate();
      const headers = await this.headers();
      return this.call(base, path, { ...init, headers }, true);
    }
    if (response.status === 404) {
      // A 404 here is a profile or binding that does not exist, which is a
      // configuration fault. "No record for this subject" is decided in findRecord
      // from an empty search result, not from a status code, so the two stay
      // distinguishable: one sends an operator to the configuration, the other to
      // provisioning.
      throw new ClaimSourceUnavailableError('authority', `${path} answered 404`);
    }
    if (!response.ok) {
      const detail = await response.text().catch(() => '');
      throw new ClaimSourceUnavailableError(
        'authority',
        `${path} answered ${response.status}${detail ? `: ${detail.slice(0, 200)}` : ''}`,
      );
    }
    try {
      return await response.json();
    } catch {
      throw new ClaimSourceUnavailableError('authority', `${path} returned unreadable JSON`);
    }
  }
}
