'use client'

import { useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { getPath, setPath } from '@/lib/shared/nestedPath'
import { sampleFieldValue } from '@/lib/shared/samples'
import type { FieldDef } from '@/lib/server/swagger/parseEntitySchema'

export function EntityFormModal({
  entityType,
  fields,
  initial,
  onClose,
  onSaved,
}: {
  entityType: string
  fields: FieldDef[]
  initial?: Record<string, unknown>
  onClose: () => void
  onSaved: () => void
}) {
  const [value, setValue] = useState<Record<string, unknown>>(initial ?? {})
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const isEdit = Boolean(initial)

  // Synthesises a plausible value per field from its swagger-derived
  // type/format/enum — same idea as verifier-app's sampleValue(), applied
  // generically to whatever entity type the registry hands us.
  const loadSample = () => {
    let next = value
    for (const f of fields) {
      next = setPath(next, f.path, sampleFieldValue(f.label, f.type, f.format, f.enumOptions))
    }
    setValue(next)
  }

  const submit = async () => {
    setBusy(true)
    setError(null)
    try {
      const url = isEdit
        ? `/admin/api/registry/entities/${entityType}/${(initial as { osid?: string })?.osid}`
        : `/admin/api/registry/entities/${entityType}`
      const res = await fetch(url, {
        method: isEdit ? 'PUT' : 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify(value),
      })
      if (!res.ok) throw new Error(await res.text())
      onSaved()
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      title={isEdit ? `Edit ${entityType}` : `New ${entityType}`}
      subtitle={isEdit ? `PUT /api/v1/${entityType}/{entityId}` : `POST /api/v1/${entityType}`}
      onClose={onClose}
      footer={
        <div className="flex items-center justify-between">
          {!isEdit && (
            <button type="button" onClick={loadSample} className="text-xs font-medium text-ink hover:underline">
              Load sample values
            </button>
          )}
          <div className="ml-auto flex gap-2">
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button disabled={busy} onClick={submit}>
              {busy ? 'Saving…' : 'Save'}
            </Button>
          </div>
        </div>
      }
    >
      {error && <div className="mb-3 rounded-sm bg-danger-bg px-3 py-2 text-xs text-danger">{error}</div>}
      <div className="flex flex-col gap-3.5">
        {fields.map((f) => (
          <div key={f.path} className="flex flex-col gap-1.5">
            <label className="text-xs font-medium text-gray-500">
              {f.label}
              {f.required && <span className="text-brick"> *</span>}
            </label>
            {f.type === 'enum' ? (
              <select
                value={String(getPath(value, f.path) ?? '')}
                onChange={(e) => setValue(setPath(value, f.path, e.target.value))}
                className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
              >
                <option value="" disabled>
                  Select…
                </option>
                {f.enumOptions?.map((opt) => (
                  <option key={opt} value={opt}>
                    {opt}
                  </option>
                ))}
              </select>
            ) : (
              <input
                type={f.format === 'date' ? 'date' : f.type === 'number' ? 'number' : 'text'}
                value={String(getPath(value, f.path) ?? '')}
                onChange={(e) => setValue(setPath(value, f.path, e.target.value))}
                className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
              />
            )}
          </div>
        ))}
      </div>
    </Modal>
  )
}
