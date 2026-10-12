/**
 * Resolves a credential's claims from registry records — for ANY credential type,
 * over ANY registry entities.
 *
 * This file contains no domain knowledge. It does not know what a farmer, a crop
 * or a qualification is, and adding a credential type that issues over new
 * entities requires no change here. Three rules, in order, decide where a claim's
 * value comes from:
 *
 *   1. `age_over_NN` (any NN) is derived from the configured date-of-birth field.
 *   2. A configured alias maps the attribute to a field, optionally qualified by
 *      the entity it must come from (`Crop.cropName`).
 *   3. Otherwise the attribute name IS the field name, looked for on the subject
 *      record first and then on each related record in configured precedence
 *      order.
 *
 * Rule 3 is what makes a new credential type work with no configuration at all:
 * a schema whose property names match the registry's field names simply resolves.
 * Aliases exist only for the cases where a schema is obliged to use a different
 * name — a standardised claim like `birthdate`, say, over a registry field called
 * `dateOfBirth`.
 *
 * Attribute names are matched case-insensitively with separators stripped, so
 * `land_area_acres`, `landAreaAcres` and `LandAreaAcres` are one key: schemas in
 * a registry are authored at different times and are rarely consistent.
 *
 * KNOWN COUPLING: the issuer portal resolves claims the same way for the
 * staff-initiated path, in `issuer-portal/bff/claim-mapping.mjs`. Both are now
 * driven by the same declarative configuration, so they agree by construction
 * rather than by two tables being maintained in step — but the algorithms are
 * still two implementations, and a change to one belongs in both.
 */

const normalise = (attr: string) => attr.toLowerCase().replace(/[^a-z0-9]/g, '');

/** `age_over_18`, `ageOver21`, `age_over_65` — any threshold, not an enumeration. */
const AGE_OVER = /^ageover(\d{1,3})$/;

function isOlderThan(isoDate: string, years: number): boolean | undefined {
  const dob = new Date(isoDate);
  if (Number.isNaN(dob.getTime())) return undefined;
  // Calendar comparison, not elapsed milliseconds: a leap-year birthday
  // otherwise flips a day early or late.
  const threshold = new Date(dob.getFullYear() + years, dob.getMonth(), dob.getDate());
  return new Date() >= threshold;
}

/** One candidate record, with the name used for provenance and alias qualifying. */
export interface ClaimSource {
  /** Entity name, e.g. `LandParcel`. Also what an alias may qualify with. */
  entity: string;
  record?: Record<string, any>;
}

export interface ResolvedClaims {
  claims: Record<string, any>;
  /** Where each resolved claim came from, keyed by attribute. For display/audit. */
  sources: Record<string, string>;
  /** Required attributes that could not be filled. */
  missing: string[];
}

/**
 * Finds a field on a record, matching the name loosely.
 *
 * Returns the raw value, or undefined. A record whose own key differs only in
 * case or separators still matches, so the registry and the schema need not
 * agree on a convention.
 */
function pick(record: Record<string, any> | undefined, field: string): unknown {
  if (!record) return undefined;
  if (record[field] !== undefined) return record[field];
  const want = normalise(field);
  for (const [k, v] of Object.entries(record)) {
    if (normalise(k) === want) return v;
  }
  return undefined;
}

const filled = (v: unknown) => v !== undefined && v !== null && v !== '';

/**
 * Builds the claim set for one credential type.
 *
 * `sources` are ordered by precedence: the subject record should come first, then
 * supporting records. The order is what makes resolution deterministic when two
 * entities carry a field of the same name.
 *
 * Required attributes that cannot be filled are REPORTED rather than omitted:
 * issuing without them fails downstream in credentials-service with an opaque
 * 500 whose real reason only reaches that service's log, so the caller is told up
 * front which value is absent.
 */
export function resolveRegistryClaims(opts: {
  properties: string[];
  required?: string[];
  /** Candidate records in precedence order. */
  sources: ClaimSource[];
  /** Attribute -> field, optionally `Entity.field`. */
  aliases?: Record<string, string>;
  /** Field holding date of birth; empty disables `age_over_NN`. */
  birthDateField?: string;
}): ResolvedClaims {
  const { properties, required = [], sources, aliases = {}, birthDateField = '' } = opts;

  // Normalise the alias keys once, so callers may write them in any style.
  const aliasByKey = new Map<string, string>(
    Object.entries(aliases).map(([k, v]) => [normalise(k), v]),
  );

  const claims: Record<string, any> = {};
  const provenance: Record<string, string> = {};
  const missing: string[] = [];

  for (const attr of properties) {
    const key = normalise(attr);
    let value: unknown;
    let from: string | undefined;

    const ageOver = AGE_OVER.exec(key);
    if (ageOver && birthDateField) {
      const years = Number(ageOver[1]);
      // Date of birth is looked up through the same precedence chain, so it may
      // live on the subject or on a supporting record.
      for (const s of sources) {
        const dob = pick(s.record, birthDateField);
        if (filled(dob)) {
          value = isOlderThan(String(dob), years);
          from = `Derived from ${s.entity}.${birthDateField}`;
          break;
        }
      }
      if (from === undefined) from = `Needs ${birthDateField}`;
    } else {
      const alias = aliasByKey.get(key);
      // An alias may pin the entity (`Crop.cropName`) when a field name is
      // ambiguous across sources.
      const [aliasEntity, aliasField] = alias?.includes('.')
        ? [alias.slice(0, alias.indexOf('.')), alias.slice(alias.indexOf('.') + 1)]
        : [undefined, alias];
      const field = aliasField || attr;

      for (const s of sources) {
        if (aliasEntity && normalise(s.entity) !== normalise(aliasEntity)) continue;
        const v = pick(s.record, field);
        if (filled(v)) {
          value = v;
          from = `${s.entity}.${field}`;
          break;
        }
      }
      if (from === undefined) {
        from = aliasEntity ? `Needs ${aliasEntity}.${field}` : `No ${field} on any record`;
      }
    }

    // `false` and `0` are legitimate claim values. Treating them as absent would
    // silently turn "not over 18" into no assertion at all, and a zero-acre
    // parcel into a missing one.
    if (filled(value)) {
      claims[attr] = value;
      provenance[attr] = from;
    } else {
      provenance[attr] = from;
      if (required.includes(attr)) missing.push(attr);
    }
  }

  return { claims, sources: provenance, missing };
}
