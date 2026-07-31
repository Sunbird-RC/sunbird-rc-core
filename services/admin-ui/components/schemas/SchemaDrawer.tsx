'use client'

import { useEffect, useState } from 'react'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { useConfirm } from '@/hooks/useConfirm'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { useToast } from '@/components/providers/ToastProvider'
import { allowedTransitions, isTerminal, type SchemaStatus } from '@/lib/shared/stateMachine/schemaLifecycle'
import type { CredentialSchema } from '@/lib/server/clients/credentialSchemaClient'
import { STATUS_COLOR } from '@/lib/shared/statusColors'

const ACTION_LABEL: Record<SchemaStatus, string> = {
  PUBLISHED: 'Publish',
  DEPRECATED: 'Deprecate',
  REVOKED: 'Revoke',
  DRAFT: 'Reset to draft',
}

export function SchemaDrawerContent({ id, onChanged }: { id: string; onChanged: () => void }) {
  const [versions, setVersions] = useState<CredentialSchema[] | null>(null)
  const { request, confirm, cancel } = useConfirm()
  const toast = useToast()

  useEffect(() => {
    fetch(`/admin/api/credential-schema/${id}`)
      .then((r) => r.json())
      .then(setVersions)
  }, [id])

  const transition = async (schema: CredentialSchema, next: SchemaStatus) => {
    const action = next === 'PUBLISHED' ? 'publish' : next === 'DEPRECATED' ? 'deprecate' : 'revoke'
    await fetch(`/admin/api/credential-schema/lifecycle/${action}/${schema.id}/${schema.version}`, { method: 'PUT' })
    toast(`Version ${schema.version} moved to ${next}.`, 'success')
    onChanged()
    fetch(`/admin/api/credential-schema/${id}`)
      .then((r) => r.json())
      .then(setVersions)
  }

  if (!versions) return <div className="text-sm text-gray-500">Loading…</div>

  return (
    <div className="flex flex-col gap-4">
      <div className="font-serif text-lg font-medium text-ink">{id}</div>
      <div className="flex flex-col gap-3">
        {versions.map((v) => (
          <div key={v.version} className="rounded-sm border border-gray-100 p-3">
            <div className="flex items-center justify-between">
              <span className="font-mono text-sm text-charcoal">{v.version}</span>
              <StatusBadge label={v.status} color={STATUS_COLOR[v.status]} />
            </div>
            {v.deprecatedId && <div className="mt-1 text-xs text-gray-400">deprecatedId → {v.deprecatedId}</div>}
            <div className="mt-2 flex flex-wrap gap-1.5">
              {isTerminal(v.status) ? (
                <span className="text-xs text-gray-400">Terminal state</span>
              ) : (
                allowedTransitions(v.status).map((next) => (
                  <button
                    key={next}
                    type="button"
                    onClick={() =>
                      confirm({
                        title: `${ACTION_LABEL[next]} version ${v.version}?`,
                        body:
                          next === 'REVOKED'
                            ? 'Revoke is terminal — it cannot be undone from this console.'
                            : next === 'PUBLISHED'
                              ? 'Only published schemas appear in oid4vci-configs, which is deliberately uncached.'
                              : 'The old row is not auto-deprecated by anything else — this is the only way to mark it superseded.',
                        detail: `PUT /credential-schema/${next.toLowerCase() === 'published' ? 'publish' : next.toLowerCase()}/${v.id}/${v.version}`,
                        ctaLabel: ACTION_LABEL[next],
                        ctaVariant: next === 'REVOKED' ? 'destructive' : 'default',
                        onConfirm: () => transition(v, next),
                      })
                    }
                    className="rounded-full border border-gray-200 px-2.5 py-1 text-xs font-medium text-ink hover:bg-gray-50"
                  >
                    {ACTION_LABEL[next]}
                  </button>
                ))
              )}
            </div>
          </div>
        ))}
      </div>
      <ConfirmDialog request={request} onCancel={cancel} />
    </div>
  )
}
