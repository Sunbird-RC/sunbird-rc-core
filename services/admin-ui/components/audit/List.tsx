'use client'

import React, { useEffect, useState } from 'react'
import { Table } from '@/components/ui/Table'
import { SearchInput } from '@/components/ui/SearchInput'
import { EmptyState } from '@/components/ui/EmptyState'
import { SkeletonRows } from '@/components/ui/Skeleton'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { useDebouncedSearch } from '@/hooks/useDebouncedSearch'
import { useDrawer } from '@/components/providers/DrawerProvider'
import type { AuditEvent } from '@/lib/server/clients/registryClient'

export function AuditList({ initialEvents }: { initialEvents: AuditEvent[] }) {
  const [q, setQ] = useState('')
  const debouncedQ = useDebouncedSearch(q)
  const [events, setEvents] = useState(initialEvents)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const drawer = useDrawer()

  useEffect(() => {
    if (debouncedQ === '') {
      setEvents(initialEvents)
      return
    }
    setLoading(true)
    setError(null)
    fetch(`/admin/api/registry/audit?q=${encodeURIComponent(debouncedQ)}`)
      .then((r) => r.json())
      .then(setEvents)
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [debouncedQ, initialEvents])

  const openDetail = (e: AuditEvent) => {
    drawer.open(
      'Audit event',
      <div className="flex flex-col gap-3">
        <div className="font-serif text-lg font-medium text-ink">
          {e.action} on {e.entityType ?? '—'}
        </div>
        <dl className="grid grid-cols-[100px_1fr] gap-x-3 gap-y-2 text-xs">
          {(['eventId', 'timestamp', 'userId', 'action', 'entityType', 'recordId', 'ip'] as const).map((k) => (
            <React.Fragment key={k}>
              <dt className="text-gray-500">{k}</dt>
              <dd className="break-all font-mono text-charcoal">{String(e[k] ?? '—')}</dd>
            </React.Fragment>
          ))}
        </dl>
        <p className="text-xs text-gray-500">Append-only — no edit or delete path exists for audit events.</p>
      </div>,
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3">
        <div className="min-w-[220px] flex-1">
          <SearchInput value={q} onChange={setQ} placeholder="Filter by actor, action, entity or target" />
        </div>
      </div>

      {loading && <SkeletonRows />}
      {!loading && error && <ErrorBanner title="Request failed" body={error} onRetry={() => setQ((v) => v)} />}
      {!loading && !error && events.length === 0 && (
        <EmptyState title="Nothing matches that filter" body="Try a different search term." />
      )}
      {!loading && !error && events.length > 0 && (
        <Table
          rows={events}
          rowKey={(e) => e.eventId}
          onRowClick={openDetail}
          columns={[
            { key: 'time', label: 'When', widthClass: '150px', render: (e) => e.timestamp },
            { key: 'actor', label: 'Actor', widthClass: '150px', render: (e) => e.userId ?? '—' },
            { key: 'action', label: 'Action', widthClass: '120px', render: (e) => e.action },
            {
              key: 'target',
              label: 'Target',
              render: (e) => (
                <span className="truncate font-mono text-xs text-charcoal">
                  {e.entityType ?? '—'} · {e.recordId ?? '—'}
                </span>
              ),
            },
          ]}
        />
      )}
    </div>
  )
}
