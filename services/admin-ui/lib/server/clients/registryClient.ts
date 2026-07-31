import 'server-only'
import { env } from '../env'
import { json, ApiError } from '../http'
import { parseResponseEnvelope, parseSearchEnvelope } from '../envelope'

const base = () => env.registryBaseUrl

export type AuditEvent = {
  eventId: string
  timestamp: string
  userId?: string
  action: string
  entityType?: string
  recordId?: string
  ip?: string
}

// The real path is POST /audit (verified live: /audit/search 404s, /audit
// 200s) — RegistryUtilsController.java:280-313. The response is the generic
// `Response` envelope, and inside `result` there's ANOTHER search envelope
// keyed by entity type: { result: { <entityType>: { totalCount, data } } }.
// When no entityType filter is given the registry keys it under "Audit".
export async function searchAudit(filters: Record<string, unknown> = {}): Promise<AuditEvent[]> {
  try {
    const res = await json<unknown>(`${base()}/audit`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(filters),
    })
    const result = parseResponseEnvelope<Record<string, unknown>>(res, {})
    const nested = Object.values(result)[0]
    return parseSearchEnvelope<AuditEvent>(nested).items
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return []
    throw e
  }
}

export type RegistrySchema = {
  osid?: string
  name: string
  status: 'DRAFT' | 'PUBLISHED'
  schema?: string
  _osConfig?: { roles?: string[] }
}

// The registry's own Schema entity — POST/GET/PUT /api/v1/Schema, upstream
// of and distinct from credential-schema. Publishing runs
// ensureCredentialSchema()/saveIdFormat(), both no-ops unless
// signature.enabled/idgen.enabled (both false by default).
//
// Uses POST /search (not GET /api/v1/Schema) — the generic controller 404s
// GET when the result set is empty (RegistryEntityController.java:705-708),
// while /search returns 200 with an empty {totalCount:0,data:[]} envelope.
export async function listRegistrySchemas(): Promise<RegistrySchema[]> {
  const res = await json<unknown>(`${base()}/api/v1/Schema/search`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ filters: {}, limit: 100, offset: 0 }),
  })
  return parseSearchEnvelope<RegistrySchema>(res).items
}

export async function getRegistrySchema(osid: string): Promise<RegistrySchema> {
  return json<RegistrySchema>(`${base()}/api/v1/Schema/${osid}`)
}

export async function createRegistrySchema(schema: Omit<RegistrySchema, 'osid'>): Promise<unknown> {
  return json(`${base()}/api/v1/Schema?mode=sync`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(schema),
  })
}

export async function publishRegistrySchema(osid: string, current: RegistrySchema): Promise<unknown> {
  return json(`${base()}/api/v1/Schema/${osid}`, {
    method: 'PUT',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ ...current, status: 'PUBLISHED' }),
  })
}

export type AttestationPolicy = {
  osid?: string
  name: string
  attestorEntity: string
  conditions: string
  attestationProperties?: Record<string, string>
  status: 'DRAFT' | 'PUBLISHED'
}

// @ConditionalOnProperty on claims.enabled — when the flag is off, these
// routes don't exist at all (404), not just "return empty". Uses the
// generic `Response` envelope (result = policy list), not the search
// envelope — RegistryAttestationPolicyController.java:52-77.
export async function listPolicies(entityType: string): Promise<AttestationPolicy[]> {
  const res = await json<unknown>(`${base()}/api/v1/${entityType}/attestationPolicies`)
  return parseResponseEnvelope<AttestationPolicy[]>(res, [])
}

export async function createPolicy(entityType: string, policy: Omit<AttestationPolicy, 'osid'>): Promise<unknown> {
  return json(`${base()}/api/v1/${entityType}/attestationPolicy`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(policy),
  })
}

export async function togglePolicyStatus(
  entityType: string,
  policyId: string,
  status: 'DRAFT' | 'PUBLISHED',
): Promise<unknown> {
  return json(`${base()}/api/v1/${entityType}/attestationPolicy/${policyId}/${status}`, { method: 'PUT' })
}

export async function deletePolicy(entityType: string, policyId: string): Promise<unknown> {
  return json(`${base()}/api/v1/${entityType}/attestationPolicy/${policyId}`, { method: 'DELETE' })
}

export type Claim = {
  id: string
  subject?: string
  entityId?: string
  property?: string
  attestorEntity?: string
  status: 'OPEN' | 'GRANTED' | 'DENIED' | string
  raisedAt?: string
  notes?: string
}

// Always via this proxy — never claim-ms's POST /api/v1/getClaims directly.
// The registry derives attestorInfo from the caller's token; claim-ms
// expects it in the request body instead. This endpoint takes a Spring
// `Pageable` (page/size query params, 0-indexed), not a request body —
// RegistryClaimsController.java:52-64. The response is claim-ms's own
// unwrapped shape, which varies by version, so parse defensively.
export async function listClaims(entityType: string): Promise<Claim[]> {
  const res = await json<unknown>(`${base()}/api/v1/${entityType}/claims?page=0&size=20`)
  if (Array.isArray(res)) return res as Claim[]
  const obj = res as { content?: Claim[]; data?: Claim[] } | undefined
  return obj?.content ?? obj?.data ?? []
}

export async function getClaim(entityType: string, claimId: string): Promise<Claim> {
  return json(`${base()}/api/v1/${entityType}/claims/${claimId}`)
}

export async function attestClaim(
  entityType: string,
  claimId: string,
  action: 'GRANTED' | 'DENIED',
  notes: string,
): Promise<unknown> {
  return json(`${base()}/api/v1/${entityType}/claims/${claimId}/attest`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ action, notes }),
  })
}

export type DocumentEntry = {
  id: string
  name: string
  size?: number
  uploadedAt?: string
}

export async function listDocuments(entityType: string, entityId: string, property: string): Promise<DocumentEntry[]> {
  const res = await json<unknown>(`${base()}/api/v1/${entityType}/${entityId}/${property}/documents`)
  if (Array.isArray(res)) return res as DocumentEntry[]
  const obj = res as { documents?: DocumentEntry[]; data?: DocumentEntry[] } | undefined
  return obj?.documents ?? obj?.data ?? []
}

export async function deleteDocument(
  entityType: string,
  entityId: string,
  property: string,
  docId: string,
): Promise<unknown> {
  return json(`${base()}/api/v1/${entityType}/${entityId}/${property}/documents/${docId}`, { method: 'DELETE' })
}

export async function sendInvite(
  entityType: string,
  invite: { name: string; email: string; role: string },
): Promise<unknown> {
  return json(`${base()}/api/v1/${entityType}/invite`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(invite),
  })
}
