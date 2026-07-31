import 'server-only'
import { env } from '../env'
import { json } from '../http'

const base = () => env.credentialsBaseUrl

export type CredentialFormat = 'ldp_vc' | 'jwt_vc_json' | 'vc+sd-jwt' | 'mso_mdoc'

export type CredentialSummary = {
  id: string
  subject: string
  issuer?: string
  schema?: string
  format?: CredentialFormat
  status: 'ISSUED' | 'REVOKED' | string
  issuanceDate?: string
}

export async function searchCredentials(query: {
  subject?: string
  issuer?: string
  type?: string
}): Promise<CredentialSummary[]> {
  const res = await json<CredentialSummary[] | { credentials?: CredentialSummary[] }>(`${base()}/credentials/search`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(query),
  })
  return Array.isArray(res) ? res : (res.credentials ?? [])
}

export async function getCredential(id: string): Promise<CredentialSummary & Record<string, unknown>> {
  return json(`${base()}/credentials/${id}`, { headers: { Accept: 'application/json' } })
}

export async function issueCredential(payload: {
  schemaId: string
  format: CredentialFormat
  subject: string
  credentialSubject: Record<string, unknown>
}): Promise<unknown> {
  return json(`${base()}/credentials/issue`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export type VerifyResult = {
  verified: boolean
  checks: { name: string; passed: boolean }[]
}

export async function verifyCredential(id: string): Promise<VerifyResult> {
  return json(`${base()}/credentials/${id}/verify`)
}

// Soft revoke: sets REVOKED status and flips the StatusList bit.
export async function revokeCredential(id: string): Promise<unknown> {
  return json(`${base()}/credentials/${id}`, { method: 'DELETE' })
}
