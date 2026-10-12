import { Injectable } from '@nestjs/common';
import { RegistryClient } from '../clients/registry.client';
import { loadConfig } from '../config/configuration';
import { resolveRegistryClaims } from '../oid4vci/registry-claims.util';
import {
  ClaimSourceUnavailableError,
  SubjectNotFoundError,
  type ClaimRequest,
  type ClaimSourceProvider,
} from './claim-source.interface';

/**
 * Claims from the Sunbird RC registry.
 *
 * An adapter, not new behaviour: this is exactly what the issuance path did
 * inline before claim sources became selectable, moved behind the interface so the
 * registry is one option rather than a precondition for issuing.
 */
@Injectable()
export class RegistryClaimSource implements ClaimSourceProvider {
  readonly name = 'registry';
  private readonly config = loadConfig();

  constructor(private readonly registry: RegistryClient) {}

  /** False when REGISTRY_BASE_URL is unset, so the factory can say so precisely. */
  get available(): boolean {
    return this.registry.enabled;
  }

  async resolve(req: ClaimRequest) {
    if (!this.registry.enabled) {
      throw new ClaimSourceUnavailableError(this.name, 'REGISTRY_BASE_URL is not set');
    }

    // Sources come back in precedence order for whatever entities this deployment
    // declares. Which entities those are is configuration — this code names none.
    const { subject, sources } = await this.registry.subjectSources(req.subjectId);
    if (!subject) throw new SubjectNotFoundError(this.name, req.subjectId);

    return resolveRegistryClaims({
      properties: req.properties,
      required: req.required,
      sources,
      aliases: this.config.registrySources.claimAliases,
      birthDateField: this.config.registrySources.birthDateField,
    });
  }
}
