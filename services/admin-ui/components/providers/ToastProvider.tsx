'use client'

import { createContext, useCallback, useContext, useRef, useState } from 'react'

type Toast = { id: number; message: string; variant: 'success' | 'error' | 'info' }
type ToastApi = (message: string, variant?: Toast['variant']) => void

const ToastContext = createContext<ToastApi | null>(null)

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextId = useRef(0)

  const push = useCallback<ToastApi>((message, variant = 'info') => {
    const id = nextId.current++
    setToasts((t) => [...t, { id, message, variant }])
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 4500)
  }, [])

  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`animate-[toastIn_0.2s_ease-out] rounded-md px-4 py-3 text-sm shadow-lg ${
              t.variant === 'success'
                ? 'bg-forest text-white'
                : t.variant === 'error'
                  ? 'bg-brick text-white'
                  : 'bg-ink text-ivory'
            }`}
          >
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}
