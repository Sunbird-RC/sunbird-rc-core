'use client'

import { useCallback, useState } from 'react'

export type ConfirmRequest = {
  title: string
  body: string
  detail?: string
  ctaLabel: string
  ctaVariant?: 'default' | 'destructive'
  onConfirm: () => void | Promise<void>
}

export function useConfirm() {
  const [request, setRequest] = useState<ConfirmRequest | null>(null)

  const confirm = useCallback((req: ConfirmRequest) => setRequest(req), [])
  const cancel = useCallback(() => setRequest(null), [])

  return { request, confirm, cancel }
}
