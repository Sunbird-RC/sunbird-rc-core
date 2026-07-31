'use client'

import { useState } from 'react'
import { Table } from '@/components/ui/Table'
import { EmptyState } from '@/components/ui/EmptyState'
import { Button } from '@/components/ui/Button'
import { useDrawer } from '@/components/providers/DrawerProvider'
import { useToast } from '@/components/providers/ToastProvider'
import type { RenderTemplate } from '@/lib/server/clients/credentialSchemaClient'

export function TemplateList({ initialTemplates }: { initialTemplates: RenderTemplate[] }) {
  const [templates] = useState(initialTemplates)
  const drawer = useDrawer()
  const toast = useToast()

  const openDetail = async (t: RenderTemplate) => {
    const res = await fetch(`/admin/api/credential-schema/templates/${t.id}`)
    const detail = await res.json()
    drawer.open(
      'Render template',
      <div className="flex flex-col gap-3">
        <div className="font-serif text-lg font-medium text-ink">{t.id}</div>
        <div className="text-xs text-gray-500">Schema: {t.schemaId}</div>
        {detail.content && (
          // SVG preview — sanitized: this is a preview surface only and must
          // never render an unsanitized string via dangerouslySetInnerHTML.
          <div className="rounded-sm border border-gray-100 bg-white p-3" aria-label="Template preview (rendering deferred to Phase 5 sanitizer)">
            <pre className="max-h-40 overflow-auto whitespace-pre-wrap break-all font-mono text-[10px] text-gray-500">
              {String(detail.content).slice(0, 2000)}
            </pre>
          </div>
        )}
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => toast('Edit — not implemented yet.', 'info')}>
            Edit
          </Button>
          <Button
            variant="destructive"
            size="sm"
            onClick={async () => {
              await fetch(`/admin/api/credential-schema/templates/${t.id}`, { method: 'DELETE' })
              toast('Template deleted.', 'success')
              drawer.close()
            }}
          >
            Delete
          </Button>
        </div>
      </div>,
    )
  }

  if (templates.length === 0) {
    return <EmptyState title="No render templates yet" body="Create one to render credentials as PDF/HTML/SVG-QR." />
  }

  return (
    <Table
      rows={templates}
      rowKey={(t) => t.id}
      onRowClick={openDetail}
      columns={[
        { key: 'name', label: 'Template', render: (t) => <span className="font-medium text-ink">{t.id}</span> },
        { key: 'schema', label: 'Schema', render: (t) => t.schemaId },
        { key: 'kind', label: 'Type', widthClass: '90px', render: (t) => t.kind ?? '—' },
        { key: 'updated', label: 'Updated', widthClass: '110px', render: (t) => t.updatedAt ?? '—' },
      ]}
    />
  )
}
