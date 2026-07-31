import 'server-only'
import { env } from '../env'
import { json } from '../http'

const base = () => env.credentialSchemaBaseUrl

export type SchemaStatus = 'DRAFT' | 'PUBLISHED' | 'DEPRECATED' | 'REVOKED'

export type CredentialSchema = {
  id: string
  version: string
  name: string
  did?: string
  tags?: string[]
  status: SchemaStatus
  schema?: unknown
  deprecatedId?: string
  updatedAt?: string
}

// GET /credential-schema 500s without `tags` — schema.controller.ts calls
// tags.split(',') unguarded. The UI must always send a tag; there is no
// "browse all" call. page/limit match the Postman collection's convention
// for this service (`?tags=Student&page=1&limit=10`).
export async function listSchemasByTag(tag: string, page = 1, limit = 10): Promise<CredentialSchema[]> {
  const res = await json<CredentialSchema[] | { schemas?: CredentialSchema[] }>(
    `${base()}/credential-schema?tags=${encodeURIComponent(tag)}&page=${page}&limit=${limit}`,
  )
  return Array.isArray(res) ? res : (res.schemas ?? [])
}

export async function getSchemaVersions(id: string): Promise<CredentialSchema[]> {
  const res = await json<CredentialSchema[] | { versions?: CredentialSchema[] }>(`${base()}/credential-schema/${id}`)
  return Array.isArray(res) ? res : (res.versions ?? [])
}

export async function getSchemaVersion(id: string, version: string): Promise<CredentialSchema> {
  return json(`${base()}/credential-schema/${id}/${version}`)
}

export async function createSchema(payload: Record<string, unknown>): Promise<unknown> {
  return json(`${base()}/credential-schema`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

// PUT .../:id/:ver does NOT mutate the row — it creates a new version
// (minor+1/patch+1/major+1). The old row is not auto-deprecated; callers
// must explicitly hit the deprecate lifecycle action too.
export async function saveSchemaVersion(id: string, version: string, payload: Record<string, unknown>): Promise<unknown> {
  return json(`${base()}/credential-schema/${id}/${version}`, {
    method: 'PUT',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export type LifecycleAction = 'publish' | 'deprecate' | 'revoke'

export async function transitionSchema(action: LifecycleAction, id: string, version: string): Promise<unknown> {
  return json(`${base()}/credential-schema/${action}/${id}/${version}`, { method: 'PUT' })
}

export async function getOid4vciConfigs(): Promise<unknown[]> {
  const res = await json<unknown[] | { configs?: unknown[] }>(`${base()}/credential-schema/oid4vci-configs`)
  return Array.isArray(res) ? res : (res as { configs?: unknown[] }).configs ?? []
}

export type RenderTemplate = {
  id: string
  schemaId: string
  schemaVersion?: string
  templateUrl?: string
  kind?: string
  updatedAt?: string
}

export async function listTemplates(schemaId?: string): Promise<RenderTemplate[]> {
  const qs = schemaId ? `?schemaId=${encodeURIComponent(schemaId)}` : ''
  const res = await json<RenderTemplate[] | { templates?: RenderTemplate[] }>(`${base()}/template${qs}`)
  return Array.isArray(res) ? res : (res.templates ?? [])
}

export async function getTemplate(id: string): Promise<RenderTemplate & { content?: string }> {
  return json(`${base()}/template/${id}`)
}

export async function createTemplate(payload: Record<string, unknown>): Promise<unknown> {
  return json(`${base()}/template`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export async function deleteTemplate(id: string): Promise<unknown> {
  return json(`${base()}/template/${id}`, { method: 'DELETE' })
}
