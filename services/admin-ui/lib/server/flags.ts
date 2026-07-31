import 'server-only'
import { env } from './env'
import { ApiError, json } from './http'

export type Flags = typeof env.flags & {
  oid4vcReachable: boolean
  swaggerReachable: boolean
}

// oid4vc-service is gated by a compose *profile* (`profiles: ["oid4vc"]`),
// not an env boolean — a plain `docker compose up` never starts it. And
// api-swagger.enabled defaults true but isn't wired through any compose
// override, so it's verify-don't-assume too. Both need a live probe instead
// of an env read, unlike the five real feature flags in env.ts.
export async function getFlags(): Promise<Flags> {
  const [oid4vcReachable, swaggerReachable] = await Promise.all([
    probe(`${env.oid4vcBaseUrl}/health`),
    probe(`${env.registryBaseUrl}/api/docs/swagger.json`),
  ])

  return { ...env.flags, oid4vcReachable, swaggerReachable }
}

async function probe(url: string): Promise<boolean> {
  try {
    await json(url)
    return true
  } catch (e) {
    if (e instanceof ApiError) return false
    return false
  }
}
