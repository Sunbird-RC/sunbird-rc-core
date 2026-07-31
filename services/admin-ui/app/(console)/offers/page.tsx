import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { OffersClient } from '@/components/offers/OffersClient'
import { FlagDisabledState } from '@/components/ui/EmptyState'
import { getFlags } from '@/lib/server/flags'
import { getOid4vciConfigs } from '@/lib/server/clients/credentialSchemaClient'

export default async function OffersPage() {
  const flags = await getFlags()

  return (
    <>
      <Header title="OID4VCI offers" subtitle="Mint a pre-authorized credential offer and QR" endpoint="POST /oid4vc/offer" />
      <ScreenBody>
        {flags.oid4vcReachable ? (
          <OffersInner />
        ) : (
          <FlagDisabledState
            title="oid4vc-service is not running"
            body='It is gated by the compose profile "oid4vc", not an env flag — start it with `docker compose --profile oid4vc up` to use this screen.'
          />
        )}
      </ScreenBody>
    </>
  )
}

async function OffersInner() {
  const configs = (await getOid4vciConfigs().catch(() => [])) as { schemaId: string; name: string }[]
  return <OffersClient configs={configs} />
}
