'use client'

import { useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { JsonEditor } from '@/components/ui/JsonEditor'
import { Button } from '@/components/ui/Button'
import { useToast } from '@/components/providers/ToastProvider'
import { SAMPLE_CREDENTIAL_SCHEMA } from '@/lib/shared/samples'

export function NewSchemaModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [value, setValue] = useState(SAMPLE_CREDENTIAL_SCHEMA)
  const [busy, setBusy] = useState(false)
  const toast = useToast()

  const submit = async () => {
    setBusy(true)
    try {
      const parsed = JSON.parse(value)
      const res = await fetch('/admin/api/credential-schema', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify(parsed),
      })
      if (!res.ok) throw new Error(await res.text())
      toast('Schema created as DRAFT.', 'success')
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
      title="New schema"
      subtitle="Created as DRAFT — publish it separately once ready."
      onClose={onClose}
      footer={
        <div className="flex items-center justify-between">
          <button
            type="button"
            onClick={() => setValue(SAMPLE_CREDENTIAL_SCHEMA)}
            className="text-xs font-medium text-ink hover:underline"
          >
            Load sample
          </button>
          <div className="flex gap-2">
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button disabled={busy} onClick={submit}>
              {busy ? 'Creating…' : 'Create as DRAFT'}
            </Button>
          </div>
        </div>
      }
    >
      <JsonEditor value={value} onChange={setValue} rows={18} />
    </Modal>
  )
}
