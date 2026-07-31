// Pure, framework-free — safe to unit-test in isolation and to import from
// either server or client code.
//
// Recorded URLs are internal Docker DNS (e.g. http://registry:8081/...),
// which isn't runnable from the operator's machine. Gateway rewriting
// doesn't work generally: nginx/nginx.conf only proxies /registry/,
// /claim-ms/, /oid4vc/, /vp/, /bucket/, /auth/ — credential-schema,
// credentials, and identity have no location at all. A host-port map
// (verified against `docker compose config`'s published ports) is the
// only rewrite that's runnable for every service.
export const DEFAULT_HOST_MAP: Record<string, string> = {
  'http://registry:8081': 'http://localhost:8091', // non-obvious: registry's published port isn't 8081
  'http://credential-schema:3333': 'http://localhost:3333',
  'http://credential:3000': 'http://localhost:3000',
  'http://identity:3332': 'http://localhost:3332',
  'http://claim-ms:8082': 'http://localhost:8082',
  'http://oid4vc-service:3400': 'http://localhost:3400',
}

// Parses `INSPECTOR_PUBLIC_BASES` as `internal=public,internal=public,...`
// and merges over the defaults.
export function parseHostMapOverride(raw: string | undefined): Record<string, string> {
  if (!raw) return {}
  const out: Record<string, string> = {}
  for (const pair of raw.split(',')) {
    const [from, to] = pair.split('=').map((s) => s.trim())
    if (from && to) out[from] = to
  }
  return out
}

export function rewriteUrl(url: string, hostMap: Record<string, string> = DEFAULT_HOST_MAP): string {
  for (const [internal, external] of Object.entries(hostMap)) {
    if (url.startsWith(internal)) return external + url.slice(internal.length)
  }
  return url
}

function shellEscape(value: string): string {
  // Single-quote the whole value, escaping any embedded single quotes as '\''
  return `'${value.replace(/'/g, `'\\''`)}'`
}

export function buildCurl(
  entry: { method: string; url: string; requestBody?: string },
  hostMap: Record<string, string> = DEFAULT_HOST_MAP,
): string {
  const url = rewriteUrl(entry.url, hostMap)
  const parts = ['curl', '-i', '-X', entry.method, shellEscape(url)]
  if (entry.requestBody) {
    parts.push('-H', shellEscape('content-type: application/json'))
    parts.push('-H', shellEscape('Authorization: Bearer $TOKEN'))
    parts.push('--data-raw', shellEscape(entry.requestBody))
  }
  return parts.join(' ')
}
