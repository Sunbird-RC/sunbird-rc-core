import { Injectable, Logger } from '@nestjs/common';
import { loadConfig } from '../config/configuration';
import {
  ClaimSourceNotConfiguredError,
  ClaimSourceUnavailableError,
  type ClaimSourceProvider,
} from './claim-source.interface';
import { HttpClaimSource } from './http.claim-source';
import { RegistryClaimSource } from './registry.claim-source';

/** The credential-type fields the factory may key on. */
export interface ClaimSourceTarget {
  schemaId: string;
  name: string;
}

/**
 * Chooses the claim source for a credential type.
 *
 * Per credential type rather than per process, so one deployment can serve an
 * authority that keeps its own records alongside authorities that use the Sunbird
 * registry — which is the actual shape of the multi-issuer deployment.
 */
@Injectable()
export class ClaimSourceFactory {
  private readonly logger = new Logger(ClaimSourceFactory.name);
  private readonly config = loadConfig();
  /** Built once per name: an HTTP source holds only config, so it is reusable. */
  private readonly httpSources = new Map<string, HttpClaimSource>();

  constructor(private readonly registrySource: RegistryClaimSource) {}

  /**
   * `schemaId` is tried before `name`: an id is unique, whereas two credential
   * types can legitimately share a display name, and silently picking the wrong
   * one would issue a credential from the wrong authority's data.
   */
  private nameFor(target: ClaimSourceTarget): string {
    const map = this.config.claimSourceMap;
    const hit = map[target.schemaId] ?? map[target.name];
    return (hit ?? this.config.claimSourceDefault).trim().toLowerCase();
  }

  for(target: ClaimSourceTarget): ClaimSourceProvider {
    const name = this.nameFor(target);

    // An explicit opt out. Not a misconfiguration: an authority may issue only
    // through POST /oid4vc/offer, supplying claims from its own backend, and never
    // want a credential mintable from a login alone.
    if (name === 'none') throw new ClaimSourceNotConfiguredError(target.name);

    if (name === 'registry') {
      if (!this.registrySource.available) {
        // Distinguished from `none` deliberately: this deployment asked for the
        // registry and did not configure it, which is a mistake worth naming,
        // whereas `none` is a decision.
        throw new ClaimSourceUnavailableError(
          'registry',
          'REGISTRY_BASE_URL is not set. Set it, map this credential type to another ' +
            'claim source in CLAIM_SOURCE_MAP, or set CLAIM_SOURCE_DEFAULT=none if this ' +
            'deployment issues only through POST /oid4vc/offer.',
        );
      }
      return this.registrySource;
    }

    const spec = this.config.claimSources[name];
    if (!spec) {
      throw new ClaimSourceUnavailableError(
        name,
        `no such claim source. Declare it by setting CLAIM_SOURCE_${name.toUpperCase()}_URL ` +
          `to the endpoint that authority hosts, or correct CLAIM_SOURCE_MAP.`,
      );
    }

    const existing = this.httpSources.get(name);
    if (existing) return existing;
    const created = new HttpClaimSource(spec);
    this.httpSources.set(name, created);
    this.logger.log(`Claim source '${name}' -> ${spec.url}`);
    return created;
  }
}
