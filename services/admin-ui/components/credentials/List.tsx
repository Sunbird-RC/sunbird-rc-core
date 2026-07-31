'use client'

import { useState } from 'react'
import { Table } from '@/components/ui/Table'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { SkeletonRows } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Button } from '@/components/ui/Button'
import { useDrawer } from '@/components/providers/DrawerProvider'
import { IssueModal } from './IssueModal'
import { CredentialDrawerContent } from './CredentialDrawer'
import { STATUS_COLOR } from '@/lib/shared/statusColors'
import type { CredentialSummary } from '@/lib/server/clients/credentialsClient'

export function CredentialList() {
  const [subject, setSubject] = useState('')
  const [issuer, setIssuer] = useState('')
  const [type, setType] = useState('')
  const [results, setResults] = useState<CredentialSummary[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showIssue, setShowIssue] = useState(false)
  const drawer = useDrawer()

  const search = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetch('/admin/api/credentials', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ subject: subject || undefined, issuer: issuer || undefined, type: type || undefined }),
      })
      if (!res.ok) throw new Error(await res.text())
      setResults(await res.json())
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  const openDetail = (c: CredentialSummary) => {
    drawer.open(c.subject, <CredentialDrawerContent credential={c} onChanged={search} />)
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-md bg-white p-4 shadow-md">
        <div className="mb-3 flex items-start justify-between gap-4">
          <div>
            <div className="text-sm font-medium text-ink">Search credentials</div>
            <div className="mt-0.5 text-sm text-gray-500">Any one field is enough.</div>
          </div>
          <Button className="flex-none" onClick={() => setShowIssue(true)}>
            + Issue credential
          </Button>
        </div>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-4">
          <input
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            placeholder="Subject DID"
            className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
          />
          <input
            value={issuer}
            onChange={(e) => setIssuer(e.target.value)}
            placeholder="Issuer DID"
            className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
          />
          <input
            value={type}
            onChange={(e) => setType(e.target.value)}
            placeholder="Credential type"
            className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
          />
          <Button onClick={search}>Search</Button>
        </div>
      </div>

      {loading && <SkeletonRows />}
      {!loading && error && <ErrorBanner title="Request failed" body={error} onRetry={search} />}
      {!loading && !error && results && results.length === 0 && (
        <EmptyState title="No credentials matched" body="Try different search terms." />
      )}
      {!loading && !error && results && results.length > 0 && (
        <Table
          rows={results}
          rowKey={(c) => c.id}
          onRowClick={openDetail}
          columns={[
            { key: 'subject', label: 'Subject', render: (c) => <span className="font-medium text-ink">{c.subject}</span> },
            { key: 'schema', label: 'Schema', render: (c) => c.schema ?? '—' },
            {
              key: 'status',
              label: 'Status',
              widthClass: '110px',
              render: (c) => <StatusBadge label={c.status} color={STATUS_COLOR[c.status] ?? 'neutral'} />,
            },
            { key: 'issued', label: 'Issued', widthClass: '110px', render: (c) => c.issuanceDate ?? '—' },
          ]}
        />
      )}

      {showIssue && <IssueModal onClose={() => setShowIssue(false)} onIssued={search} />}
    </div>
  )
}
