'use client'

import { createContext, useContext, useState } from 'react'

export type DrawerState = {
  label: string
  content: React.ReactNode
} | null

type DrawerApi = {
  drawer: DrawerState
  open: (label: string, content: React.ReactNode) => void
  close: () => void
  isOpen: boolean
  toggle: () => void
}

const DrawerContext = createContext<DrawerApi | null>(null)

export function DrawerProvider({ children }: { children: React.ReactNode }) {
  const [drawer, setDrawer] = useState<DrawerState>(null)
  const [isOpen, setIsOpen] = useState(true)

  const api: DrawerApi = {
    drawer,
    open: (label, content) => {
      setDrawer({ label, content })
      setIsOpen(true)
    },
    close: () => setDrawer(null),
    isOpen,
    toggle: () => setIsOpen((o) => !o),
  }

  return <DrawerContext.Provider value={api}>{children}</DrawerContext.Provider>
}

export function useDrawer(): DrawerApi {
  const ctx = useContext(DrawerContext)
  if (!ctx) throw new Error('useDrawer must be used within DrawerProvider')
  return ctx
}
