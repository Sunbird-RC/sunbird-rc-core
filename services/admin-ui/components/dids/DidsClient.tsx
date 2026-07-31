'use client'

import { useEffect, useState } from 'react'
import { useToast } from '@/components/providers/ToastProvider'
import { useDrawer } from '@/components/providers/DrawerProvider'
import { Table } from '@/components/ui/Table'
import { Button } from '@/components/ui/Button'

const STORAGE_KEY = 'admin-ui:dids'

type DidRow = { did: string; method: string; created: string }

function loadRows(): DidRow[] {
  if (typeof window === 'undefined') return []
  try {
    return JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? '[]')
  } catch {
    return []
  }
}

function saveRows(rows: DidRow[]) {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(rows))
}

export function DidsClient() {
  const toast = useToast()
  const drawer = useDrawer()
  const [rows, setRows] = useState<DidRow[]>([])
  const [method, setMethod] = useState('did:web')
  const [count, setCount] = useState('1')
  const [aka, setAka] = useState('')
  const [resolveQ, setResolveQ] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => setRows(loadRows()), [])

  const generate = async () => {
    setBusy(true)
    try {
      const res = await fetch('/admin/api/identity/did/generate', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({
          content: Array.from({ length: Number(count) || 1 }, () => ({
            method,
            alsoKnownAs: aka || undefined,
          })),
        }),
      })
      if (!res.ok) throw new Error(await res.text())
      const data = await res.json()
      const generated: string[] = Array.isArray(data) ? data.map((d: { id?: string }) => d.id ?? '') : []
      const now = new Date().toISOString()
      const next = [...generated.filter(Boolean).map((did) => ({ did, method, created: now })), ...rows]
      setRows(next)
      saveRows(next)
      toast(`Generated ${generated.length} DID(s).`, 'success')
    } catch (e) {
      toast(`Generate failed: ${e instanceof Error ? e.message : String(e)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  const resolve = async (did: string) => {
    try {
      const res = await fetch(`/admin/api/identity/did/resolve/${encodeURIComponent(did)}`)
      const body = await res.json()
      drawer.open(
        'DID document',
        <pre className="whitespace-pre-wrap break-all rounded-sm bg-gray-50 p-3 font-mono text-xs text-charcoal">
          {JSON.stringify(body, null, 2)}
        </pre>,
      )
      if (!rows.some((r) => r.did === did)) {
        const next = [{ did, method: did.split(':').slice(0, 2).join(':'), created: new Date().toISOString() }, ...rows]
        setRows(next)
        saveRows(next)
      }
    } catch (e) {
      toast(`Resolve failed: ${e instanceof Error ? e.message : String(e)}`, 'error')
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="rounded-md bg-white p-5 shadow-md">
          <div className="text-sm font-medium text-ink">Generate DIDs</div>
          <div className="mt-1 mb-4 text-sm text-gray-500">Batch generation through identity-service.</div>
          <div className="flex flex-col gap-3.5">
            <select
              value={method}
              onChange={(e) => setMethod(e.target.value)}
              className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
            >
              <option value="did:web">did:web</option>
              <option value="did:key">did:key</option>
              <option value="did:rcw">did:rcw</option>
            </select>
            <input
              type="number"
              min={1}
              value={count}
              onChange={(e) => setCount(e.target.value)}
              placeholder="How many"
              className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
            />
            <input
              value={aka}
              onChange={(e) => setAka(e.target.value)}
              placeholder="alsoKnownAs (optional)"
              className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
            />
            <Button disabled={busy} onClick={generate}>
              {busy ? 'Generating…' : 'Generate'}
            </Button>
          </div>
        </div>

        <div className="rounded-md bg-white p-5 shadow-md">
          <div className="text-sm font-medium text-ink">Resolve a DID</div>
          <div className="mt-1 mb-4 text-sm text-gray-500">The resolved document opens in the inspector.</div>
          <div className="flex flex-col gap-3.5">
            <input
              value={resolveQ}
              onChange={(e) => setResolveQ(e.target.value)}
              placeholder="did:web:registry.sunbird.example:9f2a1c"
              className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
            />
            <Button variant="outline" onClick={() => resolveQ && resolve(resolveQ)}>
              Resolve
            </Button>
          </div>
          <p className="mt-4 rounded-sm border border-wave/40 bg-[#eef5f7] px-3 py-2.5 text-xs text-charcoal">
            identity-service has no list endpoint, so DIDs cannot be enumerated. The table below is kept in this
            browser only.
          </p>
        </div>
      </div>

      <div className="rounded-md bg-white shadow-md">
        <div className="border-b border-gray-100 px-5 py-3.5 text-sm font-medium text-ink">
          DIDs created on this device
        </div>
        {rows.length === 0 ? (
          <div className="px-5 py-9 text-center text-sm text-gray-500">Nothing yet.</div>
        ) : (
          <Table
            rows={rows}
            rowKey={(r) => r.did}
            onRowClick={(r) => resolve(r.did)}
            columns={[
              { key: 'did', label: 'DID', render: (r) => <span className="font-mono text-xs">{r.did}</span> },
              { key: 'method', label: 'Method', widthClass: '110px', render: (r) => r.method },
              { key: 'created', label: 'Created', widthClass: '130px', render: (r) => r.created },
            ]}
          />
        )}
      </div>
    </div>
  )
}
