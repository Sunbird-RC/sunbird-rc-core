'use client'

import { X } from 'lucide-react'

export function Modal({
  title,
  subtitle,
  onClose,
  children,
  footer,
  width = 'max-w-2xl',
}: {
  title: string
  subtitle?: string
  onClose: () => void
  children: React.ReactNode
  footer?: React.ReactNode
  width?: string
}) {
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div
        className={`flex max-h-[85vh] w-full ${width} flex-col overflow-hidden rounded-md bg-white shadow-lg`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between border-b border-gray-100 px-6 py-4">
          <div>
            <div className="font-serif text-lg font-medium text-ink">{title}</div>
            {subtitle && <div className="mt-1 text-sm text-gray-500">{subtitle}</div>}
          </div>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-ink">
            <X size={18} />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto px-6 py-5">{children}</div>
        {footer && <div className="border-t border-gray-100 px-6 py-4">{footer}</div>}
      </div>
    </div>
  )
}
