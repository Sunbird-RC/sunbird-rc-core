'use client'

import { useState } from 'react'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { useConfirm } from '@/hooks/useConfirm'
import { useToast } from '@/components/providers/ToastProvider'
import { STATUS_COLOR } from '@/lib/shared/statusColors'
import type { CredentialSummary, VerifyResult } from '@/lib/server/clients/credentialsClient'

export function CredentialDrawerContent({
  credential,
  onChanged,
}: {
  credential: CredentialSummary
  onChanged: () => void
}) {
  const [renderAs, setRenderAs] = useState<'json' | 'pdf' | 'html' | 'svg'>('json')
  const [verifyState, setVerifyState] = useState<'idle' | 'busy' | 'done'>('idle')
  const [verifyResult, setVerifyResult] = useState<VerifyResult | null>(null)
  const { request, confirm, cancel } = useConfirm()
  const toast = useToast()

  const runVerify = async () => {
    setVerifyState('busy')
    try {
      const res = await fetch(`/admin/api/credentials/${credential.id}/verify`)
      setVerifyResult(await res.json())
    } finally {
      setVerifyState('done')
    }
  }

  const revoke = async () => {
    await fetch(`/admin/api/credentials/${credential.id}`, { method: 'DELETE' })
    toast('Credential revoked. StatusList bit flipped.', 'success')
    onChanged()
  }

  return (
    <div className="flex flex-col gap-4">
      <div>
        <div className="font-serif text-lg font-medium text-ink">{credential.subject}</div>
        <div className="mt-2 flex items-center gap-2">
          <StatusBadge label={credential.status} color={STATUS_COLOR[credential.status] ?? 'neutral'} />
          {credential.format && (
            <code className="rounded-full border border-wave bg-[#eef5f7] px-2 py-0.5 text-xs">{credential.format}</code>
          )}
        </div>
      </div>

      <dl className="grid grid-cols-[80px_1fr] gap-x-3 gap-y-1.5 text-xs">
        <dt className="text-gray-500">id</dt>
        <dd className="break-all font-mono text-charcoal">{credential.id}</dd>
        <dt className="text-gray-500">issuer</dt>
        <dd className="break-all font-mono text-charcoal">{credential.issuer ?? '—'}</dd>
        <dt className="text-gray-500">schema</dt>
        <dd className="font-mono text-charcoal">{credential.schema ?? '—'}</dd>
        <dt className="text-gray-500">issued</dt>
        <dd className="text-charcoal">{credential.issuanceDate ?? '—'}</dd>
      </dl>

      <div>
        <div className="mb-1.5 text-xs font-medium uppercase tracking-wide text-gray-500">Render as</div>
        <div className="inline-flex rounded-full border border-gray-200 p-0.5 text-xs">
          {(['json', 'pdf', 'html', 'svg'] as const).map((r) => (
            <button
              key={r}
              type="button"
              onClick={() => setRenderAs(r)}
              className={`rounded-full px-2.5 py-1 uppercase ${renderAs === r ? 'bg-ink text-ivory' : 'text-gray-500'}`}
            >
              {r}
            </button>
          ))}
        </div>
        {(renderAs === 'pdf' || renderAs === 'html') && (
          <p className="mt-2 text-xs text-warning-text">Needs a templateid header, or the request 400s.</p>
        )}
      </div>

      <div className="rounded-sm border border-gray-100 p-3">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-ink">Verify</span>
          <button
            type="button"
            onClick={runVerify}
            disabled={verifyState === 'busy'}
            className="rounded-sm border border-gray-200 px-3 py-1 text-xs font-medium hover:bg-gray-50"
          >
            {verifyState === 'busy' ? 'Verifying…' : 'Run verify'}
          </button>
        </div>
        {verifyResult && (
          <ul className="mt-2 flex flex-col gap-1 text-xs">
            {verifyResult.checks?.map((c) => (
              <li key={c.name} className="flex items-center gap-1.5">
                <span className={c.passed ? 'text-forest' : 'text-brick'}>{c.passed ? '✓' : '✕'}</span>
                <span className="text-charcoal">{c.name}</span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {credential.status !== 'REVOKED' && (
        <button
          type="button"
          onClick={() =>
            confirm({
              title: 'Revoke credential?',
              body: 'Sets status to REVOKED and flips the StatusList2021 bit. This cannot be undone.',
              detail: `DELETE /credentials/${credential.id}`,
              ctaLabel: 'Revoke',
              ctaVariant: 'destructive',
              onConfirm: revoke,
            })
          }
          className="rounded-sm border border-danger/30 px-4 py-2 text-sm font-medium text-brick hover:bg-danger-bg"
        >
          Revoke credential
        </button>
      )}
      <ConfirmDialog request={request} onCancel={cancel} />
    </div>
  )
}
