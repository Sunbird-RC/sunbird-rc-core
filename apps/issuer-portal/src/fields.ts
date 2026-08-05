import type { EntityKind } from './types'

/**
 * Form and table metadata for each registry entity, mirroring
 * registry-schemas/*.json.
 *
 * Deliberately hand-written rather than derived from the registry's schema
 * endpoint: these drive labels, ordering, column choice and which fields appear
 * in a compact table, none of which JSON Schema expresses. The registry still
 * validates on write, so this is presentation only — a mismatch shows up as a
 * clear validation error rather than as silently dropped data.
 */
export type FieldSpec = {
  name: string
  label: string
  type: 'text' | 'number' | 'date' | 'select'
  required?: boolean
  options?: string[]
  help?: string
  /** Show in the compact table, not just the edit form. */
  inTable?: boolean
  /** Right-align and tabular-format (numbers). */
  numeric?: boolean
}

export const FARMER_FIELDS: FieldSpec[] = [
  {
    name: 'farmerId',
    label: 'Holder ID',
    type: 'text',
    required: true,
    help: 'Unique across the registry. Suggested from this issuer’s series; editable. Cannot be changed later.',
  },
  { name: 'name', label: 'Full name', type: 'text', required: true },
  { name: 'gender', label: 'Gender', type: 'select', options: ['Female', 'Male', 'Other'] },
  { name: 'dateOfBirth', label: 'Date of birth', type: 'date' },
  { name: 'mobile', label: 'Mobile number', type: 'text' },
  { name: 'district', label: 'District', type: 'text' },
  { name: 'state', label: 'State', type: 'text' },
]

/**
 * Holder fields labelled with the issuer's own noun.
 *
 * "Holder ID" is accurate but reads as jargon to staff who only ever work with
 * one issuer; a Student ID field on the Education issuer's form is what they
 * expect to see.
 */
export function holderFields(holderLabel?: string): FieldSpec[] {
  if (!holderLabel) return FARMER_FIELDS
  return FARMER_FIELDS.map((f) =>
    f.name === 'farmerId' ? { ...f, label: `${holderLabel} ID` } : f,
  )
}

export const ISSUER_FIELDS: FieldSpec[] = [
  {
    name: 'name',
    label: 'Issuer name',
    type: 'text',
    required: true,
    help: 'The authority as it should appear to staff, e.g. State Board of Education.',
  },
  {
    name: 'holderLabel',
    label: 'Holders are called',
    type: 'text',
    required: true,
    help: 'Singular, e.g. Farmer, Citizen, Student. Used throughout this issuer’s screens.',
  },
  {
    name: 'holderIdPrefix',
    label: 'Holder ID prefix',
    type: 'text',
    help: 'Short and upper-case, e.g. EDU — generated ids become EDU-000001.',
  },
  {
    name: 'category',
    label: 'Category',
    type: 'select',
    options: ['Agriculture', 'Identity', 'Education', 'Health', 'Other'],
  },
  {
    name: 'description',
    label: 'Description',
    type: 'text',
    help: 'One line explaining what this issuer issues and to whom.',
  },
  {
    name: 'logoUrl',
    label: 'Logo URL',
    type: 'text',
    help: 'Shown on the issuer card. A portal path like /issuer-portal/logos/x.svg, or an https URL.',
  },
  {
    name: 'url',
    label: 'Website',
    type: 'text',
    help: 'Linked from the issuer card. http(s) only.',
  },
  {
    name: 'icon',
    label: 'Icon',
    type: 'text',
    help: 'A single emoji, used when no logo is set or the logo fails to load.',
  },
]

const QUALIFICATION_FIELDS: FieldSpec[] = [
  { name: 'degree', label: 'Qualification', type: 'text', required: true, inTable: true },
  { name: 'institution', label: 'Institution', type: 'text', required: true, inTable: true },
  {
    name: 'yearOfPassing',
    label: 'Year of passing',
    type: 'number',
    inTable: true,
    numeric: true,
    help: 'The most recent award is the one a credential describes.',
  },
  { name: 'grade', label: 'Grade or class', type: 'text', inTable: true },
  { name: 'enrolmentNumber', label: 'Enrolment number', type: 'text' },
]

const LAND_FIELDS: FieldSpec[] = [
  {
    name: 'landRecordRef',
    label: 'Land record reference',
    type: 'text',
    required: true,
    inTable: true,
    help: 'Survey or khasra number in the authoritative land record system.',
  },
  { name: 'farmLocation', label: 'Farm location', type: 'text', inTable: true },
  {
    name: 'landAreaAcres',
    label: 'Land area (acres)',
    type: 'number',
    required: true,
    inTable: true,
    numeric: true,
  },
  {
    name: 'ownershipType',
    label: 'Ownership type',
    type: 'select',
    options: ['Owned', 'Leased', 'Sharecropped', 'Inherited'],
    inTable: true,
  },
  { name: 'surveyedOn', label: 'Surveyed on', type: 'date' },
]

const CROP_FIELDS: FieldSpec[] = [
  { name: 'cropName', label: 'Crop', type: 'text', required: true, inTable: true },
  {
    name: 'season',
    label: 'Season',
    type: 'select',
    options: ['Kharif', 'Rabi', 'Zaid'],
    required: true,
    inTable: true,
  },
  { name: 'year', label: 'Year', type: 'number', required: true, inTable: true, numeric: true },
  { name: 'areaSownAcres', label: 'Area sown (acres)', type: 'number', inTable: true, numeric: true },
  {
    name: 'expectedYieldQuintals',
    label: 'Expected yield (quintals)',
    type: 'number',
    numeric: true,
  },
  {
    name: 'landRecordRef',
    label: 'Land record reference',
    type: 'text',
    help: 'Optional — which parcel this cycle was sown on.',
  },
]

const SEED_FIELDS: FieldSpec[] = [
  { name: 'seedType', label: 'Seed type', type: 'text', required: true, inTable: true },
  { name: 'variety', label: 'Variety', type: 'text', inTable: true },
  {
    name: 'quantityKg',
    label: 'Quantity (kg)',
    type: 'number',
    required: true,
    inTable: true,
    numeric: true,
  },
  { name: 'issuedOn', label: 'Issued on', type: 'date', required: true, inTable: true },
  { name: 'subsidyScheme', label: 'Subsidy scheme', type: 'text' },
  { name: 'distributionCentre', label: 'Distribution centre', type: 'text' },
]

export const ENTITIES: Record<
  EntityKind,
  { singular: string; plural: string; icon: string; fields: FieldSpec[] }
> = {
  LandParcel: { singular: 'land parcel', plural: 'Land parcels', icon: '🗺', fields: LAND_FIELDS },
  Crop: { singular: 'crop cycle', plural: 'Crops', icon: '🌾', fields: CROP_FIELDS },
  SeedDistribution: {
    singular: 'seed distribution',
    plural: 'Seeds',
    icon: '🌱',
    fields: SEED_FIELDS,
  },
  Qualification: {
    singular: 'qualification',
    plural: 'Qualifications',
    icon: '🎓',
    fields: QUALIFICATION_FIELDS,
  },
}

/**
 * The supporting record types an issuer works with, parsed from its
 * `recordEntities` field.
 *
 * Unknown names are dropped rather than rendered as a broken tab: the registry
 * may hold entities this portal has no form metadata for, and an issuer naming
 * one should degrade to "no tab" instead of an empty crash.
 */
export function issuerEntities(recordEntities?: string): EntityKind[] {
  return (recordEntities ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter((s): s is EntityKind => s in ENTITIES)
}

/** Blank record for a new row, with the owning farmer already filled in. */
export function blankRecord(kind: EntityKind, farmerId: string): Record<string, unknown> {
  const out: Record<string, unknown> = { farmerId }
  for (const f of ENTITIES[kind].fields) out[f.name] = f.type === 'number' ? '' : ''
  return out
}

/**
 * Client-side required/format check, so an obviously incomplete form doesn't
 * cost a round trip. The registry re-validates regardless — this is a
 * convenience, never the enforcement point.
 */
export function validate(fields: FieldSpec[], rec: Record<string, unknown>): Record<string, string> {
  const errors: Record<string, string> = {}
  for (const f of fields) {
    const v = rec[f.name]
    const blank = v === undefined || v === null || String(v).trim() === ''
    if (f.required && blank) {
      errors[f.name] = `${f.label} is required`
      continue
    }
    if (!blank && f.type === 'number' && Number.isNaN(Number(v))) {
      errors[f.name] = 'Must be a number'
    }
  }
  return errors
}

/** Strips blanks and coerces numbers, so the registry gets typed JSON. */
export function toPayload(
  fields: FieldSpec[],
  rec: Record<string, unknown>,
): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(rec)) {
    if (v === undefined || v === null || String(v).trim() === '') continue
    const spec = fields.find((f) => f.name === k)
    out[k] = spec?.type === 'number' ? Number(v) : v
  }
  return out
}
