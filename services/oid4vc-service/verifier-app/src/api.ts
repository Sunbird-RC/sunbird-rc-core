import { BASE } from './config'
import type { CredentialType } from './types'

export type VerifierStatus = {
  status: 'pending' | 'verified' | 'failed'
  verified?: boolean
  checks?: Record<string, string>
  claims?: Record<string, Record<string, unknown>>
  holderDid?: string
  error?: string
}

export type VpRequest = {
  transaction_id: string
  request_uri: string
  qr_data: string
}

const origin = () => BASE || location.origin

async function json<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, init)
  if (!res.ok) throw new Error(`${res.status} — ${(await res.text()).slice(0, 300)}`)
  return res.json() as Promise<T>
}

/** Mirrors oid4vc-service's vct.util.ts slugifyVct(). */
function slugify(name: string): string {
  return name
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

type RawConfig = {
  schemaId: string
  name: string
  formats?: string[]
  vct?: string
  author?: string
  schema?: {
    properties?: Record<string, { type?: string; description?: string }>
  }
}

/**
 * Credential types this verifier can actually verify, discovered live rather
 * than hard-coded — a type created through the schema API shows up here on the
 * next page load.
 *
 * Two filters matter:
 *  - `vc+sd-jwt` support, since that is the format this app requests; and
 *  - an author DID that resolves *on this host*. Most existing schemas are
 *    authored by `did:rcw:…` (resolvable only inside identity-service) or by a
 *    `did:web` pointing at a host that no longer serves a DID document; wallets
 *    reject both with `unsupportedDidMethod` / a resolution failure, so offering
 *    them here would only produce confusing failures.
 */
export async function listCredentialTypes(): Promise<{ types: CredentialType[] }> {
  const raw = await json<RawConfig[]>('/credential-schema/oid4vci-configs')
  const host = origin().replace(/^https?:\/\//, '')
  const resolvablePrefix = `did:web:${host}:`

  const sdJwt = raw.filter((c) => (c.formats ?? []).includes('vc+sd-jwt'))
  const usable = sdJwt.filter((c) => (c.author ?? '').startsWith(resolvablePrefix))

  const types = usable.map((c): CredentialType => {
    const props = c.schema?.properties ?? {}
    const rawVct = c.vct || c.name
    return {
      id: c.schemaId,
      name: c.name,
      // Multi-format schemas expose <schemaId>_<format>; single-format use the
      // bare id (oid4vci.service.ts:81).
      configId: (c.formats ?? []).length > 1 ? `${c.schemaId}_vc+sd-jwt` : c.schemaId,
      vct: /^https?:\/\//i.test(rawVct) ? rawVct : `${origin()}/vct/${slugify(rawVct)}`,
      issuer: c.author ?? '',
      attributes: Object.keys(props),
      descriptions: Object.fromEntries(
        Object.entries(props).map(([k, v]) => [k, v.description ?? '']),
      ),
      jsonTypes: Object.fromEntries(Object.entries(props).map(([k, v]) => [k, v.type ?? 'string'])),
    }
  })

  const hidden = sdJwt.length - usable.length
  if (hidden > 0) {
    console.info(
      `[verifier] ${hidden} SD-JWT credential type(s) hidden: issuer DID not resolvable at ${host}.`,
    )
  }

  return { types }
}

/**
 * Creates a presentation request for one credential type.
 *
 * `signed: true` is not optional in practice: a Credo-based wallet fetches the
 * request object as `application/oauth-authz-req+jwt`, and oid4vc-service
 * deliberately refuses to negotiate down to JSON — unsigned answers 406.
 *
 * DCQL spells SD-JWT VC `dc+sd-jwt` while the OID4VCI *credential* format stays
 * `vc+sd-jwt`; the id was renamed mid-spec and wallets enforce the new one.
 * `meta.vct_values` is mandatory, and SD-JWT claim paths are flat — no
 * `credentialSubject` wrapper.
 */
export function createRequest(vct: string, claims: string[]) {
  return json<VpRequest>('/vp/request', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({
      signed: true,
      dcql_query: {
        credentials: [
          {
            id: 'cred',
            format: 'dc+sd-jwt',
            meta: { vct_values: [vct] },
            claims: claims.map((path) => ({ path: [path] })),
          },
        ],
      },
    }),
  })
}

export function getStatus(transactionId: string) {
  return json<VerifierStatus>(`/vp/status/${transactionId}`)
}

/** Plausible values so "issue a sample" works for any discovered type. */
function sampleValue(attr: string, jsonType: string): unknown {
  if (jsonType === 'boolean') return true
  if (jsonType === 'number' || jsonType === 'integer') return 1
  if (/date|dob|birth/i.test(attr)) return '1990-01-01'
  if (/name/i.test(attr)) return 'Asha Devi'
  if (/gender/i.test(attr)) return 'Female'
  if (/id$|_id|Id$/.test(attr)) return 'SAMPLE-000123'
  return `Sample ${attr}`
}

/**
 * Issues a sample credential of the chosen type. Every `required` attribute must
 * be supplied or the credential endpoint fails with an opaque
 * `500 Error issuing credential` (the ajv reason only reaches the service log).
 */
export function createOffer(type: CredentialType) {
  const claims: Record<string, unknown> = {}
  for (const attr of type.attributes) {
    claims[attr] = sampleValue(attr, type.jsonTypes[attr] ?? 'string')
  }
  return json<{ qr_data: string }>('/oid4vc/offer', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({
      credential_configuration_id: type.configId,
      format: 'vc+sd-jwt',
      claims,
    }),
  })
}

/** oid4vc-service's own QR renderer — no client-side QR dependency. */
export const qrSrc = (data: string) => `${BASE}/qr?data=${encodeURIComponent(data)}`
