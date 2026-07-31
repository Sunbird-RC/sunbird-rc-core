'use client'

import { useState } from 'react'
import { Table } from '@/components/ui/Table'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { EmptyState } from '@/components/ui/EmptyState'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Button } from '@/components/ui/Button'
import { useConfirm } from '@/hooks/useConfirm'
import { useDrawer } from '@/components/providers/DrawerProvider'
import { useToast } from '@/components/providers/ToastProvider'
import { NewRegistrySchemaModal } from './NewRegistrySchemaModal'
import type { RegistrySchema } from '@/lib/server/clients/registryClient'

export function RegistrySchemaList({ initialSchemas }: { initialSchemas: RegistrySchema[] }) {
  const [schemas, setSchemas] = useState(initialSchemas)
  const [showNew, setShowNew] = useState(false)
  const drawer = useDrawer()
  const toast = useToast()
  const { request, confirm, cancel } = useConfirm()

  const reload = () => {
    fetch('/admin/api/registry/schema')
      .then((r) => r.json())
      .then(setSchemas)
  }

  const publish = async (schema: RegistrySchema) => {
    await fetch(`/admin/api/registry/schema/${schema.osid}`, {
      method: 'PUT',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(schema),
    })
    setSchemas((prev) => prev.map((s) => (s.osid === schema.osid ? { ...s, status: 'PUBLISHED' } : s)))
    toast('Schema published.', 'success')
    drawer.close()
  }

  const openDetail = (schema: RegistrySchema) => {
    drawer.open(
      'Registry schema',
      <div className="flex flex-col gap-4">
        <div>
          <div className="font-serif text-lg font-medium text-ink">{schema.name}</div>
          <div className="mt-2">
            <StatusBadge label={schema.status} color={schema.status === 'PUBLISHED' ? 'success' : 'neutral'} />
          </div>
        </div>
        <div className="text-xs text-gray-500">Roles: {schema._osConfig?.roles?.join(', ') ?? '—'}</div>
        {schema.status === 'DRAFT' && (
          <Button
            onClick={() =>
              confirm({
                title: 'Publish schema?',
                body: 'Publishing runs ensureCredentialSchema() and saveIdFormat() — both are no-ops unless signature.enabled/idgen.enabled are set (both false by default), so on a stock deploy this just flips the status.',
                detail: `PUT /api/v1/Schema/${schema.osid}`,
                ctaLabel: 'Publish',
                onConfirm: () => publish(schema),
              })
            }
          >
            Publish schema
          </Button>
        )}
      </div>,
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex justify-end">
        <Button onClick={() => setShowNew(true)}>+ New registry schema</Button>
      </div>

      {schemas.length === 0 ? (
        <EmptyState
          title="No registry schemas yet"
          body="Create one to register a new entity type — try the sample to see how it works."
        />
      ) : (
        <Table
          rows={schemas}
          rowKey={(s) => s.osid ?? s.name}
          onRowClick={openDetail}
          columns={[
            { key: 'name', label: 'Schema', render: (s) => <span className="font-medium text-ink">{s.name}</span> },
            {
              key: 'status',
              label: 'Status',
              widthClass: '124px',
              render: (s) => <StatusBadge label={s.status} color={s.status === 'PUBLISHED' ? 'success' : 'neutral'} />,
            },
            {
              key: 'roles',
              label: 'Roles',
              widthClass: 'minmax(0,1fr)',
              render: (s) => <span className="text-xs text-gray-500">{s._osConfig?.roles?.join(', ') ?? '—'}</span>,
            },
          ]}
        />
      )}

      {showNew && <NewRegistrySchemaModal onClose={() => setShowNew(false)} onCreated={reload} />}
      <ConfirmDialog request={request} onCancel={cancel} />
    </div>
  )
}
