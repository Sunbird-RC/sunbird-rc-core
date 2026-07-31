// Swagger 2.0 -> entity types + column/form field definitions, generated
// per-schema by RegistrySwaggerController.generateAPIMethods(). Kept as a
// pure function (no fetch, no framework) so it's cheap to unit-test in
// isolation from the network — the risky part of Phase 5 is this parser,
// not the wiring around it.
//
// Two things the generator does NOT do, verified against source:
//  - no top-level `tags` array is ever emitted — entity types are derived
//    from `definitions` keys instead (excluding the `<Entity>OsConfig`
//    companion key each entity also gets).
//  - no DELETE operation appears in `paths` for any entity — the registry's
//    generic controller still supports DELETE /api/v1/{entityName}/{entityId}
//    (verified directly against RegistryEntityController.java), the swagger
//    doc just doesn't document it. The UI should still offer delete.
import type { SwaggerDoc } from './fetchSwagger'

export type JsonSchemaNode = {
  type?: string
  format?: string
  enum?: string[]
  title?: string
  description?: string
  properties?: Record<string, JsonSchemaNode>
  required?: string[]
}

export type FieldDef = {
  path: string // dot-path, e.g. "identityDetails.fullName"
  label: string
  type: string // 'string' | 'number' | 'boolean' | 'enum' | 'object'
  format?: string
  enumOptions?: string[]
  required: boolean
}

export type EntityFormSchema = {
  entityType: string
  fields: FieldDef[]
  // First 4 leaf fields are used as default table columns — keeps the
  // generic table from becoming absurdly wide for schemas with many fields.
  columns: FieldDef[]
}

function titleCase(key: string): string {
  return key.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase()).trim()
}

function flatten(node: JsonSchemaNode, prefix: string, requiredParent: string[]): FieldDef[] {
  if (!node.properties) return []
  const out: FieldDef[] = []
  for (const [key, child] of Object.entries(node.properties)) {
    const path = prefix ? `${prefix}.${key}` : key
    const required = requiredParent.includes(key)
    if (child.type === 'object' && child.properties) {
      out.push(...flatten(child, path, child.required ?? []))
      continue
    }
    out.push({
      path,
      label: child.title ?? titleCase(key),
      type: child.enum ? 'enum' : (child.type ?? 'string'),
      format: child.format,
      enumOptions: child.enum,
      required,
    })
  }
  return out
}

export function parseEntitySchema(doc: SwaggerDoc): EntityFormSchema[] {
  const entityTypes = Object.keys(doc.definitions ?? {}).filter((k) => !k.endsWith('OsConfig'))

  return entityTypes.map((entityType) => {
    const def = doc.definitions[entityType] as JsonSchemaNode
    const fields = flatten(def, '', def.required ?? [])
    return { entityType, fields, columns: fields.slice(0, 4) }
  })
}
