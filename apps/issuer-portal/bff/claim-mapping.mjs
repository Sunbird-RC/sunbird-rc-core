// How a credential's attributes are filled from registry records.
//
// This file holds NO domain knowledge — no farmers, no crops, no qualifications.
// Three rules decide where a claim's value comes from, in order:
//
//   1. `age_over_NN` (any NN) is derived from the configured date-of-birth field.
//   2. A configured alias maps the attribute to a field, optionally qualified by
//      the entity it must come from (`Crop.cropName`).
//   3. Otherwise the attribute name IS the field name, looked for on the subject
//      record first and then on each related record in configured order.
//
// Rule 3 is what lets a new credential type work with no configuration at all.
//
// KNOWN COUPLING: oid4vc-service performs the same resolution at issuance time
// for the wallet self-service path, where the portal is not involved
// (src/oid4vci/registry-claims.util.ts). Both are driven by the SAME environment
// configuration, so the two cannot disagree about which entity a claim comes from
// — that used to be two hand-maintained tables, and the drift it invited is the
// reason this is now one declarative rule set. The algorithms are still two
// implementations: a change to one belongs in both.
//
// Attribute names are matched case-insensitively and with separators stripped
// (`land_area_acres`, `landAreaAcres` and `LandAreaAcres` are the same key),
// because schemas are authored at different times and are rarely consistent.

const normalise = (s) => String(s).toLowerCase().replace(/[^a-z0-9]/g, '')

/** `age_over_18`, `ageOver21`, `age_over_65` — any threshold. */
const AGE_OVER = /^ageover(\d{1,3})$/

function isOlderThan(isoDate, years) {
  const dob = new Date(isoDate)
  if (Number.isNaN(dob.getTime())) return undefined
  // Compare calendar dates, not elapsed milliseconds: a leap-year birthday
  // otherwise flips a day early or late.
  const threshold = new Date(dob.getFullYear() + years, dob.getMonth(), dob.getDate())
  return new Date() >= threshold
}

const filled = (v) => v !== undefined && v !== null && v !== ''

/** Finds a field on a record, matching the name loosely. */
function pick(record, field) {
  if (!record) return undefined
  if (record[field] !== undefined) return record[field]
  const want = normalise(field)
  for (const [k, v] of Object.entries(record)) {
    if (normalise(k) === want) return v
  }
  return undefined
}

/**
 * Reads the claim-source declaration from the environment.
 *
 * The same four variables oid4vc-service reads, so the portal's preview of what
 * will be issued matches what actually gets issued.
 */
export function loadClaimSources(env = process.env) {
  const related = (env.REGISTRY_RELATED_ENTITIES || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((item) => {
      const [entity, orderBy, dir] = item.split(':').map((p) => p.trim())
      return { entity, orderBy: orderBy || undefined, ascending: dir?.toLowerCase() === 'asc' }
    })

  let claimAliases = {}
  if ((env.REGISTRY_CLAIM_ALIASES || '').trim()) {
    try {
      const parsed = JSON.parse(env.REGISTRY_CLAIM_ALIASES)
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) claimAliases = parsed
      else throw new Error('not a JSON object')
    } catch (err) {
      // Ignoring it silently would show staff a claim table missing values that
      // issuance will actually fill, or vice versa.
      console.warn(
        `[claim-mapping] REGISTRY_CLAIM_ALIASES could not be parsed (${err.message}); ` +
          `continuing with no aliases`,
      )
    }
  }

  return {
    subjectEntity: env.REGISTRY_SUBJECT_ENTITY || '',
    subjectKey: env.REGISTRY_SUBJECT_KEY || env.KEYCLOAK_SUBJECT_CLAIM || '',
    related,
    claimAliases,
    birthDateField: env.REGISTRY_BIRTHDATE_FIELD || '',
  }
}

/** The one record a credential should describe, per the entity's sort order. */
export function pickOne(rows, spec) {
  if (!Array.isArray(rows) || rows.length === 0) return undefined
  if (!spec?.orderBy) return rows[0]
  const dir = spec.ascending ? 1 : -1
  return [...rows].sort((a, b) => {
    const x = a?.[spec.orderBy]
    const y = b?.[spec.orderBy]
    const nx = Number(x)
    const ny = Number(y)
    if (Number.isFinite(nx) && Number.isFinite(ny)) return (nx - ny) * dir
    return String(x ?? '').localeCompare(String(y ?? '')) * dir
  })[0]
}

/**
 * Resolves one credential type's attributes from candidate records.
 *
 * `sources` are `{ entity, record }` in precedence order — the subject record
 * first, then supporting records. That order is what makes resolution
 * deterministic when two entities carry a field of the same name.
 *
 * Returns the claims, per-attribute provenance, and the REQUIRED attributes that
 * could not be filled. Missing required values are reported rather than silently
 * omitted: issuing without them fails at the credential endpoint with an opaque
 * 500 whose real reason only reaches the service log.
 */
export function resolveClaims({
  attributes,
  required = [],
  sources = [],
  aliases = {},
  birthDateField = '',
}) {
  const aliasByKey = new Map(Object.entries(aliases).map(([k, v]) => [normalise(k), String(v)]))

  const claims = {}
  const provenance = {}
  const missing = []

  for (const attr of attributes) {
    const key = normalise(attr)
    let value
    let source

    const ageOver = AGE_OVER.exec(key)
    if (ageOver && birthDateField) {
      const years = Number(ageOver[1])
      for (const s of sources) {
        const dob = pick(s.record, birthDateField)
        if (filled(dob)) {
          value = isOlderThan(String(dob), years)
          source = `Derived · ${s.entity} ${birthDateField}`
          break
        }
      }
      if (source === undefined) source = `Needs ${birthDateField}`
    } else {
      const alias = aliasByKey.get(key)
      const [aliasEntity, aliasField] = alias?.includes('.')
        ? [alias.slice(0, alias.indexOf('.')), alias.slice(alias.indexOf('.') + 1)]
        : [undefined, alias]
      const field = aliasField || attr

      for (const s of sources) {
        if (aliasEntity && normalise(s.entity) !== normalise(aliasEntity)) continue
        const v = pick(s.record, field)
        if (filled(v)) {
          value = v
          source = `${s.entity} · ${field}`
          break
        }
      }
      if (source === undefined) {
        source = aliasEntity ? `Needs ${aliasEntity} · ${field}` : `No ${field} on any record`
      }
    }

    // `false` and `0` are legitimate claim values. Treating them as absent would
    // turn "not over 18" into no assertion at all, and a zero-acre parcel into a
    // missing one.
    if (filled(value)) {
      claims[attr] = value
      provenance[attr] = source
    } else {
      provenance[attr] = source
      if (required.includes(attr)) missing.push(attr)
    }
  }

  return { claims, sources: provenance, missing }
}
