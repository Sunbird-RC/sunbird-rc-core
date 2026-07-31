import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { SchemaList } from '@/components/schemas/List'

export default function SchemasPage() {
  return (
    <>
      <Header
        title="Schemas"
        subtitle="Credential schema lifecycle — DRAFT, PUBLISHED, DEPRECATED, REVOKED"
        endpoint="GET /credential-schema?tags="
      />
      <ScreenBody>
        <SchemaList />
      </ScreenBody>
    </>
  )
}
