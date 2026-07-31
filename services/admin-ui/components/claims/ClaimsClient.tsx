'use client'

import { useState } from 'react'
import { EntityTypePicker } from '@/components/shared/EntityTypePicker'
import { Table } from '@/components/ui/Table'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { EmptyState } from '@/components/ui/EmptyState'
import { useDrawer } from '@/components/providers/DrawerProvider'
import { useToast } from '@/components/providers/ToastProvider'
import { Button } from '@/components/ui/Button'
import { STATUS_COLOR } from '@/lib/shared/statusColors'
import type { Claim } from '@/lib/server/clients/registryClient'

function ClaimDetail({ entityType, claim, onChanged }: { entityType: string; claim: Claim; onChanged: () => void }) {
  const [notes, setNotes] = useState('')
  const toast = useToast()

  const attest = async (action: 'GRANTED' | 'DENIED') => {
    await fetch(`/admin/api/registry/claims/${entityType}/${claim.id}/attest`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ action, notes }),
    })
    toast(`Claim ${action.toLowerCase()}.`, 'success')
    onChanged()
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="font-serif text-lg font-medium text-ink">{claim.subject ?? claim.entityId}</div>
      <StatusBadge label={claim.status} color={STATUS_COLOR[claim.status] ?? 'neutral'} />
      <dl className="grid grid-cols-[80px_1fr] gap-x-3 gap-y-1.5 text-xs">
        <dt className="text-gray-500">property</dt>
        <dd className="text-charcoal">{claim.property ?? '—'}</dd>
        <dt className="text-gray-500">attestor</dt>
        <dd className="text-charcoal">{claim.attestorEntity ?? '—'}</dd>
      </dl>
      {claim.status === 'OPEN' ? (
        <>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Notes"
            rows={3}
            className="rounded-sm border border-gray-200 p-2.5 text-sm"
          />
          <div className="flex gap-2">
            <Button className="bg-forest hover:bg-moss" onClick={() => attest('GRANTED')}>
              Grant
            </Button>
            <Button variant="destructive" onClick={() => attest('DENIED')}>
              Deny
            </Button>
          </div>
        </>
      ) : (
        claim.notes && (
          <div className="rounded-sm bg-cream p-3 text-xs text-charcoal">{claim.notes}</div>
        )
      )}
    </div>
  )
}

export function ClaimsClient() {
  const [entityType, setEntityType] = useState('')
  const [claims, setClaims] = useState<Claim[] | null>(null)
  const [filter, setFilter] = useState<'all' | 'OPEN' | 'GRANTED' | 'DENIED'>('all')
  const drawer = useDrawer()

  const load = async (type: string) => {
    setEntityType(type)
    const res = await fetch(`/admin/api/registry/claims/${type}`)
    setClaims(res.ok ? await res.json() : [])
  }

  const openDetail = (c: Claim) => {
    drawer.open(c.subject ?? 'Claim', <ClaimDetail entityType={entityType} claim={c} onChanged={() => load(entityType)} />)
  }

  const visible = (claims ?? []).filter((c) => filter === 'all' || c.status === filter)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3">
        <EntityTypePicker value={entityType} onChange={load} />
        {claims && (
          <div className="flex gap-1.5">
            {(['all', 'OPEN', 'GRANTED', 'DENIED'] as const).map((f) => (
              <button
                key={f}
                type="button"
                onClick={() => setFilter(f)}
                className={`rounded-full px-3 py-1.5 text-xs font-medium ${filter === f ? 'bg-ink text-ivory' : 'border border-gray-200 text-gray-600'}`}
              >
                {f}
              </button>
            ))}
          </div>
        )}
      </div>

      {!entityType && <EmptyState title="Pick an entity type" body="Claims are scoped to one entity type at a time." />}
      {claims && visible.length === 0 && <EmptyState title="Nothing here" body="No claims match this filter." />}
      {claims && visible.length > 0 && (
        <Table
          rows={visible}
          rowKey={(c) => c.id}
          onRowClick={openDetail}
          columns={[
            { key: 'subject', label: 'Subject', render: (c) => c.subject ?? c.entityId ?? '—' },
            { key: 'property', label: 'Property', render: (c) => c.property ?? '—' },
            { key: 'attestor', label: 'Attestor', render: (c) => c.attestorEntity ?? '—' },
            {
              key: 'status',
              label: 'Status',
              widthClass: '110px',
              render: (c) => <StatusBadge label={c.status} color={STATUS_COLOR[c.status] ?? 'neutral'} />,
            },
          ]}
        />
      )}
      <p className="text-xs text-gray-500">
        Read and attest through the registry proxy — never claim-ms directly.
      </p>
    </div>
  )
}
