import 'server-only'
import { env } from '../env'
import { authHeader, json } from '../http'
import { getSession } from '../session'
import { parseSearchEnvelope } from '../envelope'

const base = () => env.registryBaseUrl

// Tier-2 bearer forwarding (Phase 7, optional): once AUTHENTICATION_ENABLED
// is flipped true on the registry, forward the operator's own Keycloak
// access token instead of calling anonymously. Harmless no-op while the
// flag is off — the header is simply empty. See http.ts's authHeader() note
// for why this is a one-line pattern rather than a new subsystem, and why
// it's demonstrated here (registry) rather than on credentials-service
// (which has no auth guard at all today regardless of this token).
async function bearer(): Promise<Record<string, string>> {
  if (!env.flags.authentication) return {}
  const session = await getSession()
  return authHeader(session?.accessToken)
}

// Response is a search envelope {totalCount, data}, NOT keyed by entity
// type — reading `res[entityType]` (the old bug) always came back
// undefined, so screens rendered empty even when the registry had data.
// Verified live: POST .../search on an empty result set returns
// {"totalCount":0,"data":[]}, HTTP 200.
export async function searchEntities(
  entityType: string,
  filters: Record<string, unknown> = {},
  limit = 100,
  offset = 0,
): Promise<{ items: Record<string, unknown>[]; totalCount: number }> {
  const res = await json<unknown>(`${base()}/api/v1/${entityType}/search`, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(await bearer()) },
    body: JSON.stringify({ filters, limit, offset }),
  })
  return parseSearchEnvelope<Record<string, unknown>>(res)
}

export async function getEntity(entityType: string, entityId: string): Promise<Record<string, unknown>> {
  return json(`${base()}/api/v1/${entityType}/${entityId}`, { headers: await bearer() })
}

export async function createEntity(entityType: string, payload: Record<string, unknown>): Promise<unknown> {
  return json(`${base()}/api/v1/${entityType}?mode=sync`, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(await bearer()) },
    body: JSON.stringify(payload),
  })
}

export async function updateEntity(
  entityType: string,
  entityId: string,
  payload: Record<string, unknown>,
): Promise<unknown> {
  return json(`${base()}/api/v1/${entityType}/${entityId}`, {
    method: 'PUT',
    headers: { 'content-type': 'application/json', ...(await bearer()) },
    body: JSON.stringify(payload),
  })
}

// Not documented in the generated swagger.json (the generator never emits a
// DELETE operation), but the generic controller genuinely supports it —
// verified directly against RegistryEntityController.java.
export async function deleteEntity(entityType: string, entityId: string): Promise<unknown> {
  return json(`${base()}/api/v1/${entityType}/${entityId}`, { method: 'DELETE', headers: await bearer() })
}
