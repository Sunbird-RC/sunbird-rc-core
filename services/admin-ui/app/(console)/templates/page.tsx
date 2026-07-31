import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { TemplateList } from '@/components/templates/List'
import { listTemplates } from '@/lib/server/clients/credentialSchemaClient'

export default async function TemplatesPage() {
  const templates = await listTemplates().catch(() => [])

  return (
    <>
      <Header
        title="Render templates"
        subtitle="SVG/HTML templates used for PDF, HTML and SVG-QR rendering"
        endpoint="GET /template?schemaId="
      />
      <ScreenBody>
        <TemplateList initialTemplates={templates} />
      </ScreenBody>
    </>
  )
}
