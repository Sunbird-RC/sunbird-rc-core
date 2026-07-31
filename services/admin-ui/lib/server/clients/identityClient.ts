import 'server-only'
import { env } from '../env'
import { json } from '../http'

const base = () => env.identityBaseUrl

export type DidMethod = 'did:web' | 'did:key' | 'did:rcw'

export type GenerateDidRequest = {
  content: Array<{
    method: DidMethod
    alsoKnownAs?: string
    keyPairType?: string
    webDidBaseUrl?: string
    id?: string
  }>
}

// identity-service has no list endpoint — DIDs cannot be enumerated
// server-side, only generated and resolved. The UI tracks created/resolved
// DIDs in browser storage, not here.
export async function generateDids(req: GenerateDidRequest): Promise<unknown> {
  return json(`${base()}/did/generate`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(req),
  })
}

export async function resolveDid(did: string): Promise<unknown> {
  return json(`${base()}/did/resolve/${encodeURIComponent(did)}`)
}
