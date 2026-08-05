import { useState } from 'react'
import { toPayload, validate, type FieldSpec } from '../fields'
import { Field, Spinner } from './ui'

/**
 * Generic add/edit form driven by field metadata, shared by farmers and all
 * three child entities. One implementation means labels, required markers,
 * number coercion and blank-stripping behave identically everywhere.
 */
export function EntityForm({
  fields,
  initial,
  submitLabel,
  /** Fields that must not be edited after creation (an identifier, typically). */
  lockedFields = [],
  onSubmit,
  onCancel,
}: {
  fields: FieldSpec[]
  initial: Record<string, unknown>
  submitLabel: string
  lockedFields?: string[]
  onSubmit: (payload: Record<string, unknown>) => Promise<void>
  onCancel: () => void
}) {
  const [rec, setRec] = useState<Record<string, unknown>>(initial)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)
  const [failure, setFailure] = useState<string>()

  const set = (name: string, value: unknown) => {
    setRec((r) => ({ ...r, [name]: value }))
    // Clear the field's error as soon as it's touched; re-validated on submit.
    setErrors((e) => (e[name] ? { ...e, [name]: '' } : e))
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    const found = validate(fields, rec)
    const real = Object.fromEntries(Object.entries(found).filter(([, v]) => v))
    setErrors(real)
    if (Object.keys(real).length) return

    setSaving(true)
    setFailure(undefined)
    try {
      // `farmerId` is carried through even when it isn't an editable field of
      // this form — child entities need it to attach to the right farmer.
      const payload = toPayload(fields, rec)
      if (rec.farmerId && !payload.farmerId) payload.farmerId = rec.farmerId
      await onSubmit(payload)
    } catch (err) {
      setFailure((err as Error).message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate>
      {failure && (
        <div className="alert err" role="alert">
          <span className="ico" aria-hidden>
            ⚠
          </span>
          <div>{failure}</div>
        </div>
      )}

      <div className="grid2">
        {fields.map((f) => {
          const locked = lockedFields.includes(f.name)
          const value = rec[f.name] === undefined || rec[f.name] === null ? '' : String(rec[f.name])
          return (
            <Field
              key={f.name}
              label={f.label}
              required={f.required}
              help={locked ? 'Cannot be changed after creation' : f.help}
              error={errors[f.name]}
            >
              {f.type === 'select' ? (
                <select
                  value={value}
                  disabled={locked || saving}
                  className={errors[f.name] ? 'invalid' : undefined}
                  onChange={(e) => set(f.name, e.target.value)}
                >
                  <option value="">—</option>
                  {(f.options ?? []).map((o) => (
                    <option key={o} value={o}>
                      {o}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  type={f.type === 'number' ? 'number' : f.type === 'date' ? 'date' : 'text'}
                  step={f.type === 'number' ? 'any' : undefined}
                  value={value}
                  disabled={locked || saving}
                  className={errors[f.name] ? 'invalid' : undefined}
                  onChange={(e) => set(f.name, e.target.value)}
                />
              )}
            </Field>
          )
        })}
      </div>

      <div className="form-actions">
        <button type="button" className="btn-ghost" onClick={onCancel} disabled={saving}>
          Cancel
        </button>
        <button type="submit" className="btn" disabled={saving}>
          {saving ? (
            <>
              <Spinner /> Saving…
            </>
          ) : (
            submitLabel
          )}
        </button>
      </div>
    </form>
  )
}
