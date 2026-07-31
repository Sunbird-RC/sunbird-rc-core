'use client'

import { useEffect, useState } from 'react'

export function EntityTypePicker({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  const [options, setOptions] = useState<string[]>([])

  useEffect(() => {
    fetch('/admin/api/registry/swagger')
      .then((r) => r.json())
      .then((schemas: { entityType: string }[]) => setOptions(schemas.map((s) => s.entityType)))
      .catch(() => setOptions([]))
  }, [])

  return (
    <div className="flex items-center gap-2">
      <input
        list="entity-types"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Entity type, e.g. Student"
        className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
      />
      <datalist id="entity-types">
        {options.map((o) => (
          <option key={o} value={o} />
        ))}
      </datalist>
    </div>
  )
}
