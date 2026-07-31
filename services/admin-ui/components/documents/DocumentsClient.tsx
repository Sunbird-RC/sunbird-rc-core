'use client'

import { useState } from 'react'
import { EntityTypePicker } from '@/components/shared/EntityTypePicker'
import { Table } from '@/components/ui/Table'
import { EmptyState } from '@/components/ui/EmptyState'
import { Button } from '@/components/ui/Button'
import { useToast } from '@/components/providers/ToastProvider'
import type { DocumentEntry } from '@/lib/server/clients/registryClient'

export function DocumentsClient() {
  const [entityType, setEntityType] = useState('')
  const [entityId, setEntityId] = useState('')
  const [property, setProperty] = useState('documents')
  const [docs, setDocs] = useState<DocumentEntry[] | null>(null)
  const toast = useToast()

  const load = async () => {
    if (!entityType || !entityId) return
    const res = await fetch(`/admin/api/registry/documents/${entityType}/${entityId}/${property}`)
    setDocs(res.ok ? await res.json() : [])
  }

  const remove = async (docId: string) => {
    await fetch(`/admin/api/registry/documents/${entityType}/${entityId}/${property}/${docId}`, { method: 'DELETE' })
    toast('Document removed.', 'success')
    load()
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-md bg-white p-4 shadow-md">
        <div className="text-sm font-medium text-ink">Attachments on</div>
        <div className="mt-0.5 mb-3 text-sm text-gray-500">
          Files live in the MinIO <code>issuance</code> bucket, read back through the gateway at /bucket/.
        </div>
        <div className="flex flex-wrap items-center gap-2.5">
          <div className="min-w-[160px] flex-1">
            <EntityTypePicker value={entityType} onChange={setEntityType} />
          </div>
          <input
            value={entityId}
            onChange={(e) => setEntityId(e.target.value)}
            placeholder="Entity id (osid)"
            className="h-10 min-w-[160px] flex-1 rounded-sm border border-gray-200 px-3 text-sm"
          />
          <input
            value={property}
            onChange={(e) => setProperty(e.target.value)}
            placeholder="Property (e.g. documents)"
            className="h-10 min-w-[160px] flex-1 rounded-sm border border-gray-200 px-3 text-sm"
          />
          <Button onClick={load}>Load</Button>
        </div>
      </div>

      {docs === null && <EmptyState title="Pick an entity" body="Enter an entity type and id to see its documents." />}
      {docs && docs.length === 0 && <EmptyState title="No documents attached" body="Nothing uploaded for this property yet." />}
      {docs && docs.length > 0 && (
        <Table
          rows={docs}
          rowKey={(d) => d.id}
          columns={[
            { key: 'name', label: 'Name', render: (d) => <span className="font-medium text-ink">{d.name}</span> },
            { key: 'size', label: 'Size', widthClass: '100px', render: (d) => (d.size ? `${Math.round(d.size / 1024)} KB` : '—') },
            { key: 'uploaded', label: 'Uploaded', widthClass: '120px', render: (d) => d.uploadedAt ?? '—' },
            {
              key: 'actions',
              label: '',
              widthClass: '90px',
              render: (d) => (
                <Button variant="link" size="sm" className="p-0 h-auto" onClick={() => remove(d.id)}>
                  Remove
                </Button>
              ),
            },
          ]}
        />
      )}
    </div>
  )
}
