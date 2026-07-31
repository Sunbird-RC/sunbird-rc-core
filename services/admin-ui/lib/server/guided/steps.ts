import 'server-only'
import { env } from '../env'
import { listRegistrySchemas } from '../clients/registryClient'
import { getOid4vciConfigs } from '../clients/credentialSchemaClient'
import { getEntriesSince } from '../inspector'

export type StepStatus = 'done' | 'ready' | 'blocked' | 'unknown' | 'info'

export type GuidedStep = {
  id: string
  title: string
  endpointHint: string
  status: StepStatus
  detail: string
}

function withTimeout<T>(p: Promise<T>, ms = 2000): Promise<T> {
  return Promise.race([p, new Promise<T>((_, reject) => setTimeout(() => reject(new Error('timeout')), ms))])
}

// Ring-buffer-derived: offers/verify/revoke have no queryable state
// (offers are ephemeral, verify/revoke don't persist a "was this ever done"
// flag), but the inspector already witnessed every call this console made —
// this is why these steps only become accurate once the Inspector (Pass
// 2.1) has landed.
function sawSuccessfulCall(pattern: RegExp, method?: string): boolean {
  return getEntriesSince(0).some(
    (e) => (!method || e.method === method) && pattern.test(e.url) && e.status >= 200 && e.status < 300,
  )
}

const CACHE_TTL_MS = 10_000
const CACHE_KEY = Symbol.for('admin-ui.guidedStateCache')

type Cache = { at: number; steps: GuidedStep[] } | undefined

export async function getGuidedState(oid4vcReachable: boolean): Promise<GuidedStep[]> {
  const g = globalThis as unknown as Record<symbol, Cache>
  const cached = g[CACHE_KEY]
  if (cached && Date.now() - cached.at < CACHE_TTL_MS) return cached.steps

  const [registrySchemasResult, oid4vciConfigsResult] = await Promise.allSettled([
    withTimeout(listRegistrySchemas()),
    withTimeout(getOid4vciConfigs()),
  ])

  const registrySchemas = registrySchemasResult.status === 'fulfilled' ? registrySchemasResult.value : null
  const oid4vciConfigs = oid4vciConfigsResult.status === 'fulfilled' ? oid4vciConfigsResult.value : null

  const anySchema = (registrySchemas?.length ?? 0) > 0
  const anyPublished = (registrySchemas ?? []).some((s) => s.status === 'PUBLISHED')
  const anyOid4vciEnabled = (oid4vciConfigs?.length ?? 0) > 0

  const steps: GuidedStep[] = [
    {
      id: 'registry-schema-created',
      title: 'Create a registry schema',
      endpointHint: 'POST /api/v1/Schema',
      status: registrySchemasResult.status === 'rejected' ? 'unknown' : anySchema ? 'done' : 'ready',
      detail:
        registrySchemasResult.status === 'rejected'
          ? 'Could not reach the registry.'
          : anySchema
            ? `${registrySchemas!.length} registry schema(s) found.`
            : 'No registry schemas yet — create one on the Registry schemas screen.',
    },
    {
      id: 'registry-schema-published',
      title: 'Publish the registry schema',
      endpointHint: 'PUT /api/v1/Schema/{osid}',
      status: registrySchemasResult.status === 'rejected' ? 'unknown' : anyPublished ? 'done' : anySchema ? 'ready' : 'blocked',
      detail: env.flags.signature
        ? 'Publishing runs ensureCredentialSchema()/saveIdFormat() (signature.enabled is on).'
        : 'Publishing runs ensureCredentialSchema()/saveIdFormat() — both are no-ops today since signature.enabled and idgen.enabled are off.',
    },
    {
      id: 'credential-schema-published',
      title: 'Publish a credential schema for OID4VCI',
      endpointHint: 'GET /credential-schema/oid4vci-configs',
      status: oid4vciConfigsResult.status === 'rejected' ? 'unknown' : anyOid4vciEnabled ? 'done' : 'ready',
      detail:
        oid4vciConfigsResult.status === 'rejected'
          ? 'Could not reach credential-schema.'
          : anyOid4vciEnabled
            ? `${oid4vciConfigs!.length} OID4VCI-enabled schema(s) published.`
            : 'No published, OID4VCI-enabled schema yet — create and publish one on the Schemas screen.',
    },
    {
      id: 'offer-created',
      title: 'Create an OID4VCI offer',
      endpointHint: 'POST /oid4vc/offer',
      status: !oid4vcReachable ? 'blocked' : sawSuccessfulCall(/\/oid4vc\/offer$/, 'POST') ? 'done' : 'ready',
      detail: !oid4vcReachable
        ? 'oid4vc-service is not running (compose profile "oid4vc").'
        : 'Mint an offer on the OID4VCI offers screen — this console has not seen one yet.',
    },
    {
      id: 'wallet-issuance',
      title: 'Wallet scans the offer and completes issuance',
      endpointHint: '(external — scan the QR with a wallet)',
      status: 'info',
      detail: 'Not probeable from this console — informational step only.',
    },
    {
      id: 'credential-verified',
      title: 'Verify the issued credential',
      endpointHint: 'GET /credentials/{id}/verify',
      status: sawSuccessfulCall(/\/credentials\/[^/]+\/verify$/, 'GET') ? 'done' : 'ready',
      detail: 'Run verify from a credential’s detail drawer on the Credentials screen.',
    },
    {
      id: 'credential-revoked',
      title: 'Revoke the credential',
      endpointHint: 'DELETE /credentials/{id}',
      status: sawSuccessfulCall(/\/credentials\/[^/]+$/, 'DELETE') ? 'done' : 'ready',
      detail: 'Revoke sets status to REVOKED and flips the StatusList2021 bit.',
    },
  ]

  g[CACHE_KEY] = { at: Date.now(), steps }
  return steps
}
