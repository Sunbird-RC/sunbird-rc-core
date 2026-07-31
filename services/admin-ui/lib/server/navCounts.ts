import 'server-only'
import { listRegistrySchemas } from './clients/registryClient'
import { listTemplates, getOid4vciConfigs } from './clients/credentialSchemaClient'
import { fetchSwagger } from './swagger/fetchSwagger'
import { parseEntitySchema } from './swagger/parseEntitySchema'
import { getEntriesSince } from './inspector'

// Most of the design's sidebar counts aren't actually obtainable — be
// honest rather than fabricate. Feasible: entities (count of *types* from
// the cached swagger, not record count — that's what the design's "4"
// means), registry schemas, render templates (both plain unfiltered
// lists), OID4VCI offers (session count from the ring buffer, matches the
// design's starting "0"), schemas (published-only via oid4vci-configs).
// Deliberately NOT attempted: DIDs (identity-service has no list
// endpoint), policies/claims (both per-entityType and 404 wholesale when
// CLAIMS_ENABLED=false — a global count would need N calls and would
// invent a number), credentials (no safe empty-filter probe verified).
const CACHE_TTL_MS = 30_000
const CACHE_KEY = Symbol.for('admin-ui.navCountsCache')
type Cache = { at: number; counts: Record<string, number | null> } | undefined

function withTimeout<T>(p: Promise<T>, ms = 1500): Promise<T> {
  return Promise.race([p, new Promise<T>((_, reject) => setTimeout(() => reject(new Error('timeout')), ms))])
}

async function safeCount(p: Promise<{ length: number }>): Promise<number | null> {
  try {
    return (await withTimeout(p)).length
  } catch {
    return null
  }
}

export async function getNavCounts(): Promise<Record<string, number | null>> {
  const g = globalThis as unknown as Record<symbol, Cache>
  const cached = g[CACHE_KEY]
  if (cached && Date.now() - cached.at < CACHE_TTL_MS) return cached.counts

  const [entities, registrySchemas, templates, schemas] = await Promise.allSettled([
    withTimeout(fetchSwagger().then(parseEntitySchema)),
    safeCount(listRegistrySchemas()),
    safeCount(listTemplates()),
    safeCount(getOid4vciConfigs()),
  ])

  const offers = getEntriesSince(0).filter((e) => e.method === 'POST' && /\/oid4vc\/offer$/.test(e.url)).length

  const counts: Record<string, number | null> = {
    entities: entities.status === 'fulfilled' ? entities.value.length : null,
    'registry-schemas': registrySchemas.status === 'fulfilled' ? registrySchemas.value : null,
    templates: templates.status === 'fulfilled' ? templates.value : null,
    schemas: schemas.status === 'fulfilled' ? schemas.value : null,
    offers,
    // Explicitly no data — Sidebar must render no badge for these, not 0.
    dids: null,
    policies: null,
    claims: null,
    credentials: null,
  }

  g[CACHE_KEY] = { at: Date.now(), counts }
  return counts
}
