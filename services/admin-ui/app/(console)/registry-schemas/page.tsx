import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { RegistrySchemaList } from '@/components/registrySchemas/List'
import { listRegistrySchemas } from '@/lib/server/clients/registryClient'

export default async function RegistrySchemasPage() {
  const schemas = await listRegistrySchemas().catch(() => [])

  return (
    <>
      <Header
        title="Registry schemas"
        subtitle="The registry's own Schema entity — upstream of credential schemas"
        endpoint="POST/GET/PUT /api/v1/Schema"
      />
      <ScreenBody>
        <RegistrySchemaList initialSchemas={schemas} />
      </ScreenBody>
    </>
  )
}
