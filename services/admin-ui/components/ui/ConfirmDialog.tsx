'use client'

import { useState } from 'react'
import type { ConfirmRequest } from '@/hooks/useConfirm'
import { Modal } from './Modal'
import { Button } from './Button'

// Generic reusable confirm — used for deprecate/revoke/publish/delete/deny-claim.
// Each caller supplies its own tailored warning copy via `body`/`detail`.
export function ConfirmDialog({ request, onCancel }: { request: ConfirmRequest | null; onCancel: () => void }) {
  const [busy, setBusy] = useState(false)
  if (!request) return null

  const run = async () => {
    setBusy(true)
    try {
      await request.onConfirm()
      onCancel()
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      title={request.title}
      onClose={onCancel}
      width="max-w-md"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onCancel}>
            Keep as is
          </Button>
          <Button variant={request.ctaVariant === 'destructive' ? 'destructive' : 'default'} disabled={busy} onClick={run}>
            {busy ? 'Working…' : request.ctaLabel}
          </Button>
        </div>
      }
    >
      <p className="text-sm text-gray-600">{request.body}</p>
      {request.detail && (
        <code className="mt-3 block rounded-sm bg-gray-50 p-3 font-mono text-xs text-gray-700">
          {request.detail}
        </code>
      )}
    </Modal>
  )
}
