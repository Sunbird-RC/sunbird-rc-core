'use client'

import { useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { JsonEditor } from '@/components/ui/JsonEditor'
import { Button } from '@/components/ui/Button'
import { useToast } from '@/components/providers/ToastProvider'
import { SAMPLE_REGISTRY_SCHEMA } from '@/lib/shared/samples'

export function NewRegistrySchemaModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState(SAMPLE_REGISTRY_SCHEMA.name)
  const [description, setDescription] = useState(SAMPLE_REGISTRY_SCHEMA.description)
  const [schemaJson, setSchemaJson] = useState(SAMPLE_REGISTRY_SCHEMA.schema)
  const [busy, setBusy] = useState(false)
  const toast = useToast()

  const loadSample = () => {
    setName(SAMPLE_REGISTRY_SCHEMA.name)
    setDescription(SAMPLE_REGISTRY_SCHEMA.description)
    setSchemaJson(SAMPLE_REGISTRY_SCHEMA.schema)
  }

  const submit = async () => {
    setBusy(true)
    try {
      // The registry's Schema entity takes `schema` as a STRINGIFIED
      // string — confirmed against the Postman collection and the generic
      // create controller. This differs from credential-schema's contract
      // (a nested object) — don't parse schemaJson back into an object here.
      const res = await fetch('/admin/api/registry/schema', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ name, description, schema: schemaJson, status: 'DRAFT' }),
      })
      if (!res.ok) throw new Error(await res.text())
      toast('Registry schema created as DRAFT.', 'success')
      onCreated()
      onClose()
    } catch (e) {
      toast(`Create failed: ${e instanceof Error ? e.message : String(e)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      title="New registry schema"
      subtitle="Registers a new entity type — created as DRAFT, publish it separately once ready."
      onClose={onClose}
      footer={
        <div className="flex items-center justify-between">
          <button type="button" onClick={loadSample} className="text-xs font-medium text-ink hover:underline">
            Load sample
          </button>
          <div className="flex gap-2">
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button disabled={busy || !name} onClick={submit}>
              {busy ? 'Creating…' : 'Create as DRAFT'}
            </Button>
          </div>
        </div>
      }
    >
      <div className="flex flex-col gap-3.5">
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Entity type name, e.g. Student"
          className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
        />
        <input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Description"
          className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
        />
        <div>
          <label className="mb-1.5 block text-xs font-medium text-gray-500">
            Schema (sent as a stringified JSON schema, including <code>_osConfig</code>)
          </label>
          <JsonEditor value={schemaJson} onChange={setSchemaJson} rows={16} />
        </div>
      </div>
    </Modal>
  )
}
