'use client'

import { useEffect, useState } from 'react'

export function useDebouncedSearch(value: string, delayMs = 300): string {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(t)
  }, [value, delayMs])

  return debounced
}
