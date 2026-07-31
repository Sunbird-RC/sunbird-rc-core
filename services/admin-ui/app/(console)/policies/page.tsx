import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { FlagDisabledState } from '@/components/ui/EmptyState'
import { PoliciesClient } from '@/components/policies/PoliciesClient'
import { getFlags } from '@/lib/server/flags'

export default async function PoliciesPage() {
  const flags = await getFlags()

  return (
    <>
      <Header
        title="Attestation policies"
        subtitle="Names the property, attestor entity, and condition an attestation must satisfy"
        endpoint="POST /api/v1/{entityName}/attestationPolicy"
      />
      <ScreenBody>
        {flags.claims ? (
          <PoliciesClient />
        ) : (
          <FlagDisabledState
            title="Attestation policies are switched off"
            body="This screen needs CLAIMS_ENABLED=true on the registry. It defaults to false, so the underlying routes don't exist (404) until it's flipped."
          />
        )}
      </ScreenBody>
    </>
  )
}
