'use client'

import Ajv from 'ajv'
import { useMemo, useState } from 'react'

const ajv = new Ajv({ allErrors: true, strict: false })

// Plain textarea + client-side AJV validation for v1 — no heavy editor
// dependency. Documented upgrade path: swap for CodeMirror/Monaco if syntax
// highlighting becomes a real ask, without changing the calling contract.
export function JsonEditor({
  value,
  onChange,
  schema,
  rows = 16,
}: {
  value: string
  onChange: (v: string) => void
  schema?: object
  rows?: number
}) {
  const [error, setError] = useState<string | null>(null)

  const validate = useMemo(() => (schema ? ajv.compile(schema) : null), [schema])

  const handleChange = (v: string) => {
    onChange(v)
    try {
      const parsed = JSON.parse(v)
      if (validate && !validate(parsed)) {
        setError(ajv.errorsText(validate.errors))
        return
      }
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Invalid JSON')
    }
  }

  return (
    <div>
      <textarea
        value={value}
        onChange={(e) => handleChange(e.target.value)}
        rows={rows}
        spellCheck={false}
        className="w-full rounded-sm border border-gray-200 bg-gray-50 p-3 font-mono text-xs text-charcoal focus:border-wave focus:outline-none"
      />
      {error && (
        <div className="mt-2 rounded-sm border border-danger/30 bg-danger-bg px-3 py-2 text-xs text-danger">
          {error}
        </div>
      )}
    </div>
  )
}
