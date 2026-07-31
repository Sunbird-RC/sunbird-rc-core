import Link from 'next/link'
import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { EmptyState, FlagDisabledState } from '@/components/ui/EmptyState'
import { Chip } from '@/components/ui/Chip'
import { getFlags } from '@/lib/server/flags'
import { fetchSwagger } from '@/lib/server/swagger/fetchSwagger'
import { parseEntitySchema } from '@/lib/server/swagger/parseEntitySchema'

export default async function EntitiesPage() {
  const flags = await getFlags()

  return (
    <>
      <Header
        title="Entities"
        subtitle="Columns and forms generated from the registry's swagger.json"
        endpoint="GET /api/v1/{entityName}"
      />
      <ScreenBody>
        {!flags.swaggerReachable ? (
          <FlagDisabledState
            title="Registry swagger.json is not reachable"
            body="Entity columns/forms are generated from GET /api/docs/swagger.json. Confirm the registry is running and api-swagger.enabled is true (it defaults to true, but isn't wired through any compose override)."
          />
        ) : (
          <EntityTypeChips />
        )}
      </ScreenBody>
    </>
  )
}

async function EntityTypeChips() {
  const schemas = await fetchSwagger()
    .then(parseEntitySchema)
    .catch(() => [])

  if (schemas.length === 0) {
    return <EmptyState title="No entity types published yet" body="Publish a registry schema to see it here." />
  }

  return (
    <div className="flex flex-wrap gap-2">
      {schemas.map((s) => (
        <Link key={s.entityType} href={`/entities/${s.entityType}`}>
          <Chip label={s.entityType} />
        </Link>
      ))}
    </div>
  )
}
