import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { FlagDisabledState } from '@/components/ui/EmptyState'
import { ClaimsClient } from '@/components/claims/ClaimsClient'
import { getFlags } from '@/lib/server/flags'

export default async function ClaimsPage() {
  const flags = await getFlags()

  return (
    <>
      <Header
        title="Claim inbox"
        subtitle="Grant or deny claims raised against your entities"
        endpoint="POST /api/v1/{entityName}/claims/{claimId}/attest"
      />
      <ScreenBody>
        {flags.claims ? (
          <ClaimsClient />
        ) : (
          <FlagDisabledState
            title="The claim inbox needs CLAIMS_ENABLED=true"
            body="Set the flag on the registry and restart, then claims raised against your entities land here."
          />
        )}
      </ScreenBody>
    </>
  )
}
