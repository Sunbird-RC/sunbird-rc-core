import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { InvitesClient } from '@/components/invites/InvitesClient'

export default function InvitesPage() {
  return (
    <>
      <Header
        title="Invites"
        subtitle="Create a Keycloak user and add them to an entity's realm group"
        endpoint="POST /api/v1/{entityName}/invite"
      />
      <ScreenBody>
        <InvitesClient />
      </ScreenBody>
    </>
  )
}
