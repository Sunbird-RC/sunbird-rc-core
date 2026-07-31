import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { EmptyState } from '@/components/ui/EmptyState'
import { EntityClient } from '@/components/entities/EntityClient'
import { fetchSwagger } from '@/lib/server/swagger/fetchSwagger'
import { parseEntitySchema } from '@/lib/server/swagger/parseEntitySchema'
import { searchEntities } from '@/lib/server/clients/registryEntityClient'

export default async function EntityTypePage({ params }: { params: { entityType: string } }) {
  const schemas = await fetchSwagger()
    .then(parseEntitySchema)
    .catch(() => [])
  const schema = schemas.find((s) => s.entityType === params.entityType)

  return (
    <>
      <Header
        title={params.entityType}
        subtitle="Generated from the registry's own swagger.json — zero per-type code"
        endpoint={`GET /api/v1/${params.entityType}`}
      />
      <ScreenBody>
        {!schema ? (
          <EmptyState title="Unknown entity type" body="It may not be published, or swagger.json is unreachable." />
        ) : (
          <EntityInner entityType={params.entityType} schema={schema} />
        )}
      </ScreenBody>
    </>
  )
}

async function EntityInner({
  entityType,
  schema,
}: {
  entityType: string
  schema: ReturnType<typeof parseEntitySchema>[number]
}) {
  const { items, totalCount } = await searchEntities(entityType).catch(() => ({ items: [], totalCount: 0 }))
  return <EntityClient schema={schema} initialRows={items} initialTotalCount={totalCount} />
}
