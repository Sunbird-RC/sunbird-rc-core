'use client'

import { useState } from 'react'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { useToast } from '@/components/providers/ToastProvider'
import type { OfferResponse } from '@/lib/server/clients/oid4vcClient'

type Config = { schemaId: string; name: string }
type SessionOffer = { label: string; status: string; time: string }

export function OffersClient({ configs }: { configs: Config[] }) {
  const [configId, setConfigId] = useState(configs[0]?.schemaId ?? '')
  const [subject, setSubject] = useState('')
  const [expiry, setExpiry] = useState('600')
  const [batch, setBatch] = useState('1')
  const [txCode, setTxCode] = useState(false)
  const [busy, setBusy] = useState(false)
  const [offer, setOffer] = useState<OfferResponse | null>(null)
  const [sessionOffers, setSessionOffers] = useState<SessionOffer[]>([])
  const toast = useToast()

  const create = async () => {
    setBusy(true)
    setOffer(null)
    try {
      const res = await fetch('/admin/api/oid4vc/offer', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({
          credential_configuration_id: configId,
          subject: subject || undefined,
          expires_in: Number(expiry) || undefined,
          batch_size: Number(batch) || undefined,
          tx_code_required: txCode,
        }),
      })
      if (!res.ok) throw new Error(await res.text())
      const data: OfferResponse = await res.json()
      setOffer(data)
      setSessionOffers((prev) => [
        { label: configId, status: 'pending', time: new Date().toLocaleTimeString() },
        ...prev,
      ])
    } catch (e) {
      toast(`Offer creation failed: ${e instanceof Error ? e.message : String(e)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-[380px_1fr] lg:items-start">
      <div className="rounded-md bg-white p-5 shadow-md">
        <div className="text-sm font-medium text-ink">New credential offer</div>
        <div className="mt-1 mb-4 text-sm text-gray-500">
          Only <strong>published</strong> schemas appear here.
        </div>
        <div className="flex flex-col gap-3.5">
          <select
            value={configId}
            onChange={(e) => setConfigId(e.target.value)}
            className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
          >
            {configs.length === 0 && <option value="">No published OID4VCI-enabled schemas yet</option>}
            {configs.map((c) => (
              <option key={c.schemaId} value={c.schemaId}>
                {c.name}
              </option>
            ))}
          </select>
          <input
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            placeholder="Subject DID (optional — leave blank for a bearer offer)"
            className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
          />
          <div className="flex gap-3">
            <input
              value={expiry}
              onChange={(e) => setExpiry(e.target.value)}
              placeholder="Expiry (s)"
              className="h-10 flex-1 rounded-sm border border-gray-200 px-3 text-sm"
            />
            <input
              value={batch}
              onChange={(e) => setBatch(e.target.value)}
              placeholder="Batch size"
              className="h-10 flex-1 rounded-sm border border-gray-200 px-3 text-sm"
            />
          </div>
          <label className="flex items-start gap-2.5 rounded-sm border border-gray-100 bg-cream p-3 text-sm">
            <input type="checkbox" checked={txCode} onChange={(e) => setTxCode(e.target.checked)} className="mt-0.5" />
            <span>
              <span className="font-medium text-ink">Require a transaction code</span>
              <span className="mt-0.5 block text-xs text-gray-500">
                A 6-digit PIN, returned once, in plaintext, and never again.
              </span>
            </span>
          </label>
          <button
            type="button"
            disabled={busy || !configId}
            onClick={create}
            className="rounded-sm bg-ink py-2.5 text-sm font-medium text-white disabled:opacity-60"
          >
            {busy ? 'Minting…' : 'Create offer'}
          </button>
          <div className="text-center text-xs text-gray-400">POST /oid4vc/offer</div>
        </div>
      </div>

      <div className="flex flex-col gap-4">
        {!offer && !busy && (
          <div className="rounded-md border border-dashed border-gray-200 bg-cream px-6 py-16 text-center">
            <div className="font-serif text-lg font-medium text-ink">No offer yet</div>
            <div className="mt-1.5 text-sm text-gray-500">Create one and the QR appears here.</div>
          </div>
        )}
        {busy && (
          <div className="flex flex-col items-center gap-3 rounded-md bg-white p-16 shadow-md">
            <div className="h-7 w-7 animate-spin rounded-full border-[3px] border-gray-100 border-t-brick" />
            <div className="text-sm text-gray-500">Minting the offer…</div>
          </div>
        )}
        {offer && (
          <div className="flex flex-col gap-4">
            <div className="flex flex-wrap gap-5 rounded-md bg-white p-5 shadow-md">
              <img
                src={`/admin/api/oid4vc/qr?data=${encodeURIComponent(offer.qr_data)}`}
                alt="Offer QR"
                width={140}
                height={140}
                className="rounded-sm border border-gray-100"
              />
              <div className="min-w-[200px] flex-1">
                <div className="text-xs font-medium uppercase tracking-wide text-gray-500">Offer URI</div>
                <code className="mt-1 block break-all rounded-sm bg-gray-50 p-2 font-mono text-xs">
                  {offer.credential_offer_uri}
                </code>
              </div>
            </div>
            {offer.tx_code && (
              <div className="rounded-md border border-warning bg-warning-bg p-4">
                <div className="text-sm font-medium text-warning-text">Transaction code — shown once</div>
                <div className="mt-2 font-mono text-2xl font-bold tracking-widest text-warning-text">
                  {offer.tx_code}
                </div>
              </div>
            )}
          </div>
        )}

        <div className="rounded-md bg-white shadow-md">
          <div className="border-b border-gray-100 px-5 py-3.5 text-sm font-medium text-ink">Recent offers</div>
          {sessionOffers.length === 0 ? (
            <div className="px-5 py-9 text-center text-sm text-gray-500">
              Offers you mint in this session show up here.
            </div>
          ) : (
            sessionOffers.map((o, i) => (
              <div key={i} className="flex items-center gap-3.5 border-b border-gray-50 px-5 py-3 last:border-0">
                <span className="truncate text-sm text-ink">{o.label}</span>
                <StatusBadge label={o.status} color="neutral" />
                <span className="ml-auto text-xs text-gray-500">{o.time}</span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}
