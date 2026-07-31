'use client'

import { useState } from 'react'
import { EntityTypePicker } from '@/components/shared/EntityTypePicker'
import { useToast } from '@/components/providers/ToastProvider'
import { Button } from '@/components/ui/Button'
import { SAMPLE_INVITE } from '@/lib/shared/samples'

type SentInvite = { name: string; email: string; entity: string; role: string; time: string }

export function InvitesClient() {
  const [entityType, setEntityType] = useState('')
  const [name, setName] = useState(SAMPLE_INVITE.name)
  const [email, setEmail] = useState(SAMPLE_INVITE.email)
  const [role, setRole] = useState('viewer')
  const [busy, setBusy] = useState(false)
  const [sent, setSent] = useState<SentInvite[]>([])
  const toast = useToast()

  const send = async () => {
    setBusy(true)
    try {
      const res = await fetch(`/admin/api/registry/invite/${entityType}`, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ name, email, role }),
      })
      if (!res.ok) throw new Error(await res.text())
      setSent((prev) => [{ name, email, entity: entityType, role, time: new Date().toLocaleTimeString() }, ...prev])
      toast('Invite sent.', 'success')
      setName('')
      setEmail('')
    } catch (e) {
      toast(`Invite failed: ${e instanceof Error ? e.message : String(e)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-[380px_1fr] lg:items-start">
      <div className="rounded-md bg-white p-5 shadow-md">
        <div className="text-sm font-medium text-ink">Invite someone</div>
        <div className="mt-1 mb-4 text-sm text-gray-500">
          Creates a Keycloak user and puts them in the realm group for that entity.
        </div>
        <div className="flex flex-col gap-3.5">
          <EntityTypePicker value={entityType} onChange={setEntityType} />
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Full name" className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm" />
          <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email" className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm" />
          <select value={role} onChange={(e) => setRole(e.target.value)} className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm">
            <option value="admin">admin</option>
            <option value="attestor">attestor</option>
            <option value="viewer">viewer</option>
          </select>
          <Button fullWidth size="lg" disabled={busy || !entityType || !name || !email} onClick={send}>
            {busy ? 'Sending…' : 'Send invite'}
          </Button>
          <button
            type="button"
            onClick={() => {
              setName(SAMPLE_INVITE.name)
              setEmail(SAMPLE_INVITE.email)
            }}
            className="text-xs font-medium text-ink hover:underline"
          >
            Load sample
          </button>
        </div>
      </div>

      <div className="flex flex-col gap-4">
        <div className="rounded-sm border border-wave/40 bg-[#eef5f7] px-4 py-3 text-xs text-charcoal">
          Invite is the only user API the registry exposes — there is no list-users or edit-roles endpoint. For
          anything richer, use the Keycloak console at /auth/.
        </div>
        <div className="rounded-md bg-white shadow-md">
          <div className="border-b border-gray-100 px-5 py-3.5 text-sm font-medium text-ink">Sent in this session</div>
          {sent.length === 0 ? (
            <div className="px-5 py-9 text-center text-sm text-gray-500">
              Nothing sent yet — the registry does not keep a list you can read back.
            </div>
          ) : (
            sent.map((s, i) => (
              <div key={i} className="flex items-center gap-3.5 border-b border-gray-50 px-5 py-3 last:border-0">
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-medium text-ink">{s.name}</div>
                  <div className="truncate text-xs text-gray-400">{s.email}</div>
                </div>
                <span className="text-xs text-gray-500">{s.entity}</span>
                <code className="text-xs">{s.role}</code>
                <span className="ml-auto text-xs text-gray-400">{s.time}</span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}
