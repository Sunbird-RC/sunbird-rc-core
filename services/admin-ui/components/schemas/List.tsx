'use client'

import { useState } from 'react'
import { Chip } from '@/components/ui/Chip'
import { Table } from '@/components/ui/Table'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { SkeletonRows } from '@/components/ui/Skeleton'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Button } from '@/components/ui/Button'
import { useDrawer } from '@/components/providers/DrawerProvider'
import { NewSchemaModal } from './NewSchemaModal'
import { SchemaDrawerContent } from './SchemaDrawer'
import type { CredentialSchema } from '@/lib/server/clients/credentialSchemaClient'
import { STATUS_COLOR } from '@/lib/shared/statusColors'

// No "list all" endpoint exists — the controller unguarded-splits `tags`,
// so browsing requires picking one of these common tags first.
const COMMON_TAGS = ['education', 'identity', 'employment', 'health', 'finance', 'general']

export function SchemaList() {
  const [tag, setTag] = useState<string | null>(null)
  const [schemas, setSchemas] = useState<CredentialSchema[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showNew, setShowNew] = useState(false)
  const drawer = useDrawer()

  const loadTag = async (t: string) => {
    setTag(t)
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(`/admin/api/credential-schema?tags=${encodeURIComponent(t)}`)
      if (!res.ok) throw new Error(await res.text())
      setSchemas(await res.json())
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  const openDetail = (s: CredentialSchema) => {
    drawer.open(s.name, <SchemaDrawerContent id={s.id} onChanged={() => tag && loadTag(tag)} />)
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-md bg-white p-4 shadow-md">
        <div className="mb-3 flex items-start justify-between gap-4">
          <div>
            <div className="text-sm font-medium text-ink">Filter by tag</div>
            <div className="mt-0.5 text-sm text-gray-500">
              The listing endpoint splits <code>tags</code> without a guard, so a tag is required.
            </div>
          </div>
          <Button className="flex-none" onClick={() => setShowNew(true)}>
            + New schema
          </Button>
        </div>
        <div className="flex flex-wrap gap-2">
          {COMMON_TAGS.map((t) => (
            <Chip key={t} label={t} active={tag === t} onClick={() => loadTag(t)} />
          ))}
        </div>
      </div>

      {tag === null && (
        <EmptyState title="Pick a tag to browse schemas" body='There is no "all schemas" call yet — GET /credential-schema needs at least one tag.' />
      )}
      {loading && <SkeletonRows />}
      {!loading && error && <ErrorBanner title="Request failed" body={error} onRetry={() => tag && loadTag(tag)} />}
      {!loading && !error && schemas && schemas.length === 0 && (
        <EmptyState title="No schemas carry this tag" body="Try another tag, or create the first schema for it." />
      )}
      {!loading && !error && schemas && schemas.length > 0 && (
        <Table
          rows={schemas}
          rowKey={(s) => `${s.id}@${s.version}`}
          onRowClick={openDetail}
          columns={[
            { key: 'name', label: 'Schema', render: (s) => <span className="font-medium text-ink">{s.name}</span> },
            { key: 'ver', label: 'Latest', widthClass: '90px', render: (s) => <span className="font-mono text-sm">{s.version}</span> },
            {
              key: 'status',
              label: 'Status',
              widthClass: '110px',
              render: (s) => <StatusBadge label={s.status} color={STATUS_COLOR[s.status] ?? 'neutral'} />,
            },
            {
              key: 'tags',
              label: 'Tags',
              render: (s) => <span className="truncate text-xs text-gray-500">{s.tags?.join(', ') ?? '—'}</span>,
            },
            { key: 'updated', label: 'Updated', widthClass: '110px', render: (s) => s.updatedAt ?? '—' },
          ]}
        />
      )}

      {showNew && <NewSchemaModal onClose={() => setShowNew(false)} onCreated={() => tag && loadTag(tag)} />}
    </div>
  )
}
