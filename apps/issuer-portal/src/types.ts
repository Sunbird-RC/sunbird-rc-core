/** The signed-in user, as reported by the BFF from the access token. */
export type Session = {
  authenticated: boolean
  username?: string
  fullName?: string
  roles?: string[]
  /**
   * Present when the account is linked to a farmer record — a citizen's own
   * registry key, from the token claim.
   *
   * A DISPLAY HINT ONLY. The server re-reads it from the session on every
   * self-scoped route and never accepts it from the client, so changing it here
   * grants nothing.
   */
  farmerId?: string
  /** True when the BFF is running with PORTAL_MOCK=1 — surfaced so the UI can say so. */
  mock?: boolean
}

/**
 * An issuing authority: its own identity, its own credential type, its own
 * holders.
 *
 * This is the portal's top-level object — staff pick an issuer, then work on that
 * issuer's holders. Everything domain-specific about a holder screen (what they
 * are called, which supporting records they have, what gets issued) comes from
 * here rather than from code, so adding an issuer is data entry.
 */
export type Issuer = {
  osid?: string
  issuerId: string
  name: string
  description?: string
  category?: string
  /** The did:web this authority signs with, for display and audit. */
  did?: string
  /** OID4VCI credential_configuration_id this issuer issues. */
  credentialConfigId?: string
  credentialName?: string
  /** What this issuer calls its holders — Farmer, Citizen, Student. */
  holderLabel: string
  /** Prefix for generated holder ids, e.g. FRM. */
  holderIdPrefix?: string
  /** Comma-separated registry entities holding supporting records. */
  recordEntities?: string
  /** Logo image; a portal-served path or an absolute http(s) URL. */
  logoUrl?: string
  /** The authority's public homepage. Rendered only if http(s). */
  url?: string
  icon?: string
  accent?: string
  status?: 'Active' | 'Draft'
  /** Filled by the list endpoint. */
  holderCount?: number
}

export type Farmer = {
  /** Registry osid, absent until the record is created. */
  osid?: string
  /** Which issuer this holder belongs to. */
  issuerId?: string
  farmerId: string
  name: string
  gender?: string
  dateOfBirth?: string
  mobile?: string
  district?: string
  state?: string
  keycloakSub?: string
  keycloakUsername?: string
}

export type LandParcel = {
  osid?: string
  farmerId: string
  landRecordRef: string
  farmLocation?: string
  landAreaAcres: number
  ownershipType?: string
  surveyedOn?: string
}

export type Crop = {
  osid?: string
  farmerId: string
  landRecordRef?: string
  cropName: string
  season: string
  year: number
  areaSownAcres?: number
  expectedYieldQuintals?: number
}

export type SeedDistribution = {
  osid?: string
  farmerId: string
  seedType: string
  variety?: string
  quantityKg: number
  issuedOn: string
  subsidyScheme?: string
  distributionCentre?: string
}

export type Qualification = {
  osid?: string
  farmerId: string
  degree: string
  institution: string
  yearOfPassing?: number
  grade?: string
  enrolmentNumber?: string
}

/** Any of the child entities, for the generic table/form. */
export type Related = LandParcel | Crop | SeedDistribution | Qualification

/** Which registry entity a table is bound to. */
export type EntityKind = 'LandParcel' | 'Crop' | 'SeedDistribution' | 'Qualification'

/**
 * A credential type the portal can issue, discovered live from
 * /credential-schema/oid4vci-configs rather than hard-coded.
 */
export type CredentialType = {
  id: string
  name: string
  /** OID4VCI credential_configuration_id for the vc+sd-jwt variant. */
  configId: string
  vct: string
  /** The DID that signs this type's credentials. */
  issuer: string
  attributes: string[]
  descriptions: Record<string, string>
  jsonTypes: Record<string, string>
  /** Attributes the schema marks required — these must resolve or issuance fails. */
  required: string[]
}

/** A created credential offer, ready to show as a QR plus a PIN. */
export type Offer = {
  offerId: string
  qrData: string
  credentialOfferUri: string
  /** Present only when the offer was created with a transaction code. */
  txCode?: string
}

/** One field of the claim set, resolved from registry records. */
export type ResolvedClaim = {
  attribute: string
  value: unknown
  /** Which registry record the value came from, for the provenance column. */
  source: string
  required: boolean
}
