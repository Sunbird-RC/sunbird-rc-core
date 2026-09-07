// Typed access to environment configuration for oid4vc-service.

/** One entity holding records that belong to a subject. */
export interface RelatedEntitySpec {
  /** Registry entity name, e.g. LandParcel. */
  entity: string;
  /** Field to sort on, so "the holder's current one" is deterministic. */
  orderBy?: string;
  /** Descending by default: the most recent record is the interesting one. */
  ascending?: boolean;
}

export interface RegistrySources {
  /** Entity holding the subject record itself. */
  subjectEntity: string;
  /** Field on that entity matching the token's subject claim. */
  subjectKey: string;
  /** Entities holding the subject's supporting records, in precedence order. */
  related: RelatedEntitySpec[];
  /**
   * Credential attribute -> registry field, for the cases a name match cannot
   * cover. Values may name an entity (`Crop.cropName`) or just a field
   * (`cropName`), in which case every source is searched in precedence order.
   *
   * Only needed where a schema's attribute name genuinely differs from the
   * registry's field name — a well-named schema needs no aliases at all.
   */
  claimAliases: Record<string, string>;
  /**
   * Field holding the subject's date of birth, enabling `age_over_NN` claims.
   * Empty disables age derivation rather than guessing a field name.
   */
  birthDateField: string;
}

/**
 * Parses `Entity[:orderField[:asc|desc]]` lists, e.g.
 * `LandParcel,Crop:year:desc,Qualification:yearOfPassing`.
 */
function parseRelated(raw: string): RelatedEntitySpec[] {
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((item) => {
      const [entity, orderBy, dir] = item.split(':').map((p) => p.trim());
      return {
        entity,
        ...(orderBy ? { orderBy } : {}),
        ...(dir?.toLowerCase() === 'asc' ? { ascending: true } : {}),
      };
    });
}

/**
 * Parses a JSON object of string -> string from an env var.
 *
 * Malformed input is warned about rather than thrown on, but never silently
 * dropped: both callers configure how claims are found, so a typo that reduced to
 * `{}` in silence would produce credentials quietly missing values.
 */
function parseStringMap(
  raw: string,
  envName: string,
  expected: string,
  warn: (m: string) => void,
): Record<string, string> {
  if (!raw.trim()) return {};
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error('not a JSON object');
    }
    return Object.fromEntries(
      Object.entries(parsed as Record<string, unknown>).map(([k, v]) => [k, String(v)]),
    );
  } catch (err: any) {
    warn(
      `${envName} could not be parsed (${err?.message ?? err}); continuing with none. ` +
        `Expected a JSON object of ${expected}.`,
    );
    return {};
  }
}

/** Parses the alias map, tolerating an unset or malformed value. */
function parseAliases(raw: string, warn: (m: string) => void): Record<string, string> {
  return parseStringMap(raw, 'REGISTRY_CLAIM_ALIASES', 'attribute -> field', warn);
}

/**
 * One named claim source: an endpoint the issuing authority hosts against its own
 * system of record.
 *
 * There is no `type` discriminator. `registry` is a built-in name usable directly
 * in `CLAIM_SOURCE_MAP`, so every source declared here is an HTTP one — and then
 * the URL says everything a type field would have. Should a future source type not
 * be URL-based, that is when a discriminator earns its place.
 */
export interface ClaimSourceSpec {
  /** Lower-cased name, as referenced by CLAIM_SOURCE_MAP / CLAIM_SOURCE_DEFAULT. */
  name: string;
  /** The endpoint. Its presence is what declares this source. */
  url: string;
  /** Bearer token, if the issuer's endpoint requires one. */
  token?: string;
  /** Request timeout. A wallet is waiting on this call, so it is bounded. */
  timeoutMs?: number;
}

/** Names with built-in meaning, which a declared source must not shadow. */
const RESERVED_SOURCE_NAMES = new Set(['registry', 'none']);

/**
 * Discovers named claim sources from `CLAIM_SOURCE_<NAME>_URL` variables.
 *
 * The URL is the declaration, so a source cannot exist in a half-configured state
 * where it is selectable but has nowhere to call — which would defer the failure to
 * the first holder who tried to collect a credential, and report it as their
 * problem rather than the operator's.
 *
 * Separate variables rather than one JSON blob so a bearer token stays a discrete
 * secret, injected like every other secret, instead of being embedded in a value
 * that gets echoed whole in logs and `docker compose config` output.
 */
function parseClaimSources(
  env: Record<string, string | undefined>,
  warn: (m: string) => void,
): Record<string, ClaimSourceSpec> {
  const out: Record<string, ClaimSourceSpec> = {};
  for (const key of Object.keys(env)) {
    const m = /^CLAIM_SOURCE_([A-Z0-9]+)_URL$/.exec(key);
    if (!m) continue;
    const name = m[1].toLowerCase();
    const url = (env[key] || '').trim();
    if (!url) continue;
    if (RESERVED_SOURCE_NAMES.has(name)) {
      warn(`${key} is ignored: '${name}' already has a built-in meaning.`);
      continue;
    }
    const prefix = `CLAIM_SOURCE_${m[1]}`;
    out[name] = {
      name,
      url,
      ...(env[`${prefix}_TOKEN`] ? { token: env[`${prefix}_TOKEN`] } : {}),
      timeoutMs: num(env[`${prefix}_TIMEOUT_MS`], 5000),
    };
  }
  return out;
}

export interface Oid4vcConfig {
  port: number;
  publicUrl: string;
  // Only for URLs that identity-service itself must dereference over HTTP
  // from inside its own container (currently: the ldp_vc @context document,
  // fetched by its JSON-LD signer) — distinct from publicUrl because that's
  // externally wallet-facing and, in the shipped compose stack, defaults to
  // http://localhost:3400, which resolves to identity-service's own loopback
  // from inside its container rather than back to this service.
  internalUrl: string;
  credentialServiceBaseUrl: string;
  identityBaseUrl: string;
  schemaBaseUrl: string;
  sessionStore: 'memory' | 'redis';
  redisUrl: string;
  issuerDid: string;
  /**
   * Advertise only the credentials this issuer authored.
   *
   * `credential-schema`'s /oid4vci-configs returns every published config in the
   * deployment, so an issuer metadata document built from it lists other
   * issuers' credentials too. Harmless with one issuer; wrong as soon as there
   * are two, because a wallet's issuer directory then shows the same credential
   * under whichever issuer the holder happened to open.
   *
   * Off by default: a single-issuer deployment behaves exactly as before, and
   * nothing changes for anyone who has not asked for it.
   */
  advertiseOwnCredentialsOnly: boolean;
  /**
   * Refuse a presentation that discloses more than the DCQL query asked for,
   * instead of dropping the surplus and answering normally.
   *
   * ON by default, unlike the flag above. That one widens what a wallet is shown
   * and could surprise an existing deployment, so it opts in; this one closes a
   * gap between what a verifier receives and what it can claim to have received,
   * and defaulting it off would ship the weaker guarantee to everyone who does
   * not know to look for it.
   *
   * Set REJECT_UNREQUESTED_DISCLOSURES=false to restore the previous behaviour —
   * useful when testing against a wallet that over-discloses and you need to see
   * the rest of the flow before fixing it.
   */
  rejectUnrequestedDisclosures: boolean;
  oid4vpEnabled: boolean;
  draft13CompatMode: boolean;
  vpSignRequest: boolean;
  vpLegacyClientIdScheme: boolean;
  verifierDid: string;
  auth: {
    enabled: boolean;
    jwksUri: string;
    issuers: string[];
  };
  /**
   * Keycloak-as-authorization-server, enabling wallet self-service issuance.
   *
   * When `keycloak.enabled`, issuer metadata advertises the Keycloak realm as the
   * authorization server, the wallet runs authorization_code + PKCE against
   * Keycloak directly (so this service implements no /authorize and no consent
   * UI), and the credential endpoint accepts Keycloak-issued access tokens —
   * sourcing claims from the registry record named by the token rather than from
   * anything the caller sends.
   */
  keycloak: {
    enabled: boolean;
    /** Browser-facing base, e.g. https://host/auth. Published in metadata. */
    publicUrl: string;
    /** In-cluster base for JWKS fetches. Defaults to publicUrl. */
    internalUrl: string;
    realm: string;
    /** Token claim carrying the subject's registry key. */
    subjectClaim: string;
    /** Expected `aud`; empty accepts any audience (Keycloak's default is azp-only). */
    audience: string;
  };
  /**
   * How the credential issuer presents itself in its own metadata
   * (OpenID4VCI `display` on the credential issuer).
   *
   * Matters for wallet-driven issuance: a wallet showing a directory of issuers
   * has nothing but this to label the entry, and falls back to the bare
   * hostname without it — so the citizen is asked to pick "98.70.36.106.sslip.io"
   * rather than "National Identity Authority". Empty name omits `display`
   * entirely rather than publishing a blank one.
   */
  issuerDisplay: { name: string; logoUri: string; locale: string };
  registryBaseUrl: string;
  /**
   * Where a self-issued credential's claims come from, as CONFIGURATION rather
   * than code.
   *
   * This service issues whatever credential types the schema registry publishes,
   * for whatever registry entities a deployment happens to have. It therefore
   * knows no domain nouns: which entity holds the subject, which entities hold
   * their supporting records, and how a credential attribute maps onto a field
   * are all declared here. Adding a new credential type — or a whole new issuing
   * domain — is a configuration change, not a code change.
   */
  registrySources: RegistrySources;
  /**
   * Named claim sources, keyed by lower-cased name.
   *
   * `registry` is always available (configured by `REGISTRY_*` above) and needs no
   * entry here; anything else is declared with `CLAIM_SOURCE_<NAME>_*`.
   */
  claimSources: Record<string, ClaimSourceSpec>;
  /**
   * Credential type -> claim source name. Keys match a credential's `schemaId` or
   * its name, in that order — the name is what an operator can read in a compose
   * file, the schemaId is unambiguous when two types share a name.
   */
  claimSourceMap: Record<string, string>;
  /**
   * Source for credential types not named in the map. `registry` preserves the
   * behaviour that predates per-type sources; `none` means credentials are issued
   * only through `POST /oid4vc/offer`, where the caller supplies the claims.
   */
  claimSourceDefault: string;
  /**
   * Locale stamped onto SD-JWT VC Type Metadata display entries that lack one.
   * A missing locale makes the document fail wallet-side validation outright.
   */
  defaultDisplayLocale: string;
  /**
   * Require an `issuer-staff` Keycloak token on POST /oid4vc/offer. Defaults
   * false so existing deployments keep working; production should set it true —
   * unauthenticated offer creation with caller-supplied claims lets anyone mint
   * a credential saying anything about anyone.
   */
  offerRequiresStaff: boolean;
  /**
   * Realm role that satisfies `offerRequiresStaff`.
   *
   * A role name belongs to the deployment's realm, not to this service, so it is
   * configuration — the one security-relevant identifier here that used to be a
   * literal. Defaulted rather than required, because unlike a registry field name
   * a wrong value fails closed (nobody can create an offer) instead of silently
   * reading the wrong data.
   */
  offerStaffRole: string;
  /** Accepted wallet key-proof algorithms, published in proof_types_supported. */
  ttl: {
    offer: number;
    nonce: number;
    accessToken: number;
    deferred: number;
    vpTxn: number;
  };
}

const num = (v: string | undefined, def: number) => {
  const n = parseInt(v || '', 10);
  return isNaN(n) ? def : n;
};

/**
 * Comma-separated env list, falling back to `def`.
 *
 * An env var set to only separators or blanks yields the default rather than an
 * empty list: publishing an EMPTY `credential_signing_alg_values_supported` tells
 * a wallet the issuer can sign with nothing, which is worse than a stale default
 * and reads in the metadata as if the field were broken.
 */
const list = (v: string | undefined, def: string[]): string[] => {
  const parsed = (v || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
  return parsed.length ? parsed : def;
};

export const loadConfig = (): Oid4vcConfig => ({
  port: num(process.env.PORT, 3400),
  publicUrl: process.env.PUBLIC_URL || 'http://localhost:3400',
  internalUrl: process.env.INTERNAL_URL || process.env.PUBLIC_URL || 'http://localhost:3400',
  credentialServiceBaseUrl:
    process.env.CREDENTIAL_SERVICE_BASE_URL || 'http://localhost:3000',
  identityBaseUrl: process.env.IDENTITY_BASE_URL || 'http://localhost:3332',
  schemaBaseUrl: process.env.SCHEMA_BASE_URL || 'http://localhost:3333',
  sessionStore: process.env.SESSION_STORE === 'redis' ? 'redis' : 'memory',
  redisUrl: process.env.REDIS_URL || 'redis://localhost:6379',
  issuerDid: process.env.ISSUER_DID || '',
  advertiseOwnCredentialsOnly: process.env.ADVERTISE_OWN_CREDENTIALS_ONLY === 'true',
  rejectUnrequestedDisclosures: process.env.REJECT_UNREQUESTED_DISCLOSURES !== 'false',
  oid4vpEnabled: process.env.OID4VP_ENABLED !== 'false',
  draft13CompatMode: process.env.DRAFT13_COMPAT_MODE === 'true',
  // OID4VP request-object mode. Legacy implies unsigned (the `redirect_uri`
  // client_id scheme MUST NOT be used with a signed request object), so it
  // overrides vpSignRequest regardless of how that flag is set.
  vpSignRequest:
    process.env.OID4VP_LEGACY_CLIENT_ID_SCHEME !== 'true' &&
    process.env.OID4VP_SIGN_REQUEST !== 'false',
  vpLegacyClientIdScheme: process.env.OID4VP_LEGACY_CLIENT_ID_SCHEME === 'true',
  // Deliberately NOT falling back to ISSUER_DID here. ISSUER_DID is commonly a
  // did:rcw (identity-service's own method — the documented setup step for it
  // uses method "rcw"), which only identity-service can resolve, so no wallet
  // could verify a request object signed with it. An explicit VERIFIER_DID is
  // an operator's deliberate choice and is trusted as-is; the ISSUER_DID /
  // auto-provisioned fallbacks are resolvability-checked in oid4vp.service.ts.
  verifierDid: process.env.VERIFIER_DID || '',
  // Keycloak bearer auth on POST /oid4vc/offer. Unset means OFF here, unlike
  // identity-service/credential-schema whose guards treat unset as ON — this
  // endpoint has always been open, so defaulting it closed would break every
  // existing deployment on upgrade. Operators opt in explicitly.
  auth: {
    enabled: (process.env.ENABLE_AUTH || '').trim() === 'true',
    jwksUri: (process.env.JWKS_URI || '').trim(),
    // Accepted `iss` values, comma-separated. Empty means "derive the realm URL
    // from JWKS_URI". A list is needed because Keycloak stamps `iss` with the
    // URL the token was OBTAINED at, and a deployment fronting one realm with
    // both a public and an in-cluster URL legitimately mints both spellings.
    issuers: (process.env.AUTH_ISSUER || '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean),
  },
  keycloak: {
    // Presence of a URL is the switch: there is no separate enable flag to get
    // out of sync with it.
    enabled: Boolean(process.env.KEYCLOAK_PUBLIC_URL),
    publicUrl: (process.env.KEYCLOAK_PUBLIC_URL || '').replace(/\/$/, ''),
    internalUrl: (
      process.env.KEYCLOAK_INTERNAL_URL ||
      process.env.KEYCLOAK_PUBLIC_URL ||
      ''
    ).replace(/\/$/, ''),
    realm: process.env.KEYCLOAK_REALM || 'sunbird-rc',
    // No default. This is a DOMAIN field name — which claim carries the holder's
    // registry key is a property of the deployment's realm, not of this service.
    // A default here (it used to be `farmerId`) also silently backfilled
    // registrySources.subjectKey below, so an unconfigured deployment searched a
    // borrowed field name and reported "no record found" instead of naming the
    // variable it was missing.
    subjectClaim: process.env.KEYCLOAK_SUBJECT_CLAIM || '',
    audience: process.env.KEYCLOAK_AUDIENCE || '',
  },
  issuerDisplay: {
    name: (process.env.ISSUER_DISPLAY_NAME || '').trim(),
    logoUri: (process.env.ISSUER_DISPLAY_LOGO_URI || '').trim(),
    locale: (process.env.ISSUER_DISPLAY_LOCALE || 'en-US').trim(),
  },
  registryBaseUrl: process.env.REGISTRY_BASE_URL || '',
  registrySources: {
    // No domain defaults. A deployment declares its own entities; leaving these
    // unset disables registry-sourced issuance with an explicit error naming the
    // variable, which is far better than defaulting to some other project's
    // entity name and reporting "no record found".
    subjectEntity: process.env.REGISTRY_SUBJECT_ENTITY || '',
    subjectKey:
      process.env.REGISTRY_SUBJECT_KEY || process.env.KEYCLOAK_SUBJECT_CLAIM || '',
    related: parseRelated(process.env.REGISTRY_RELATED_ENTITIES || ''),
    claimAliases: parseAliases(process.env.REGISTRY_CLAIM_ALIASES || '', (m) =>
      console.warn(`[configuration] ${m}`),
    ),
    birthDateField: process.env.REGISTRY_BIRTHDATE_FIELD || '',
  },
  claimSources: parseClaimSources(process.env, (m) => console.warn(`[configuration] ${m}`)),
  claimSourceMap: parseStringMap(
    process.env.CLAIM_SOURCE_MAP || '',
    'CLAIM_SOURCE_MAP',
    'credential schemaId or name -> claim source name',
    (m) => console.warn(`[configuration] ${m}`),
  ),
  // `registry` by default: this setting was introduced after registry-sourced
  // issuance was already deployed, and silently switching those deployments to
  // `none` would take away a working flow.
  claimSourceDefault: (process.env.CLAIM_SOURCE_DEFAULT || 'registry').trim().toLowerCase(),
  defaultDisplayLocale: process.env.DEFAULT_DISPLAY_LOCALE || 'en-US',
  offerRequiresStaff: process.env.OFFER_REQUIRES_STAFF === 'true',
  offerStaffRole: process.env.OFFER_STAFF_ROLE || 'issuer-staff',
  ttl: {
    offer: num(process.env.OFFER_TTL, 600),
    nonce: num(process.env.NONCE_TTL, 300),
    accessToken: num(process.env.ACCESS_TOKEN_TTL, 300),
    deferred: num(process.env.DEFERRED_TTL, 86400),
    vpTxn: num(process.env.VP_TXN_TTL, 300),
  },
});

export const CONFIG = Symbol('OID4VC_CONFIG');
