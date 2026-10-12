import { HttpService } from '@nestjs/axios';
import { Injectable, Logger } from '@nestjs/common';
import { firstValueFrom } from 'rxjs';
import { loadConfig, type RelatedEntitySpec } from '../config/configuration';
import type { ClaimSource } from '../oid4vci/registry-claims.util';

/**
 * Reads entity records from the Sunbird RC registry.
 *
 * This exists so wallet self-service issuance can source a credential's claims
 * from the system of record rather than from the request. In that flow the
 * caller is the holder's own wallet, so anything it sends about itself is
 * unverified by definition — the only trustworthy input is the subject id inside
 * its Keycloak access token, which this client turns into a record.
 *
 * Which entities those are is CONFIGURATION (`registrySources`), not code: this
 * client works for any registry, and issuing a new credential type over new
 * entities needs no change here.
 */
@Injectable()
export class RegistryClient {
  private readonly logger = new Logger(RegistryClient.name);
  private readonly config = loadConfig();

  constructor(private readonly http: HttpService) {}

  get enabled(): boolean {
    return Boolean(this.config.registryBaseUrl);
  }

  private get baseUrl(): string {
    return this.config.registryBaseUrl.replace(/\/$/, '');
  }

  /**
   * Searches one entity type. The service account's own token is used, not the
   * holder's: a citizen's token carries the `citizen` role and the registry
   * refuses reads with it, yet the record is legitimately needed to issue to
   * them. Authorisation is enforced by the caller instead — a holder only ever
   * reaches the record named by their own token's subject claim.
   */
  async search<T = Record<string, any>>(
    entity: string,
    filters: Record<string, unknown>,
    limit = 100,
  ): Promise<T[]> {
    if (!this.enabled) return [];
    try {
      const res = await firstValueFrom(
        this.http.post(
          `${this.baseUrl}/api/v1/${encodeURIComponent(entity)}/search`,
          { offset: 0, limit, filters },
          { headers: { 'content-type': 'application/json' } },
        ),
      );
      const data = res.data;
      // The registry returns a bare array on some versions and {data:[…]} on
      // others; normalise rather than depending on which is deployed.
      return Array.isArray(data) ? data : (data?.data ?? []);
    } catch (err: any) {
      this.logger.error(
        `Registry search ${entity} failed: ${err?.response?.status ?? ''} ${err?.message ?? err}`,
      );
      throw new Error(`registry unavailable: ${err?.message ?? err}`);
    }
  }

  /** The single record for a subject id, or undefined. */
  async findOne<T = Record<string, any>>(
    entity: string,
    field: string,
    value: string,
  ): Promise<T | undefined> {
    const rows = await this.search<T>(entity, { [field]: { eq: value } }, 1);
    return rows[0];
  }

  /**
   * Everything issuance needs about one subject, as claim sources in precedence
   * order: the subject record first, then one record from each configured related
   * entity.
   *
   * The entities come from configuration, so this method names none of them. All
   * of them are fetched regardless of which credential is being issued, because
   * the claim resolver — not this client — decides which records a given type
   * draws on. Fetching per credential type would split that decision across two
   * places, and the searches are indexed and run concurrently.
   *
   * ONE record per related entity, not the list: the holder does not get to choose
   * which of their records a self-issued credential describes (a wallet has no UI
   * for it, and letting the request choose would be a way to ask for someone
   * else's row). The configured sort makes that choice deterministic — newest
   * first unless the deployment says otherwise.
   */
  async subjectSources(subjectId: string): Promise<{
    subject?: Record<string, any>;
    sources: ClaimSource[];
  }> {
    const { subjectEntity, subjectKey, related } = this.config.registrySources;
    if (!subjectEntity || !subjectKey) {
      throw new Error(
        'registry claim sources are not configured: set REGISTRY_SUBJECT_ENTITY and ' +
          'REGISTRY_SUBJECT_KEY (or KEYCLOAK_SUBJECT_CLAIM) to the entity and field ' +
          'holding the subject record',
      );
    }

    const [subject, ...relatedRecords] = await Promise.all([
      this.findOne(subjectEntity, subjectKey, subjectId),
      ...related.map((spec) =>
        this.search(spec.entity, { [subjectKey]: { eq: subjectId } })
          // An entity a deployment has not created answers 4xx. That must not
          // fail issuance of credentials which never needed it: treated as "no
          // records", so the resolver reports any REQUIRED claim it fed as
          // missing, which names the real problem instead of a 500.
          .catch(() => [] as Record<string, any>[])
          .then((rows) => this.pickOne(rows, spec)),
      ),
    ]);

    return {
      subject,
      sources: [
        { entity: subjectEntity, record: subject },
        ...related.map((spec, i) => ({ entity: spec.entity, record: relatedRecords[i] })),
      ],
    };
  }

  /** The one record a credential should describe, per the entity's sort order. */
  private pickOne(
    rows: Record<string, any>[],
    spec: RelatedEntitySpec,
  ): Record<string, any> | undefined {
    if (!spec.orderBy) return rows[0];
    const dir = spec.ascending ? 1 : -1;
    // Numeric where both sides are numeric (years, sequence numbers), string
    // otherwise (ISO dates sort correctly as strings, which covers most of the
    // rest without having to know the field's type).
    const sorted = [...rows].sort((a, b) => {
      const x = a?.[spec.orderBy!];
      const y = b?.[spec.orderBy!];
      const nx = Number(x);
      const ny = Number(y);
      if (Number.isFinite(nx) && Number.isFinite(ny)) return (nx - ny) * dir;
      return String(x ?? '').localeCompare(String(y ?? '')) * dir;
    });
    return sorted[0];
  }
}
