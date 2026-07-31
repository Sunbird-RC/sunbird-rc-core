import 'server-only'
import { recordCall } from './inspector'

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: string,
  ) {
    super(`${status} — ${body.slice(0, 300)}`)
  }
}

function logRequest(method: string, url: string, status: number, ms: number) {
  // Structured, one line per backend call — deliberately not a logging
  // framework: this runs inside a container whose stdout is already
  // collected by the platform, so plain JSON lines are enough.
  console.log(JSON.stringify({ at: 'admin-ui.http', method, url, status, ms }))
}

// Folds HTTP status + response body into one Error, the same pattern used in
// oid4vc-service/verifier-app/src/api.ts — the best error-surfacing idiom
// already in this repo, ported here for the server-side proxy layer.
export async function json<T>(url: string, init?: RequestInit): Promise<T> {
  const start = Date.now()
  const method = init?.method ?? 'GET'
  try {
    const res = await fetch(url, { ...init, cache: 'no-store' })
    const text = await res.text()
    const ms = Date.now() - start
    logRequest(method, url, res.status, ms)
    recordCall({ method, url, status: res.status, ms, requestBody: init?.body, responseBody: text })
    if (!res.ok) throw new ApiError(res.status, text)
    return text ? (JSON.parse(text) as T) : (undefined as T)
  } catch (e) {
    if (e instanceof ApiError) throw e
    // Transport failure (ECONNREFUSED, timeout, DNS) — never reached the
    // server, so record status 0. This is routine here (oid4vc-service is
    // compose-profile-gated and usually down) and is most of the
    // inspector's value: without this branch these calls vanish silently.
    const ms = Date.now() - start
    const message = e instanceof Error ? e.message : String(e)
    logRequest(method, url, 0, ms)
    recordCall({ method, url, status: 0, ms, requestBody: init?.body, error: message })
    throw e
  }
}

export async function raw(url: string, init?: RequestInit): Promise<Response> {
  const start = Date.now()
  const method = init?.method ?? 'GET'
  try {
    const res = await fetch(url, { ...init, cache: 'no-store' })
    const ms = Date.now() - start
    logRequest(method, url, res.status, ms)
    if (!res.ok) {
      const text = await res.text()
      recordCall({ method, url, status: res.status, ms, requestBody: init?.body, responseBody: text })
      throw new ApiError(res.status, text)
    }
    // Success path is a stream (QR/document downloads) — don't consume it.
    recordCall({ method, url, status: res.status, ms, requestBody: init?.body, responseBody: '<binary/streamed>' })
    return res
  } catch (e) {
    if (e instanceof ApiError) throw e
    const ms = Date.now() - start
    const message = e instanceof Error ? e.message : String(e)
    logRequest(method, url, 0, ms)
    recordCall({ method, url, status: 0, ms, requestBody: init?.body, error: message })
    throw e
  }
}

// Tier-2 (Phase 7, optional): forward the operator's Keycloak access token
// as a bearer header once an operator flips AUTHENTICATION_ENABLED/
// ENABLE_AUTH true backend-side. A one-line addition per client call site —
// `headers: authHeader(session)` merged into the existing headers object —
// not a new subsystem, because the session already lives server-side
// (see session.ts). credentials-service has no auth guard at all today
// regardless of ENABLE_AUTH, so forwarding a token there doesn't protect it
// until that gap is fixed upstream; this is a backend prerequisite, not
// something admin-ui can work around.
export function authHeader(accessToken?: string): Record<string, string> {
  return accessToken ? { Authorization: `Bearer ${accessToken}` } : {}
}
