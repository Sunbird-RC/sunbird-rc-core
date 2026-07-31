'use client'

import { createContext, useContext } from 'react'
import type { Flags } from '@/lib/shared/types/flags'

const FlagsContext = createContext<Flags | null>(null)

export function FlagsProvider({ flags, children }: { flags: Flags; children: React.ReactNode }) {
  return <FlagsContext.Provider value={flags}>{children}</FlagsContext.Provider>
}

export function useFlags(): Flags {
  const flags = useContext(FlagsContext)
  if (!flags) throw new Error('useFlags must be used within FlagsProvider')
  return flags
}
