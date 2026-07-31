import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { AuditList } from '@/components/audit/List'
import { searchAudit } from '@/lib/server/clients/registryClient'

export default async function AuditPage() {
  const events = await searchAudit().catch(() => [])

  return (
    <>
      <Header title="Audit trail" subtitle="Read-only, append-only event log" endpoint="POST /audit/search" />
      <ScreenBody>
        <AuditList initialEvents={events} />
      </ScreenBody>
    </>
  )
}
