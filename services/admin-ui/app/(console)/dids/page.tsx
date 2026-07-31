import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { DidsClient } from '@/components/dids/DidsClient'

export default function DidsPage() {
  return (
    <>
      <Header title="DIDs" subtitle="Generate and resolve decentralized identifiers" endpoint="POST /did/generate" />
      <ScreenBody>
        <DidsClient />
      </ScreenBody>
    </>
  )
}
