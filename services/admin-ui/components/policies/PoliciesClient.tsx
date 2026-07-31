'use client'

import { useState } from 'react'
import { EntityTypePicker } from '@/components/shared/EntityTypePicker'
import { Table } from '@/components/ui/Table'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { EmptyState } from '@/components/ui/EmptyState'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { useToast } from '@/components/providers/ToastProvider'
import { SAMPLE_POLICY } from '@/lib/shared/samples'
import type { AttestationPolicy } from '@/lib/server/clients/registryClient'

export function PoliciesClient() {
  const [entityType, setEntityType] = useState('')
  const [policies, setPolicies] = useState<AttestationPolicy[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [showNew, setShowNew] = useState(false)
  const [name, setName] = useState(SAMPLE_POLICY.name)
  const [attestorEntity, setAttestorEntity] = useState(SAMPLE_POLICY.attestorEntity)
  const [conditions, setConditions] = useState(SAMPLE_POLICY.conditions)
  const toast = useToast()

  const loadSample = () => {
    setName(SAMPLE_POLICY.name)
    setAttestorEntity(SAMPLE_POLICY.attestorEntity)
    setConditions(SAMPLE_POLICY.conditions)
  }

  const load = async (type: string) => {
    setEntityType(type)
    setError(null)
    try {
      const res = await fetch(`/admin/api/registry/policies/${type}`)
      if (!res.ok) throw new Error(await res.text())
      setPolicies(await res.json())
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    }
  }

  const create = async () => {
    await fetch(`/admin/api/registry/policies/${entityType}`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ name, attestorEntity, conditions, status: 'DRAFT' }),
    })
    toast('Policy created.', 'success')
    setShowNew(false)
    load(entityType)
  }

  const toggle = async (p: AttestationPolicy) => {
    const next = p.status === 'DRAFT' ? 'PUBLISHED' : 'DRAFT'
    await fetch(`/admin/api/registry/policies/${entityType}/${p.osid}/${next}`, { method: 'PUT' })
    toast(`Policy ${next.toLowerCase()}.`, 'success')
    load(entityType)
  }

  const remove = async (p: AttestationPolicy) => {
    await fetch(`/admin/api/registry/policies/${entityType}/${p.osid}`, { method: 'DELETE' })
    toast('Policy deleted.', 'success')
    load(entityType)
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3">
        <EntityTypePicker value={entityType} onChange={load} />
        {entityType && <Button onClick={() => setShowNew(true)}>+ New policy</Button>}
      </div>

      {!entityType && <EmptyState title="Pick an entity type" body="Policies are scoped to one entity type at a time." />}
      {error && <EmptyState title="Not available" body={error} />}
      {policies && policies.length === 0 && (
        <EmptyState title="No policy covers this entity yet" body="Until one exists, nobody can raise a claim against it." />
      )}
      {policies && policies.length > 0 && (
        <Table
          rows={policies}
          rowKey={(p) => p.osid ?? p.name}
          columns={[
            { key: 'name', label: 'Policy', render: (p) => <span className="font-medium text-ink">{p.name}</span> },
            { key: 'property', label: 'Property', render: (p) => Object.keys(p.attestationProperties ?? {}).join(', ') || '—' },
            { key: 'attestor', label: 'Attestor', render: (p) => p.attestorEntity },
            {
              key: 'status',
              label: 'Status',
              widthClass: '110px',
              render: (p) => <StatusBadge label={p.status} color={p.status === 'PUBLISHED' ? 'success' : 'neutral'} />,
            },
            {
              key: 'actions',
              label: '',
              widthClass: '150px',
              render: (p) => (
                <div className="flex gap-2">
                  <button type="button" onClick={() => toggle(p)} className="text-xs font-medium text-ink hover:underline">
                    {p.status === 'DRAFT' ? 'Publish' : 'Unpublish'}
                  </button>
                  <button type="button" onClick={() => remove(p)} className="text-xs font-medium text-brick hover:underline">
                    Delete
                  </button>
                </div>
              ),
            },
          ]}
        />
      )}

      {showNew && (
        <Modal
          title="New policy"
          subtitle={`POST /api/v1/${entityType}/attestationPolicy`}
          onClose={() => setShowNew(false)}
          footer={
            <div className="flex items-center justify-between">
              <button type="button" onClick={loadSample} className="text-xs font-medium text-ink hover:underline">
                Load sample
              </button>
              <div className="flex gap-2">
                <Button variant="outline" onClick={() => setShowNew(false)}>
                  Cancel
                </Button>
                <Button onClick={create}>Create</Button>
              </div>
            </div>
          }
        >
          <div className="flex flex-col gap-3.5">
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Policy name" className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm" />
            <input
              value={attestorEntity}
              onChange={(e) => setAttestorEntity(e.target.value)}
              placeholder="Attestor entity (e.g. Principal)"
              className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
            />
            <textarea
              value={conditions}
              onChange={(e) => setConditions(e.target.value)}
              placeholder="(ATTESTOR#$.role#.contains('principal'))"
              rows={3}
              className="rounded-sm border border-gray-200 bg-gray-50 p-3 font-mono text-xs"
            />
          </div>
        </Modal>
      )}
    </div>
  )
}
