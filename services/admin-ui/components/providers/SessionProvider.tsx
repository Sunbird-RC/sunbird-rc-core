'use client'

import { createContext, useContext } from 'react'

export type ClientSession = { name: string; email?: string } | null

const SessionContext = createContext<ClientSession>(null)

export function SessionProvider({ session, children }: { session: ClientSession; children: React.ReactNode }) {
  return <SessionContext.Provider value={session}>{children}</SessionContext.Provider>
}

export function useSession(): ClientSession {
  return useContext(SessionContext)
}
