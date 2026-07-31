import 'server-only'

// Records every backend call made through http.ts's json()/raw() — the
// single choke point all 6 clients funnel through — so the Request
// Inspector drawer sees both Server-Component and Route-Handler traffic.
//
// Stored on globalThis, not a module-level `let`: Next 14 bundles Server
// Components under the `react-server` condition and Route Handlers under
// the node/route layer, so this module can be instantiated twice in one
// process. A plain module variable would give two disjoint buffers — the
// drawer would read one and never see the other half of all traffic.
const KEY = Symbol.for('admin-ui.inspector')

export type InspectorEntry = {
  seq: number
  at: string
  method: string
  url: string
  status: number // 0 = transport failure (e.g. ECONNREFUSED), never reached the server
  ms: number
  requestBody?: string
  responseBody?: string
  error?: string
}

type Store = { entries: InspectorEntry[]; seq: number }

function store(): Store {
  const g = globalThis as unknown as Record<symbol, Store>
  if (!g[KEY]) g[KEY] = { entries: [], seq: 0 }
  return g[KEY]
}

const MAX_ENTRIES = 64
const MAX_BODY_BYTES = 8 * 1024

function truncate(body: string | undefined): string | undefined {
  if (body === undefined) return undefined
  const bytes = Buffer.byteLength(body, 'utf8')
  if (bytes <= MAX_BODY_BYTES) return body
  return `${body.slice(0, MAX_BODY_BYTES)}…[truncated ${bytes - MAX_BODY_BYTES} bytes]`
}

// Request headers (which may carry Authorization/Cookie) are never stored
// at all — the drawer only ever needs method/url/status/bodies, so there's
// nothing to redact after the fact. Simpler and safer than store-then-scrub.
export function recordCall(entry: {
  method: string
  url: string
  status: number
  ms: number
  requestBody?: unknown
  responseBody?: string
  error?: string
}) {
  const s = store()
  s.seq += 1
  s.entries.push({
    seq: s.seq,
    at: new Date(Date.now() - entry.ms).toISOString(),
    method: entry.method,
    url: entry.url,
    status: entry.status,
    ms: entry.ms,
    requestBody: truncate(
      entry.requestBody === undefined
        ? undefined
        : typeof entry.requestBody === 'string'
          ? entry.requestBody
          : JSON.stringify(entry.requestBody),
    ),
    responseBody: truncate(entry.responseBody),
    error: entry.error,
  })
  if (s.entries.length > MAX_ENTRIES) s.entries.shift()
}

export function getEntriesSince(seq: number): InspectorEntry[] {
  return store().entries.filter((e) => e.seq > seq)
}
