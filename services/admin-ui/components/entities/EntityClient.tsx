'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { Table } from '@/components/ui/Table'
import { SearchInput } from '@/components/ui/SearchInput'
import { ViewToggle } from '@/components/ui/ViewToggle'
import { EmptyState } from '@/components/ui/EmptyState'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Button } from '@/components/ui/Button'
import { useConfirm } from '@/hooks/useConfirm'
import { useDebouncedSearch } from '@/hooks/useDebouncedSearch'
import { useDrawer } from '@/components/providers/DrawerProvider'
import { useToast } from '@/components/providers/ToastProvider'
import { getPath } from '@/lib/shared/nestedPath'
import { downloadCsv } from '@/lib/shared/csv'
import { EntityFormModal } from './EntityFormModal'
import type { EntityFormSchema } from '@/lib/server/swagger/parseEntitySchema'

type Row = Record<string, unknown>

export function EntityClient({
  schema,
  initialRows,
  initialTotalCount,
}: {
  schema: EntityFormSchema
  initialRows: Row[]
  initialTotalCount: number
}) {
  const [rows, setRows] = useState(initialRows)
  const [totalCount, setTotalCount] = useState(initialTotalCount)
  const [q, setQ] = useState('')
  const debouncedQ = useDebouncedSearch(q)
  const [view, setView] = useState<'table' | 'cards'>('table')
  const [showNew, setShowNew] = useState(false)
  const [editing, setEditing] = useState<Row | null>(null)
  const drawer = useDrawer()
  const toast = useToast()
  const { request, confirm, cancel } = useConfirm()

  const reload = (query?: string) => {
    const firstField = schema.columns[0]?.path
    const params = query && firstField ? `?q=${encodeURIComponent(query)}&field=${encodeURIComponent(firstField)}` : ''
    fetch(`/admin/api/registry/entities/${schema.entityType}${params}`)
      .then((r) => r.json())
      .then((res: { items: Row[]; totalCount: number }) => {
        setRows(res.items)
        setTotalCount(res.totalCount)
      })
  }

  const isFirstRender = useRef(true)
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false
      return
    }
    reload(debouncedQ || undefined)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQ])

  // Server-side "contains" filter already narrowed `rows` when a query is
  // active; this client-side pass is just a fallback for the render frame
  // between typing and the debounced server response landing.
  const filtered = useMemo(() => {
    if (!debouncedQ) return rows
    const needle = debouncedQ.toLowerCase()
    return rows.filter((r) =>
      schema.fields.some((f) => String(getPath(r, f.path) ?? '').toLowerCase().includes(needle)),
    )
  }, [rows, debouncedQ, schema.fields])

  const del = async (row: Row) => {
    await fetch(`/admin/api/registry/entities/${schema.entityType}/${row.osid}`, { method: 'DELETE' })
    toast(`${schema.entityType} deleted.`, 'success')
    reload()
    drawer.close()
  }

  const openDetail = (row: Row) => {
    drawer.open(
      String(getPath(row, schema.columns[0]?.path ?? '') ?? schema.entityType),
      <div className="flex flex-col gap-3">
        <dl className="grid grid-cols-[110px_1fr] gap-x-3 gap-y-1.5 text-xs">
          {schema.fields.map((f) => (
            <div key={f.path} className="contents">
              <dt className="text-gray-500">{f.label}</dt>
              <dd className="break-all text-charcoal">{String(getPath(row, f.path) ?? '—')}</dd>
            </div>
          ))}
        </dl>
        <pre className="max-h-48 overflow-auto rounded-sm bg-gray-50 p-3 font-mono text-[10px] text-gray-600">
          {JSON.stringify(row, null, 2)}
        </pre>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => setEditing(row)}>
            Edit
          </Button>
          <Button
            variant="destructive"
            size="sm"
            onClick={() =>
              confirm({
                title: `Delete this ${schema.entityType}?`,
                body: 'This cannot be undone.',
                detail: `DELETE /api/v1/${schema.entityType}/${row.osid}`,
                ctaLabel: 'Delete',
                ctaVariant: 'destructive',
                onConfirm: () => del(row),
              })
            }
          >
            Delete
          </Button>
        </div>
      </div>,
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3">
        <div className="min-w-[220px] flex-1">
          <SearchInput value={q} onChange={setQ} placeholder={`Search ${schema.entityType} records`} />
        </div>
        <span className="text-xs text-gray-500">
          {filtered.length} of {totalCount}
        </span>
        <ViewToggle value={view} onChange={setView} />
        <Button
          variant="outline"
          size="sm"
          onClick={() => downloadCsv(`${schema.entityType}.csv`, filtered as Record<string, unknown>[])}
        >
          Export CSV
        </Button>
        <Button onClick={() => setShowNew(true)}>+ New {schema.entityType}</Button>
      </div>

      {filtered.length === 0 ? (
        <EmptyState title={`No ${schema.entityType} records match`} body="Loosen the search or create a new record." />
      ) : view === 'table' ? (
        <Table
          rows={filtered}
          rowKey={(r) => String(r.osid ?? JSON.stringify(r))}
          onRowClick={openDetail}
          columns={schema.columns.map((f) => ({
            key: f.path,
            label: f.label,
            render: (r: Row) => <span className="truncate text-sm text-charcoal">{String(getPath(r, f.path) ?? '—')}</span>,
          }))}
        />
      ) : (
        <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((r) => (
            <button
              key={String(r.osid ?? JSON.stringify(r))}
              type="button"
              onClick={() => openDetail(r)}
              className="flex flex-col gap-1.5 rounded-md bg-white p-4 text-left shadow-md transition hover:-translate-y-0.5 hover:shadow-lg"
            >
              {schema.columns.map((f) => (
                <div key={f.path} className="truncate text-sm text-charcoal">
                  {String(getPath(r, f.path) ?? '—')}
                </div>
              ))}
            </button>
          ))}
        </div>
      )}

      {showNew && (
        <EntityFormModal
          entityType={schema.entityType}
          fields={schema.fields}
          onClose={() => setShowNew(false)}
          onSaved={reload}
        />
      )}
      {editing && (
        <EntityFormModal
          entityType={schema.entityType}
          fields={schema.fields}
          initial={editing}
          onClose={() => setEditing(null)}
          onSaved={reload}
        />
      )}
      <ConfirmDialog request={request} onCancel={cancel} />
    </div>
  )
}
