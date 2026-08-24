import type { ResolvedClaims } from '../oid4vci/registry-claims.util';

/**
 * Where a credential's claims come from.
 *
 * Issuing and record-keeping are deliberately separable: an authority may run its
 * farmers and land parcels in its own database and never adopt the Sunbird
 * registry, yet still issue through this service. The registry is therefore one
 * implementation of this interface rather than a dependency of issuance.
 *
 * Note what is NOT here: authentication. Who the holder is has already been
 * established by the time a provider is called — the Keycloak token was verified
 * and `subjectId` read out of it — and no provider is given the chance to
 * influence that. A provider answers only "what do we know about this subject".
 */
export interface ClaimSourceProvider {
  /** Configured name, used in errors and logs so a failure names its source. */
  readonly name: string;
  resolve(req: ClaimRequest): Promise<ResolvedClaims>;
}

export interface ClaimRequest {
  /**
   * The holder's key in the issuing authority's system.
   *
   * Taken from the VALIDATED access token, never from the request body — that is
   * what makes "a holder cannot ask for someone else's credential" structural
   * rather than a check that could be forgotten.
   */
  subjectId: string;
  /** Token claim `subjectId` came from. Passed on so an issuer can log it. */
  subjectClaim: string;
  /** Which credential is being issued. */
  credentialConfigurationId: string;
  credentialName: string;
  /** The attributes this credential type declares, and which of them are required. */
  properties: string[];
  required: string[];
}

/**
 * A provider could not answer because the SOURCE failed — unreachable, timed out,
 * or returned an error of its own.
 *
 * Distinguished from "this subject has no record" on purpose. The two need
 * different messages: one is the issuing authority's system being down, the other
 * is a holder who was never provisioned, and telling an operator the wrong one
 * sends them looking in the wrong place.
 */
export class ClaimSourceUnavailableError extends Error {
  constructor(
    readonly source: string,
    reason: string,
  ) {
    super(`claim source '${source}' is unavailable: ${reason}`);
    this.name = 'ClaimSourceUnavailableError';
  }
}

/** The source answered, and has no record for this subject. */
export class SubjectNotFoundError extends Error {
  constructor(
    readonly source: string,
    readonly subjectId: string,
  ) {
    super(`no record for subject '${subjectId}' in claim source '${source}'`);
    this.name = 'SubjectNotFoundError';
  }
}

/**
 * No claim source is configured for a credential type, so nothing can be issued
 * from a login alone. Not an error state for the deployment: an authority may
 * deliberately issue only through `POST /oid4vc/offer`, where its own backend
 * supplies the claims.
 */
export class ClaimSourceNotConfiguredError extends Error {
  constructor(credentialName: string) {
    super(
      `no claim source is configured for '${credentialName}', so it cannot be issued from a ` +
        `holder login. Map it in CLAIM_SOURCE_MAP, set CLAIM_SOURCE_DEFAULT, or issue it ` +
        `through POST /oid4vc/offer instead.`,
    );
    this.name = 'ClaimSourceNotConfiguredError';
  }
}
