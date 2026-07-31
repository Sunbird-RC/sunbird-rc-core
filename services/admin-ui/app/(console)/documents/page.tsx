import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { FlagDisabledState } from '@/components/ui/EmptyState'
import { DocumentsClient } from '@/components/documents/DocumentsClient'
import { getFlags } from '@/lib/server/flags'

export default async function DocumentsPage() {
  const flags = await getFlags()

  return (
    <>
      <Header
        title="Documents"
        subtitle="Attachments stored in the MinIO issuance bucket"
        endpoint="POST /api/v1/{entity}/{entityId}/{property}/documents"
      />
      <ScreenBody>
        {flags.filestorage ? (
          <DocumentsClient />
        ) : (
          <FlagDisabledState
            title="File storage is switched off"
            body="Documents need FILESSTORAGE_ENABLED=true plus a reachable MinIO."
          />
        )}
      </ScreenBody>
    </>
  )
}
