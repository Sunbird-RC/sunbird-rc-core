import 'server-only'
import { env } from '../env'

export type SwaggerDoc = {
  paths: Record<string, unknown>
  definitions: Record<string, unknown>
}

// GET /api/docs/swagger.json's controller returns
// `ResponseEntity<Object>(objectMapper.writeValueAsString(apiDoc))` — the
// body is a JSON-encoded STRING, not the object directly, depending on
// Spring's content negotiation. Handle both shapes defensively rather than
// assuming one.
export async function fetchSwagger(): Promise<SwaggerDoc> {
  const res = await fetch(`${env.registryBaseUrl}/api/docs/swagger.json`, { cache: 'no-store' })
  if (!res.ok) throw new Error(`swagger.json fetch failed: ${res.status}`)
  const text = await res.text()
  let parsed: unknown = JSON.parse(text)
  if (typeof parsed === 'string') parsed = JSON.parse(parsed)
  return parsed as SwaggerDoc
}
